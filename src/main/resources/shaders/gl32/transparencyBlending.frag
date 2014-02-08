// $shader_type: fragment

// $texture_layout: weightedColor = 0
// $texture_layout: layerCount = 1

#version 330

in vec2 textureUV;

layout(location = 0) out vec4 outputColor;

uniform sampler2D weightedColor;
uniform sampler2D weightedVelocity;
uniform sampler2D layerCount;

void main() {
    vec4 colorSum = texture(weightedColor, textureUV);
    float count = texture(layerCount, textureUV).r;

    if (count < 0.00001 || colorSum.a < 0.00001) {
        discard;
    }

    float destinationAlpha = pow(max(0, 1 - colorSum.a / count), count);

    outputColor = vec4(colorSum.rgb / colorSum.a, destinationAlpha);
}
