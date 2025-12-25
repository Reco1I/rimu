#version 300 es
precision mediump float;

in vec2 vUV;
in vec4 vColor;

uniform sampler2D uTexture;

uniform float uRadius;
uniform vec2 uSize;

out vec4 fragColor;


float roundedRectMask(vec2 p, vec2 size, float r) {
    vec2 q = abs(p - size * 0.5) - (size * 0.5 - r);
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    return clamp(0.5 - dist, 0.0, 1.0);
}

void main() {

    vec4 color = texture(uTexture, vUV) * vColor;

    if (uRadius > 0.0) {
        color.a *= roundedRectMask(vUV * uSize, uSize, uRadius);
    }

    fragColor = color;
}
