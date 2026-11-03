#version 300 es

// Per vertex: the corner of the quad, between -0.5 and 0.5.
layout(location = 0) in vec2 aCorner;

// Per particle.
layout(location = 1) in vec2 aPosition;
layout(location = 2) in vec2 aVelocity;
layout(location = 3) in vec2 aAcceleration;
layout(location = 4) in vec2 aLife;      // Time it was born at, and how long it lasts.
layout(location = 5) in vec2 aScale;     // At the start and at the end of its life.
layout(location = 6) in vec2 aAlpha;     // At the start and at the end of its life.
layout(location = 7) in vec2 aRotation;  // In radians, and in radians per second.
layout(location = 8) in vec4 aColor;

uniform vec2 uViewportSize;
uniform mat4 uTransform;
uniform float uTime;
uniform vec2 uParticleSize;
uniform vec4 uUVRect;
uniform vec4 uColorMultiplier;

out vec2 vUV;
out vec4 vColor;

void main() {
    float age = uTime - aLife.x;
    float progress = clamp(age / aLife.y, 0.0, 1.0);

    float angle = aRotation.x + aRotation.y * age;
    float cosine = cos(angle);
    float sine = sin(angle);

    vec2 corner = aCorner * uParticleSize * mix(aScale.x, aScale.y, progress);
    corner = vec2(corner.x * cosine - corner.y * sine, corner.x * sine + corner.y * cosine);

    vec2 position = aPosition + aVelocity * age + 0.5 * aAcceleration * age * age + corner;
    vec4 transformed = uTransform * vec4(position, 0.0, 1.0);

    gl_Position = vec4(
        (transformed.x / uViewportSize.x) * 2.0 - 1.0,
        1.0 - (transformed.y / uViewportSize.y) * 2.0,
        0.0,
        1.0
    );

    vUV = mix(uUVRect.xy, uUVRect.zw, aCorner + 0.5);
    vColor = aColor * uColorMultiplier;
    vColor.a *= mix(aAlpha.x, aAlpha.y, progress);
}
