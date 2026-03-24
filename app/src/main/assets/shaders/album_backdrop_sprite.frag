precision mediump float;

uniform sampler2D uTexture;
uniform float uTextureAspect;

varying vec2 vTexCoord;

void main() {
    vec2 artUv = vTexCoord;

    if (uTextureAspect > 1.0) {
        artUv.x = (vTexCoord.x - 0.5) / uTextureAspect + 0.5;
    } else {
        artUv.y = (vTexCoord.y - 0.5) * uTextureAspect + 0.5;
    }

    gl_FragColor = vec4(texture2D(uTexture, clamp(artUv, 0.0, 1.0)).rgb, 1.0);
}
