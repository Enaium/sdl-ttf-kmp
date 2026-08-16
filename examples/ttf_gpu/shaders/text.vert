#version 450

// Text vertex shader: per-vertex pixel position (location 0, positive Y
// upwards, matching TTF_GetGPUTextDrawData), texture coordinate (location 1)
// and color (location 2). Positions are pre-transformed to NDC on the CPU;
// the color tints the atlas glyphs (the atlas itself is white).

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 uv;
layout(location = 2) in vec4 color;

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec4 vColor;

void main() {
    gl_Position = vec4(position, 0.0, 1.0);
    vUV = uv;
    vColor = color;
}
