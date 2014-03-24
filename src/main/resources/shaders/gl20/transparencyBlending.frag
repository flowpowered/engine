// $shader_type: fragment

// $texture_layout: weightedColor = 0
// $texture_layout: layerCount = 1

#version 120

varying vec2 textureUV;

uniform sampler2D weightedColor;
uniform sampler2D layerCount;

void main() {
    vec4 colorSum = texture2D(weightedColor, textureUV);
    float count = texture2D(layerCount, textureUV).r;

    if (count < 0.00001 || colorSum.a < 0.00001) {
        discard;
    }

    float destinationAlpha = pow(max(0, 1 - colorSum.a / count), count);

    gl_FragData[0] = vec4(colorSum.rgb / colorSum.a, destinationAlpha);
}
