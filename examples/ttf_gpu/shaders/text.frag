#version 450

// Text fragment shader: sample the glyph atlas texture (alpha is the
// coverage for ALPHA/COLOR image types) and multiply by the vertex color.

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 vColor;
layout(set = 0, binding = 0) uniform sampler2D atlas;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = vColor * texture(atlas, vUV);
}
