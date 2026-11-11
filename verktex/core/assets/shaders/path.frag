#version 300 es
precision mediump float;

// One band of a path, see com.reco1l.verktex.ui.UIPath: what's within uMaxDistance of the centre
// line, in the path's width, in a single colour.

in float vDistance;

uniform vec4 uColor;
uniform float uMaxDistance;

out vec4 fragColor;

// Colours given to shaders have straight alpha, the output is premultiplied.

void main() {
    if (vDistance > uMaxDistance) {
        discard;
    }

    fragColor = vec4(uColor.rgb * uColor.a, uColor.a);
}
