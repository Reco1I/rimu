#version 300 es
precision mediump float;

in vec2 vUV;
in vec4 vColor;

uniform sampler2D uTexture;

// One step between taps in UV space, along the blurred axis.
uniform vec2 uDirection;

out vec4 fragColor;

// Colours given to shaders have straight alpha, textures and the output are premultiplied.

// A 9-tap Gaussian (sigma of 2 steps) in 5 fetches, linear filtering blends each pair of taps.
const float OFFSET_1 = 1.3846153846;
const float OFFSET_2 = 3.2307692308;
const float WEIGHT_0 = 0.2270270270;
const float WEIGHT_1 = 0.3162162162;
const float WEIGHT_2 = 0.0702702703;

void main() {
    vec3 color = texture(uTexture, vUV).rgb * WEIGHT_0;
    color += texture(uTexture, vUV + uDirection * OFFSET_1).rgb * WEIGHT_1;
    color += texture(uTexture, vUV - uDirection * OFFSET_1).rgb * WEIGHT_1;
    color += texture(uTexture, vUV + uDirection * OFFSET_2).rgb * WEIGHT_2;
    color += texture(uTexture, vUV - uDirection * OFFSET_2).rgb * WEIGHT_2;

    fragColor = vec4(color, 1.0) * vec4(vColor.rgb * vColor.a, vColor.a);
}
