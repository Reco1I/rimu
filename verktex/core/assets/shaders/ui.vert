#version 300 es

// Boxes and text drawn in the same batches, see ui.frag. Everything describing a box is a vertex
// attribute instead of a uniform, so boxes of any shape share a draw call.

layout(location = 0) in vec2 aPosition;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec4 aColor;
layout(location = 3) in vec2 aBoxSize;
// Radius, border width, softness and inset.
layout(location = 4) in vec4 aBoxParams;
layout(location = 5) in vec4 aBorderColor;
layout(location = 6) in vec4 aBorderColorBottom;
// 0 draws a rounded box, 1 samples the texture.
layout(location = 7) in float aMode;

uniform vec2 uViewportSize;

out vec2 vUV;
out vec4 vColor;
flat out vec2 vBoxSize;
flat out vec4 vBoxParams;
flat out vec4 vBorderColor;
flat out vec4 vBorderColorBottom;
flat out float vMode;

void main() {
    vec2 ndc = vec2(
        (aPosition.x / uViewportSize.x) * 2.0 - 1.0,
        1.0 - (aPosition.y / uViewportSize.y) * 2.0
    );

    gl_Position = vec4(ndc, 0.0, 1.0);

    vUV = aUV;
    vColor = aColor;
    vBoxSize = aBoxSize;
    vBoxParams = aBoxParams;
    vBorderColor = aBorderColor;
    vBorderColorBottom = aBorderColorBottom;
    vMode = aMode;
}
