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

vec2 twist(vec2 uv, vec2 center, float strength, float radius) {
    vec2 delta = uv - center;
    float distanceFromCenter = length(delta);
    float influence = smoothstep(radius, 0.0, distanceFromCenter);
    float angle = influence * strength;
    return center + rotate(delta, angle);
}

float bandMask(vec2 uv, vec2 normal, float center, float halfWidth, float feather) {
    float axis = dot(uv - 0.5, normalize(normal));
    float distanceToBand = abs(axis - center);
    return 1.0 - smoothstep(halfWidth, halfWidth + feather, distanceToBand);
}

float sideReveal(vec2 uv, vec2 direction, float threshold, float feather) {
    float axis = dot(uv, normalize(direction));
    return 1.0 - smoothstep(threshold, threshold + feather, axis);
}

float wedgeMask(
    vec2 uv,
    vec2 bandDirection,
    float center,
    float halfWidth,
    float feather,
    vec2 revealDirection,
    float revealThreshold
) {
    float band = bandMask(uv, bandDirection, center, halfWidth, feather);
    float reveal = sideReveal(uv, revealDirection, revealThreshold, feather * 2.0);
    return band * reveal;
}

vec2 mirroredUv(vec2 uv) {
    vec2 tiled = fract(uv);
    return mix(vec2(0.002), vec2(0.998), abs(tiled * 2.0 - 1.0));
}

vec4 sampleLayer(
    sampler2D textureSampler,
    vec2 uv,
    float aspect,
    vec4 seed,
    float time,
    float scale,
    float twistStrength,
    float speed
) {
    vec2 baseUv = coverUv(uv, aspect);
    vec2 centered = baseUv - 0.5;
    float angle = (seed.z - 0.5) * 0.3 + time * speed * (seed.w - 0.5);
    vec2 rotated = rotate(centered, angle);
    vec2 drift = vec2(
        sin(time * (0.12 + seed.x * 0.08) + seed.y * 6.2831),
        cos(time * (0.10 + seed.z * 0.07) + seed.w * 6.2831)
    ) * (0.05 + seed.x * 0.03);
    vec2 scaled = rotated * scale + 0.5 + drift;
    vec2 twistCenter = vec2(
        0.5 + (seed.x - 0.5) * 0.22,
        0.5 + (seed.y - 0.5) * 0.22
    );
    vec2 warped = twist(scaled, twistCenter, twistStrength, 0.62 + seed.z * 0.18);
    return texture2D(textureSampler, mirroredUv(warped));
}

vec4 sampleArtworkField(
    sampler2D textureSampler,
    vec2 uv,
    float aspect,
    vec4 seed,
    float time
) {
    vec4 base = sampleLayer(textureSampler, uv, aspect, seed, time, 1.28, 0.08 + seed.x * 0.06, 0.018);
    vec4 sliceA = sampleLayer(textureSampler, uv, aspect, seed.yzwx, time, 1.84, -0.22 - seed.y * 0.10, 0.011);
    vec4 sliceB = sampleLayer(textureSampler, uv, aspect, seed.zwxy, time, 1.68, 0.24 + seed.z * 0.10, 0.010);
    vec4 sliceC = sampleLayer(textureSampler, uv, aspect, seed.wxyz, time, 1.96, -0.30 - seed.w * 0.12, 0.009);
    vec4 sliceD = sampleLayer(textureSampler, uv, aspect, seed.xzyw, time, 1.56, 0.18 + seed.x * 0.10, 0.012);

    vec2 uvA = uv + vec2(sin(time * 0.08 + seed.x * 6.2831) * 0.018, cos(time * 0.04 + seed.y * 6.2831) * 0.010);
    vec2 uvB = uv + vec2(cos(time * 0.05 + seed.z * 6.2831) * 0.014, cos(time * 0.06 + seed.w * 6.2831) * 0.015);
    vec2 uvC = uv + vec2(sin(time * 0.03 + seed.y * 6.2831) * 0.012, sin(time * 0.07 + seed.z * 6.2831) * 0.017);
    vec2 uvD = uv + vec2(cos(time * 0.09 + seed.w * 6.2831) * 0.016, sin(time * 0.05 + seed.x * 6.2831) * 0.010);

    float maskA = wedgeMask(
        uvA,
        vec2(0.95, -0.30),
        -0.12 + seed.x * 0.12,
        0.22 + seed.y * 0.06,
        0.030,
        vec2(1.0, 0.0),
        0.52
    );

    float maskB = wedgeMask(
        uvB,
        vec2(-0.76, -0.64),
        0.02 - seed.z * 0.10,
        0.18 + seed.w * 0.05,
        0.030,
        vec2(0.0, 1.0),
        0.54
    );

    float maskC = wedgeMask(
        uvC,
        vec2(-0.22, 0.98),
        0.16 - seed.y * 0.10,
        0.15 + seed.x * 0.04,
        0.028,
        vec2(-1.0, 0.0),
        0.48
    );

    float maskD = wedgeMask(
        uvD,
        vec2(0.54, 0.84),
        -0.18 + seed.w * 0.10,
        0.21 + seed.z * 0.05,
        0.032,
        vec2(0.0, -1.0),
        0.50
    );

    float used = 0.0;
    maskA *= 1.0 - used;
    used = max(used, maskA);
    maskB *= 1.0 - used;
    used = max(used, maskB);
    maskC *= 1.0 - used;
    used = max(used, maskC);
    maskD *= 1.0 - used;

    vec3 combined = base.rgb;
    combined = mix(combined, sliceA.rgb, maskA);
    combined = mix(combined, sliceB.rgb, maskB);
    combined = mix(combined, sliceC.rgb, maskC);
    combined = mix(combined, sliceD.rgb, maskD);

    float luminance = dot(combined, vec3(0.2126, 0.7152, 0.0722));
    combined = mix(combined, vec3(luminance), 0.01);

    return vec4(combined, 1.0);
}

void main() {
    vec4 previousField = sampleArtworkField(uPreviousTexture, vTexCoord, uPreviousAspect, uPreviousSeed, uTime);
    vec4 currentField = sampleArtworkField(uCurrentTexture, vTexCoord, uCurrentAspect, uCurrentSeed, uTime);
    vec3 color = mix(previousField, currentField, smoothstep(0.0, 1.0, uTransition)).rgb;
    gl_FragColor = vec4(color, 1.0);
}
