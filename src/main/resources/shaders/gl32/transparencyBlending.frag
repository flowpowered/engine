// $shader_type: fragment

// $texture_layout: weightedColor = 0
// $texture_layout: weightedVelocity = 1
// $texture_layout: layerCount = 2

#version 330

in vec2 textureUV;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec4 outputVelocity;

uniform sampler2D weightedColor;
uniform sampler2D weightedVelocity;
uniform sampler2D layerCount;

void main() {
    vec4 colorSum = texture(weightedColor, textureUV);
    vec2 velocitySum = texture(weightedVelocity, textureUV).rg;
    float count = texture(layerCount, textureUV).r;

    if (count < 0.00001 || colorSum.a < 0.00001) {
        discard;
    }

    float destinationAlpha = pow(max(0, 1 - colorSum.a / count), count);

    outputColor = vec4(colorSum.rgb / colorSum.a, destinationAlpha);
    outputVelocity = vec4(velocitySum.rg / colorSum.a, 0, destinationAlpha);
}
