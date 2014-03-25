// $shader_type: fragment

// $texture_layout: colors = 0

#version 330

const int MAX_KERNEL_SIZE = 51;

in vec2 textureUV;

layout(location = 0) out vec4 outputColor;

uniform sampler2D colors;
uniform float[MAX_KERNEL_SIZE] offsets;
uniform int kernelSize;
uniform float[MAX_KERNEL_SIZE] kernel;
uniform vec2 resolution;
uniform bool direction;

void main() {
    outputColor = texture(colors, textureUV) * offsets[0];
    if (!direction) {
        for (int i = 1; i < kernelSize; i++) {
            vec2 offset = vec2(offsets[i], 0) / resolution.x;
            outputColor += texture(colors, textureUV + offset) * kernel[i];
            outputColor += texture(colors, textureUV - offset) * kernel[i];
        }
    } else {
        for (int i = 1; i < kernelSize; i++) {
            vec2 offset = vec2(0, offsets[i]) / resolution.y;
            outputColor += texture(colors, textureUV + offset) * kernel[i];
            outputColor += texture(colors, textureUV - offset) * kernel[i];
        }
    }
}
