precision mediump float;

uniform sampler2D uTexture;
uniform vec2 uTexelSize;
uniform float uApplyVignette;
uniform float uKawaseOffset;

varying vec2 vTexCoord;

// Kawase blur: samples the center and four diagonal neighbors at a
// configurable offset. Each pass widens the effective radius without
// the tiling/refraction artifacts of a large-kernel Gaussian at low res.
void main() {
    float off = uKawaseOffset + 0.5;
    vec2 ts = uTexelSize;

    vec3 color = texture2D(uTexture, vTexCoord).rgb;
    color += texture2D(uTexture, vTexCoord + vec2(-off, -off) * ts).rgb;
    color += texture2D(uTexture, vTexCoord + vec2( off, -off) * ts).rgb;
    color += texture2D(uTexture, vTexCoord + vec2(-off,  off) * ts).rgb;
    color += texture2D(uTexture, vTexCoord + vec2( off,  off) * ts).rgb;
    color /= 5.0;

    if (uApplyVignette > 0.5) {
        float vignette = smoothstep(1.16, 0.12, distance(vTexCoord, vec2(0.5)));
        color *= mix(0.92, 1.0, vignette);
    }

    gl_FragColor = vec4(min(color, vec3(0.98)), 1.0);
}
