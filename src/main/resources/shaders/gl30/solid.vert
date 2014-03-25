// $shader_type: vertex

#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

out vec4 positionClip;
out vec4 previousPositionClip;
out vec3 normalView;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 normalMatrix;
uniform mat4 projectionMatrix;
uniform mat4 previousModelMatrix;
uniform mat4 previousViewMatrix;
uniform mat4 previousProjectionMatrix;

void main() {
    positionClip = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1);

    previousPositionClip = previousProjectionMatrix * previousViewMatrix * previousModelMatrix * vec4(position, 1);

    normalView = (normalMatrix * vec4(normal, 0)).xyz;

    gl_Position = positionClip;
}
