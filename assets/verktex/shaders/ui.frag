#version 300 es
precision highp float;

// A rounded box like box.frag with its parameters per vertex, or a textured quad (text).

in vec2 vUV;
in vec4 vColor;
flat in vec2 vBoxSize;
flat in vec4 vBoxParams;
flat in vec4 vBorderColor;
flat in vec4 vBorderColorBottom;
flat in float vMode;

uniform vec4 uColor;
uniform sampler2D uTexture;

out vec4 fragColor;

// Signed distance to a rounded box centered at the origin, in pixels.
float roundedBoxSDF(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    if (vMode > 0.5) {
        fragColor = texture(uTexture, vUV) * vColor * uColor;
        return;
    }

    float radius = vBoxParams.x;
    float borderWidth = vBoxParams.y;
    float softness = vBoxParams.z;
    float inset = vBoxParams.w;

    vec2 p = (vUV - 0.5) * vBoxSize;
    vec2 boxSize = vBoxSize - 2.0 * inset;
    float r = min(radius, min(boxSize.x, boxSize.y) * 0.5);

    float dist = roundedBoxSDF(p, boxSize * 0.5, r);
    float aa = max(fwidth(dist), 0.0001);

    float alpha = softness > 0.0
        ? 1.0 - smoothstep(-softness, softness, dist)
        : clamp(0.5 - dist / aa, 0.0, 1.0);

    // A negative border width cuts the shape out of a soft edge (a glow): it fades from the edge
    // outwards and nothing is drawn inside.
    if (borderWidth < 0.0 && softness > 0.0) {
        alpha = (1.0 - smoothstep(0.0, softness, dist)) * clamp(0.5 + dist / aa, 0.0, 1.0);
    }

    vec4 fill = uColor * vColor;

    if (borderWidth > 0.0) {
        float borderMask = clamp(0.5 + (dist + borderWidth) / aa, 0.0, 1.0);

        vec4 border = mix(vBorderColor, vBorderColorBottom, vUV.y);
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
