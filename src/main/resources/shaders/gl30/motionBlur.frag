// $shader_type: fragment

// $texture_layout: colors = 0
// $texture_layout: velocities = 1

#version 330

in vec2 textureUV;

layout(location = 0) out vec4 outputColor;

uniform sampler2D colors;
uniform sampler2D velocities;
uniform vec2 resolution;
uniform int sampleCount;
uniform float blurStrength;

void main() {
    vec4 color = texture(colors, textureUV);
    if (color.a <= 0) {
    	outputColor = color;
    	return;
    }

    vec2 velocity = texture(velocities, textureUV).rg * blurStrength;

    float speed = length(resolution / velocity);
    int samples = clamp(int(speed), 1, sampleCount);

    for (int i = 1; i < samples; i++) {
        color += texture(colors, textureUV + velocity * (float(i) / (samples - 1) - 0.5));
    }

    outputColor = color / samples;
}
