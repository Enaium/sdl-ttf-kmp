#version 450

// SDF text fragment shader: the atlas alpha channel holds the signed
// distance; smoothstep at the 0.5 edge turns it into a crisp anti-aliased
// outline that stays sharp at any scale. Tinted by the vertex color.

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 vColor;
layout(set = 0, binding = 0) uniform sampler2D atlas;

layout(location = 0) out vec4 outColor;

void main() {
    float d = texture(atlas, vUV).a;
    float alpha = smoothstep(0.4375, 0.5625, d);
    outColor = vec4(vColor.rgb, alpha);
}
