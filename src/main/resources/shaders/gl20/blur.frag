// $shader_type: fragment

// $texture_layout: occlusions = 0
// $texture_layout: shadows = 1

#version 120

varying vec2 textureUV;

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
            blurredOcclusion += texture2D(occlusions, adjacentUV).r;
            blurredShadow += texture2D(shadows, adjacentUV).r;
        }
    }
    float blurSizeSquared = blurSize * blurSize;
    blurredOcclusion /= blurSizeSquared;
    blurredShadow /= blurSizeSquared;
    gl_FragData[0] = vec4(blurredOcclusion, blurredOcclusion, blurredOcclusion, 1);
    gl_FragData[1] = vec4(blurredShadow, blurredShadow, blurredShadow, 1);
}
