#version 300 es
precision mediump float;

// Only the interpolated vertex color, for large plain areas (e.g. a fullscreen gradient) where
// the per pixel cost of the UI shader adds up.

in vec2 vUV;
in vec4 vColor;

uniform vec4 uColor;

out vec4 fragColor;

void main() {
    fragColor = uColor * vColor;
}
