#version 300 es
precision mediump float;

// A path's colour by the distance from its centre line, see com.reco1l.verktex.ui.UIPath. The
// distances are out of the mesh's radius: the hint up to uHintEnd, the body up to uBodyEnd going
// from uBodyCenterColor to uBodyColor, the border up to uBorderEnd and the shadow up to 1, fading out.

in float vDistance;

uniform vec4 uHintColor;
uniform vec4 uBodyCenterColor;
uniform vec4 uBodyColor;
uniform vec4 uBorderColor;
uniform vec4 uShadowColor;
uniform float uHintEnd;
uniform float uBodyEnd;
uniform float uBorderEnd;

out vec4 fragColor;

// Colours given to shaders have straight alpha, the output is premultiplied.

void main() {
    float distance = vDistance;
    vec4 color;

    if (distance < uHintEnd) {
        color = uHintColor;
    } else if (distance <= uBodyEnd) {
        color = uBodyColor;

        if (uBodyCenterColor != uBodyColor) {
            float progress = clamp(distance / max(uBodyEnd, 0.0001), 0.0, 1.0);

            // In linear light, as osu!lazer interpolates colours.
            vec3 center = pow(uBodyCenterColor.rgb, vec3(2.2));
            vec3 edge = pow(uBodyColor.rgb, vec3(2.2));

            color = vec4(pow(mix(center, edge, progress), vec3(1.0 / 2.2)), mix(uBodyCenterColor.a, uBodyColor.a, progress));
        }
    } else if (distance <= uBorderEnd) {
        color = uBorderColor;
    } else {
        float progress = clamp((distance - uBorderEnd) / max(1.0 - uBorderEnd, 0.0001), 0.0, 1.0);
        color = vec4(uShadowColor.rgb, uShadowColor.a * (1.0 - progress));
    }

    fragColor = vec4(color.rgb * color.a, color.a);
}
