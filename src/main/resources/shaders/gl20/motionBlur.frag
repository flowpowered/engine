// $shader_type: fragment

// $texture_layout: colors = 0
// $texture_layout: velocities = 1

#version 120

varying vec2 textureUV;

uniform sampler2D colors;
uniform sampler2D velocities;
uniform vec2 resolution;
uniform int sampleCount;
uniform float blurStrength;

void main() {
    vec4 color = texture2D(colors, textureUV);
    if (color.a <= 0) {
    	gl_FragColor = color;
    	return;
    }

    vec2 velocity = texture2D(velocities, textureUV).rg * blurStrength;

    float speed = length(resolution / velocity);
    int samples = int(clamp(speed, 1, sampleCount));

    for (int i = 1; i < samples; i++) {
        color += texture2D(colors, textureUV + velocity * (float(i) / (samples - 1) - 0.5));
    }

    gl_FragColor = color / samples;
}
