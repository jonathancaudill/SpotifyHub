precision mediump float;

uniform sampler2D uTexture;
uniform vec2 uTexelSize;
uniform vec2 uDirection;
uniform float uApplyVignette;
uniform float uBlurScale;

varying vec2 vTexCoord;

void main() {
    vec2 step = uDirection * uTexelSize * uBlurScale;

    vec3 color = texture2D(uTexture, vTexCoord).rgb * 0.204164;
    color += texture2D(uTexture, vTexCoord + step * 1.0).rgb * 0.180174;
    color += texture2D(uTexture, vTexCoord - step * 1.0).rgb * 0.180174;
    color += texture2D(uTexture, vTexCoord + step * 2.0).rgb * 0.123832;
    color += texture2D(uTexture, vTexCoord - step * 2.0).rgb * 0.123832;
    color += texture2D(uTexture, vTexCoord + step * 3.0).rgb * 0.066282;
    color += texture2D(uTexture, vTexCoord - step * 3.0).rgb * 0.066282;
    color += texture2D(uTexture, vTexCoord + step * 4.0).rgb * 0.027631;
    color += texture2D(uTexture, vTexCoord - step * 4.0).rgb * 0.027631;

    if (uApplyVignette > 0.5) {
        float vignette = smoothstep(1.16, 0.12, distance(vTexCoord, vec2(0.5)));
        color *= mix(0.95, 1.0, vignette);
    }

    gl_FragColor = vec4(min(color, vec3(0.98)), 1.0);
}
