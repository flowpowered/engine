// $shader_type: vertex

#version 330

layout(location = 0) in vec3 position;

out vec2 textureUV;

void main() {
    textureUV = (position.xy + 1) / 2;

    gl_Position = vec4(position, 1);
}
