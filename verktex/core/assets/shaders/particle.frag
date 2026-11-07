#version 300 es
precision mediump float;

in vec2 vUV;
in vec4 vColor;

uniform sampler2D uTexture;

out vec4 fragColor;

// Colours given to shaders have straight alpha, textures and the output are premultiplied.

void main() {
    fragColor = texture(uTexture, vUV) * vec4(vColor.rgb * vColor.a, vColor.a);
}
