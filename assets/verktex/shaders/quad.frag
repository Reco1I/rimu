#version 300 es
precision mediump float;

in vec2 vUV;
in vec4 vColor;

uniform vec4 uColor;
uniform vec4 uBorderColor;
uniform float uBorderWidth;
uniform float uRadius;

out vec4 fragColor;

float roundedRectSDF(vec2 p, float r) {
    vec2 d = abs(p - vec2(0.5)) - vec2(0.5 - r);
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

void main() {
    vec2 p = vUV;

    float r = uRadius;
    float dist = roundedRectSDF(p, r);
    float aa = fwidth(dist);
    float alpha = 1.0 - smoothstep(0.0, aa, dist);
    float border = 1.0 - smoothstep(uBorderWidth - aa, uBorderWidth + aa, abs(dist));

    vec4 innerColor = uColor * vColor;
    vec4 finalColor = innerColor;

    finalColor = mix(innerColor, uBorderColor, border);
    finalColor.a *= alpha;
    fragColor = finalColor;
}