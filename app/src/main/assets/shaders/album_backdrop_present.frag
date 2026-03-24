precision mediump float;

uniform sampler2D uTexture;
uniform float uSaturation;

varying vec2 vTexCoord;

vec4 sampleOverlay(float t) {
    vec4 c0 = vec4(8.0 / 255.0, 9.0 / 255.0, 12.0 / 255.0, 40.0 / 255.0);
    vec4 c1 = vec4(9.0 / 255.0, 10.0 / 255.0, 12.0 / 255.0, 16.0 / 255.0);
    vec4 c2 = vec4(6.0 / 255.0, 7.0 / 255.0, 9.0 / 255.0, 50.0 / 255.0);

    if (t < 0.5) {
        return mix(c0, c1, t * 2.0);
    }
    return mix(c1, c2, (t - 0.5) * 2.0);
}

void main() {
    vec3 color = texture2D(uTexture, vTexCoord).rgb;

    if (uSaturation > 1.01) {
        float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(vec3(lum), color, uSaturation);
    }

    float vignette = smoothstep(1.16, 0.12, distance(vTexCoord, vec2(0.5)));
    color *= mix(0.92, 1.0, vignette);

    float gradientT = clamp((vTexCoord.x + vTexCoord.y) * 0.5, 0.0, 1.0);
    vec4 overlay = sampleOverlay(gradientT);
    color = mix(color, overlay.rgb, overlay.a);

    gl_FragColor = vec4(min(color, vec3(0.98)), 1.0);
}
