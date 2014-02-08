// $shader_type: vertex

#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

out vec3 positionView;
out vec3 normalView;
out vec3 lightDirectionView;

uniform vec3 lightDirection;
uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 normalMatrix;
uniform mat4 projectionMatrix;

void main() {
    normalView = (normalMatrix * vec4(normal, 0)).xyz;

    lightDirectionView = (normalMatrix * vec4(lightDirection, 0)).xyz;

    positionView = (viewMatrix * modelMatrix * vec4(position, 1)).xyz;

    gl_Position = projectionMatrix * vec4(positionView, 1);
}
