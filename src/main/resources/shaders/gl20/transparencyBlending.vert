// $shader_type: vertex

// $attrib_layout: position = 0

#version 120

attribute vec3 position;

varying vec2 textureUV;

void main() {
    textureUV = (position.xy + 1) / 2;

    gl_Position = vec4(position, 1);
}
