#version 300 es

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec4 aColor;

uniform vec2 uViewportSize;

out vec2 vUV;
out vec4 vColor;

void main() {
    vec2 ndc = vec2(
        (aPosition.x / uViewportSize.x) * 2.0 - 1.0,
        1.0 - (aPosition.y / uViewportSize.y) * 2.0
    );

    gl_Position = vec4(ndc, 0.0, 1.0);

    vUV = aUV;
    vColor = aColor;
}