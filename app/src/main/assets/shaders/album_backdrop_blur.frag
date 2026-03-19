precision mediump float;

uniform sampler2D uTexture;
uniform vec2 uTexelSize;
uniform float uApplyVignette;
uniform float uKawaseOffset;
uniform float uSaturation;     // 1.0 = no change, >1 = boost (applied on final pass)

varying vec2 vTexCoord;

// Kawase blur: samples center + four diagonal neighbors.
// Stacking multiple passes with progressively larger offsets
// (5, 10, 20, 40, 80) achieves a very heavy blur efficiently.
void main() {
    float off = uKawaseOffset + 0.5;
    vec2 ts = uTexelSize;

    vec3 color = texture2D(uTexture, vTexCoord).rgb;
    color += texture2D(uTexture, vTexCoord + vec2(-off, -off) * ts).rgb;
    color += texture2D(uTexture, vTexCoord + vec2( off, -off) * ts).rgb;
    color += texture2D(uTexture, vTexCoord + vec2(-off,  off) * ts).rgb;
    color += texture2D(uTexture, vTexCoord + vec2( off,  off) * ts).rgb;
    color /= 5.0;

    // Saturation boost (only on the final pass)
    if (uSaturation > 1.01) {
        float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(vec3(lum), color, uSaturation);
    }

    // Vignette (only on the final pass)
    if (uApplyVignette > 0.5) {
        float vignette = smoothstep(1.16, 0.12, distance(vTexCoord, vec2(0.5)));
        color *= mix(0.92, 1.0, vignette);
    }

    gl_FragColor = vec4(min(color, vec3(0.98)), 1.0);
}
