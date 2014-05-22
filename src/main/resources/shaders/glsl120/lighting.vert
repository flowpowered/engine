// $shader_type: vertex

// $attrib_layout: position = 0

#version 120

attribute vec3 position;

varying vec2 textureUV;
varying vec3 viewRay;
varying vec3 lightDirectionView;

uniform mat4 viewMatrix;
uniform mat4 normalMatrix;
uniform vec3 lightDirection;
uniform float tanHalfFOV;
uniform float aspectRatio;

void main() {
    textureUV = (position.xy + 1) / 2;

    viewRay = vec3(position.x * tanHalfFOV * aspectRatio, position.y * tanHalfFOV, -1);

    lightDirectionView = normalize((normalMatrix * vec4(lightDirection, 1)).xyz);

    gl_Position = vec4(position, 1);
}
