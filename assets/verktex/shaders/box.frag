#version 300 es
precision mediump float;

// A rounded box like quad.frag, with a vertical border gradient (e.g. a bezel lighter on top and
// darker at the bottom) and soft edges (e.g. a shadow).

in vec2 vUV;
in vec4 vColor;

uniform vec4 uColor;
uniform vec4 uBorderColor;
uniform vec4 uBorderColorBottom;
uniform float uBorderWidth;
uniform float uRadius;
uniform vec2 uSize;

// How far the box is inside the quad on each side, so a soft edge can extend past it.
uniform float uInset;

// The width of the soft edge in pixels, antialiasing only when zero.
uniform float uSoftness;

out vec4 fragColor;

// Signed distance to a rounded box centered at the origin, in pixels.
float roundedBoxSDF(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 p = (vUV - 0.5) * uSize;
    vec2 boxSize = uSize - 2.0 * uInset;
    float r = min(uRadius, min(boxSize.x, boxSize.y) * 0.5);

    float dist = roundedBoxSDF(p, boxSize * 0.5, r);
    float aa = max(fwidth(dist), 0.0001);

    float alpha = uSoftness > 0.0
        ? 1.0 - smoothstep(-uSoftness, uSoftness, dist)
        : clamp(0.5 - dist / aa, 0.0, 1.0);

    vec4 fill = uColor * vColor;

    if (uBorderWidth > 0.0) {
        float borderMask = clamp(0.5 + (dist + uBorderWidth) / aa, 0.0, 1.0);

        vec4 border = mix(uBorderColor, uBorderColorBottom, vUV.y);
        border.a *= borderMask;

        // The border goes over the fill, so a translucent border tints it instead of showing
        // what is behind the box.
        float outAlpha = border.a + fill.a * (1.0 - border.a);
        vec3 outColor = outAlpha > 0.0
            ? (border.rgb * border.a + fill.rgb * fill.a * (1.0 - border.a)) / outAlpha
            : fill.rgb;

        fill = vec4(outColor, outAlpha);
    }

    fill.a *= alpha;
    fragColor = fill;
}
