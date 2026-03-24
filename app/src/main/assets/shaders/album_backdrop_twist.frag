precision mediump float;

uniform sampler2D uPreviousTexture;
uniform sampler2D uCurrentTexture;
uniform vec2 uResolution;
uniform float uTransition;
uniform float uAngle;
uniform float uRadius;
uniform vec2 uOffset;
uniform float uCrop;

varying vec2 vTexCoord;

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

float mirror01(float v) {
    float t = fract(v * 0.5) * 2.0;
    return 1.0 - abs(t - 1.0);
}

vec2 mirrorUv(vec2 uv) {
    return vec2(mirror01(uv.x), mirror01(uv.y));
}

void main() {
    float transition = smoothstep(0.0, 1.0, uTransition);
    vec2 zoomUv = (vTexCoord - 0.5) / uCrop + 0.5;
    vec2 twistedCoord = twist(zoomUv * uResolution, uOffset, uRadius, uAngle);
    vec2 sampleUv = mirrorUv(twistedCoord / uResolution);

    vec3 previousColor = texture2D(uPreviousTexture, sampleUv).rgb;
    vec3 currentColor = texture2D(uCurrentTexture, sampleUv).rgb;
    gl_FragColor = vec4(mix(previousColor, currentColor, transition), 1.0);
}
