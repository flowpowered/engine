// $shader_type: fragment

// $texture_layout: occlusions = 0
// $texture_layout: shadows = 1

#version 330

in vec2 textureUV;

layout(location = 0) out float outputOcclusion;
layout(location = 1) out float outputShadow;

uniform sampler2D occlusions;
uniform sampler2D shadows;
uniform int blurSize;
uniform vec2 texelSize;

void main() {
    float blurredOcclusion = 0;
    float blurredShadow = 0;
    float halfBlurSize = float(blurSize) / 2;
    int blurStart = int(-floor(halfBlurSize));
    int blurEnd = int(ceil(halfBlurSize));
    for (int x = blurStart; x < blurEnd; x++) {
        for (int y = blurStart; y < blurEnd; y++) {
            vec2 adjacentUV = textureUV + vec2(x * texelSize.x, y * texelSize.y);
            blurredOcclusion += texture(occlusions, adjacentUV).r;
            blurredShadow += texture(shadows, adjacentUV).r;
        }
    }
    float blurSizeSquared = blurSize * blurSize;
    outputOcclusion = blurredOcclusion / blurSizeSquared;
    outputShadow = blurredShadow / blurSizeSquared;
}
