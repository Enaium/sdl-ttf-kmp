#version 450

// Text vertex shader: per-vertex pixel position (location 0, positive Y
// upwards, matching TTF_GetGPUTextDrawData) and texture coordinate
// (location 1). Positions are pre-transformed to NDC on the CPU.

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 uv;

layout(location = 0) out vec2 vUV;

void main() {
    gl_Position = vec4(position, 0.0, 1.0);
    vUV = uv;
}
