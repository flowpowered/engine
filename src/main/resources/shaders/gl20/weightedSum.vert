// $shader_type: vertex

// $attrib_layout: position = 0
// $attrib_layout: normal = 1

#version 120

attribute vec3 position;
attribute vec3 normal;

varying vec3 positionView;
varying vec3 normalView;
varying vec3 lightDirectionView;

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
