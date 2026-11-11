#version 300 es

// A vertex of a path's mesh, see com.reco1l.verktex.ui.UIPath.
layout(location = 0) in vec2 aPosition;

// Its distance from the centre line, 0 on it and 1 at the edge of the path.
layout(location = 1) in float aDistance;

uniform vec2 uViewportSize;
uniform mat4 uTransform;

out float vDistance;

void main() {
    vec4 transformed = uTransform * vec4(aPosition, 0.0, 1.0);

    // Every vertex has the same depth, so that the first pass drawing a pixel keeps it.
    gl_Position = vec4(
        (transformed.x / uViewportSize.x) * 2.0 - 1.0,
        1.0 - (transformed.y / uViewportSize.y) * 2.0,
        0.0,
        1.0
    );

    vDistance = aDistance;
}
