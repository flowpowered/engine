// $shader_type: fragment

// $texture_layout: colors = 0

#version 120

const int MAX_KERNEL_SIZE = 51;

varying vec2 textureUV;

uniform sampler2D colors;
uniform float[MAX_KERNEL_SIZE] offsets;
uniform int kernelSize;
uniform float[MAX_KERNEL_SIZE] kernel;
uniform vec2 resolution;
uniform bool direction;

void main() {
    gl_FragColor = texture2D(colors, textureUV) * offsets[0];
    if (!direction) {
        for (int i = 1; i < kernelSize; i++) {
            vec2 offset = vec2(offsets[i], 0) / resolution.x;
            gl_FragColor += texture2D(colors, textureUV + offset) * kernel[i];
            gl_FragColor += texture2D(colors, textureUV - offset) * kernel[i];
        }
    } else {
        for (int i = 1; i < kernelSize; i++) {
            vec2 offset = vec2(0, offsets[i]) / resolution.y;
            gl_FragColor += texture2D(colors, textureUV + offset) * kernel[i];
            gl_FragColor += texture2D(colors, textureUV - offset) * kernel[i];
        }
    }
}
