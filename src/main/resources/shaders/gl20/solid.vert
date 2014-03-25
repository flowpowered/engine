// $shader_type: vertex

// $attrib_layout: position = 0
// $attrib_layout: normal = 1

#version 120

attribute vec3 position;
attribute vec3 normal;

varying vec4 positionClip;
varying vec4 previousPositionClip;
varying vec3 normalView;

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
