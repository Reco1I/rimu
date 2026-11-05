#version 300 es
precision mediump float;

in vec2 vUV;
in vec4 vColor;

uniform vec4 uColor;
uniform vec4 uBorderColor;
uniform float uBorderWidth;
uniform float uRadius;
uniform vec2 uSize;

out vec4 fragColor;

// Signed distance to a rounded box centered at the origin, in pixels.
float roundedBoxSDF(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 p = (vUV - 0.5) * uSize;
    float r = min(uRadius, min(uSize.x, uSize.y) * 0.5);

    float dist = roundedBoxSDF(p, uSize * 0.5, r);
    float aa = max(fwidth(dist), 0.0001);

    float alpha = clamp(0.5 - dist / aa, 0.0, 1.0);
    float border = uBorderWidth > 0.0 ? clamp(0.5 + (dist + uBorderWidth) / aa, 0.0, 1.0) : 0.0;

    vec4 finalColor = mix(uColor * vColor, uBorderColor, border);
    finalColor.a *= alpha;
    fragColor = finalColor;
}
