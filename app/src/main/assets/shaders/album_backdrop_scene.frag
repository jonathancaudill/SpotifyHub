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

// Apple-style twist: squared distance ratio.
// Applied once to the entire scene (not per-copy).
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

vec2 rotate(vec2 point, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(point.x * c - point.y * s,
                point.x * s + point.y * c);
}

// Sample one square copy of the artwork.
// size = fraction of viewport width (e.g. 1.25 = 125%)
// center = copy center in UV space
// rot = spin angle
// Returns vec4 with alpha=0 outside the copy's square bounds.
vec4 sampleCopy(
    sampler2D tex,
    vec2 uv,
    float textureAspect,
    float screenAspect,
    float size,
    vec2 center,
    float rot
) {
    // Position relative to copy center, in aspect-corrected space
    // so the copy appears square on screen.
    vec2 pos = uv - center;
    pos.y /= screenAspect;

    // Scale: the copy covers `size` of viewport width
    pos /= max(size, 0.001);

    // Undo the copy's spin rotation
    pos = rotate(pos, -rot);

    // Now pos is in copy-local space: [-0.5, 0.5] = the artwork square
    vec2 texUv = pos + 0.5;

    // Outside the square? Transparent.
    if (texUv.x < 0.0 || texUv.x > 1.0 || texUv.y < 0.0 || texUv.y > 1.0) {
        return vec4(0.0);
    }

    // Handle non-square artwork (cover-fit within the square)
    vec2 artUv = texUv;
    if (textureAspect > 1.0) {
        artUv.x = (texUv.x - 0.5) / textureAspect + 0.5;
    } else {
        artUv.y = (texUv.y - 0.5) * textureAspect + 0.5;
    }
    artUv = clamp(artUv, 0.0, 1.0);

    return vec4(texture2D(tex, artUv).rgb, 1.0);
}

vec3 sampleArtworkField(
    sampler2D tex,
    vec2 uv,
    float aspect,
    vec4 seed,
    float time
) {
    float screenAspect = uResolution.x / max(uResolution.y, 1.0);

    // ---- Global twist (applied to entire scene, not per-copy) ----
    // Work in aspect-corrected space so twist is circular on screen.
    vec2 twistCoord = uv - 0.5;
    twistCoord.y /= screenAspect;
    // Radius ~80% of viewport width, angle toned down from reference's -3.25
    float twistRadius = 0.8;
    float twistAngle = -2.0 - seed.x * 0.5;
    vec2 twistOffset = vec2(
        (seed.y - 0.5) * 0.05,
        (seed.z - 0.5) * 0.05
    );
    twistCoord = twist(twistCoord, twistOffset, twistRadius, twistAngle);
    twistCoord.y *= screenAspect;
    vec2 tUv = twistCoord + 0.5;

    // ---- Spin angles ----
    // Based on reference speeds, halved for a calmer feel.
    float spin0 = time * 0.045;
    float spin1 = time * -0.12;
    float spin2 = time * -0.09;
    float spin3 = time * 0.06;

    // ---- Centers ----
    // Sprites 0,1: stationary (1 is slightly offset per reference)
    // Sprites 2,3: orbit on circular tracks
    vec2 center0 = vec2(0.5, 0.5);
    vec2 center1 = vec2(0.5 / 2.5 * 2.0, 0.5 / 2.5 * 2.0);
    // Reference: orbit radius = screenWidth/4 → 0.25 in UV-x space
    // Orbit angle tied to spin * 0.75
    vec2 center2 = vec2(
        0.5 + 0.25 * cos(spin2 * 0.75),
        0.5 + 0.25 * screenAspect * sin(spin2 * 0.75)
    );
    vec2 center3 = vec2(
        0.5 + 0.05 + 0.25 * cos(spin3 * 0.75),
        0.5 + 0.05 * screenAspect + 0.25 * screenAspect * sin(spin3 * 0.75)
    );

    // ---- Sample 4 copies (back to front, largest first) ----
    vec4 c0 = sampleCopy(tex, tUv, aspect, screenAspect, 1.25, center0, spin0);
    vec4 c1 = sampleCopy(tex, tUv, aspect, screenAspect, 0.80, center1, spin1);
    vec4 c2 = sampleCopy(tex, tUv, aspect, screenAspect, 0.50, center2, spin2);
    vec4 c3 = sampleCopy(tex, tUv, aspect, screenAspect, 0.25, center3, spin3);

    // ---- Composite back to front ----
    vec3 color = vec3(0.0);
    color = mix(color, c0.rgb, c0.a);
    color = mix(color, c1.rgb, c1.a);
    color = mix(color, c2.rgb, c2.a);
    color = mix(color, c3.rgb, c3.a);

    return color;
}

void main() {
    vec3 previousField = sampleArtworkField(uPreviousTexture, vTexCoord, uPreviousAspect, uPreviousSeed, uTime);
    vec3 currentField = sampleArtworkField(uCurrentTexture, vTexCoord, uCurrentAspect, uCurrentSeed, uTime);
    vec3 color = mix(previousField, currentField, smoothstep(0.0, 1.0, uTransition));
    gl_FragColor = vec4(color, 1.0);
}
