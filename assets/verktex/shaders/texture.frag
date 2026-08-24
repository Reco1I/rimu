#version 300 es
precision mediump float;

in vec2 vUV;
in vec4 vColor;

uniform sampler2D uTexture;

uniform float uRadius;
uniform vec2 uSize;

out vec4 fragColor;


float roundedRect(vec2 p, vec2 size, float r) {
    vec2 d = abs(p - size * 0.5) - (size * 0.5 - r);
    float outside = length(max(d, 0.0)) - r;
    return smoothstep(0.0, 1.0, -outside);
}

void main() {

    vec4 tex = texture(uTexture, vUV);
    vec2 p = vUV * uSize;

    float mask = roundedRect(p, uSize, uRadius);
    vec4 color = tex * vColor;

    color.a *= mask;
    fragColor = color;
}