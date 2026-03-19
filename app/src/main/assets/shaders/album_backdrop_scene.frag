precision mediump float;

uniform sampler2D uCurrentTexture;
uniform sampler2D uPreviousTexture;
uniform vec2 uResolution;
uniform float uTime;
uniform float uTransition;
uniform vec4 uCurrentSeed;
uniform vec4 uPreviousSeed;
uniform float uCurrentAspect;
uniform float uPreviousAspect;

varying vec2 vTexCoord;

vec2 coverUv(vec2 uv, float textureAspect) {
    float screenAspect = uResolution.x / max(uResolution.y, 1.0);
    vec2 scaled = uv;
    if (textureAspect > screenAspect) {
        float scale = screenAspect / textureAspect;
        scaled.x = (uv.x - 0.5) * scale + 0.5;
    } else {
        float scale = textureAspect / screenAspect;
        scaled.y = (uv.y - 0.5) * scale + 0.5;
    }
    return clamp(scaled, 0.0, 1.0);
}

vec2 rotate(vec2 point, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(
        point.x * c - point.y * s,
        point.x * s + point.y * c
    );
}

// Apple-style twist: squared distance ratio for dramatic flowing distortion.
vec2 twist(vec2 coord, vec2 offset, float radius, float angle) {
    coord -= offset;
    float dist = length(coord);
    if (dist < radius) {
        float ratioDist = (radius - dist) / radius;
        float angleMod = ratioDist * ratioDist * angle;
        float s = sin(angleMod);
        float c = cos(angleMod);
        coord = vec2(coord.x * c - coord.y * s,
                     coord.x * s + coord.y * c);
    }
    coord += offset;
    return coord;
}

vec2 mirroredUv(vec2 uv) {
    vec2 tiled = fract(uv);
    return mix(vec2(0.002), vec2(0.998), abs(tiled * 2.0 - 1.0));
}

// Boost saturation for vivid Apple-style colors.
vec3 saturate(vec3 color, float amount) {
    float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(lum), color, amount);
}

// Sample a single layer: a copy of the artwork at a given scale,
// with rotation/orbit and heavy twist distortion.
vec4 sampleLayer(
    sampler2D tex,
    vec2 uv,
    float aspect,
    vec4 seed,
    float time,
    float scale,         // size relative to viewport (0.25 = 25%)
    float twistAngle,    // twist strength (radians, use large values!)
    float twistRadius,   // twist radius in UV space
    vec2 orbitCenter,    // center of circular orbit track
    float orbitRadius,   // radius of orbit (0 = spin in place)
    float spinSpeed,     // rotation speed
    float orbitSpeed     // orbit track speed
) {
    // Start with cover-fit UVs
    vec2 baseUv = coverUv(uv, aspect);

    // Center coordinates
    vec2 centered = baseUv - 0.5;

    // Scale the copy
    centered /= max(scale, 0.01);

    // Spin rotation
    float spinAngle = time * spinSpeed * (seed.x - 0.5) + seed.z * 6.2831;
    centered = rotate(centered, spinAngle);

    // Offset by orbit position (small copies orbit, large copies stay put)
    vec2 orbit = vec2(
        cos(time * orbitSpeed + seed.y * 6.2831),
        sin(time * orbitSpeed + seed.w * 6.2831)
    ) * orbitRadius;

    vec2 finalUv = centered + 0.5 + orbit;

    // Apply heavy twist distortion
    vec2 twistCenter = vec2(
        0.5 + (seed.x - 0.5) * 0.4,
        0.5 + (seed.y - 0.5) * 0.4
    );
    finalUv = twist(finalUv, twistCenter, twistRadius, twistAngle);

    return texture2D(tex, mirroredUv(finalUv));
}

vec4 sampleArtworkField(
    sampler2D tex,
    vec2 uv,
    float aspect,
    vec4 seed,
    float time
) {
    // Layer 0: 125% scale — large background, spins in place only
    vec4 layer0 = sampleLayer(
        tex, uv, aspect, seed,
        time,
        1.25,                                    // scale
        3.8 + seed.x * 1.2,                      // twist angle (strong!)
        0.9 + seed.y * 0.2,                      // twist radius
        vec2(0.5),                                // orbit center (unused)
        0.0,                                      // orbit radius (none — spin only)
        0.03,                                     // spin speed
        0.0                                       // orbit speed
    );

    // Layer 1: 80% scale — medium, spins in place
    vec4 layer1 = sampleLayer(
        tex, uv, aspect, seed.yzwx,
        time,
        0.80,
        -4.5 - seed.z * 1.5,                     // opposite twist direction
        0.85 + seed.w * 0.2,
        vec2(0.5),
        0.0,                                      // no orbit
        -0.025,                                   // spin (opposite dir)
        0.0
    );

    // Layer 2: 50% scale — small, orbits + spins
    vec4 layer2 = sampleLayer(
        tex, uv, aspect, seed.zwxy,
        time,
        0.50,
        5.2 + seed.y * 1.8,
        0.75 + seed.x * 0.2,
        vec2(0.5),
        0.08 + seed.z * 0.04,                    // orbits on a circular track
        0.04,
        0.02 + seed.w * 0.01
    );

    // Layer 3: 25% scale — smallest, orbits + spins
    vec4 layer3 = sampleLayer(
        tex, uv, aspect, seed.wxyz,
        time,
        0.25,
        -6.0 - seed.w * 2.0,                     // strongest twist
        0.65 + seed.z * 0.2,
        vec2(0.5),
        0.12 + seed.x * 0.06,                    // larger orbit
        -0.05,
        -0.03 + seed.y * 0.01
    );

    // Blend layers: largest on bottom, progressively overlay smaller ones.
    // Use opacity to let layers underneath show through.
    vec3 color = layer0.rgb;
    color = mix(color, layer1.rgb, 0.6);
    color = mix(color, layer2.rgb, 0.45);
    color = mix(color, layer3.rgb, 0.35);

    // Boost saturation for vivid colors (Apple oversaturates)
    color = saturate(color, 1.35);

    return vec4(color, 1.0);
}

void main() {
    vec4 previousField = sampleArtworkField(uPreviousTexture, vTexCoord, uPreviousAspect, uPreviousSeed, uTime);
    vec4 currentField = sampleArtworkField(uCurrentTexture, vTexCoord, uCurrentAspect, uCurrentSeed, uTime);
    vec3 color = mix(previousField, currentField, smoothstep(0.0, 1.0, uTransition)).rgb;
    gl_FragColor = vec4(color, 1.0);
}
