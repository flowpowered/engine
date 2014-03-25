// $shader_type: vertex

#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 textureCoords;
layout(location = 3) in vec4 tangent;

out vec4 positionClip;
out vec4 previousPositionClip;
out vec3 normalView;
out vec2 textureUV;
out mat3 tangentMatrix;

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

    textureUV = textureCoords;

    normalView = (normalMatrix * vec4(normal, 0)).xyz;
    vec3 tangentView = (normalMatrix * vec4(tangent.xyz, 0)).xyz;
    vec3 biTangentView = cross(normalView, tangentView) * tangent.w;
    tangentMatrix = mat3(tangentView, biTangentView, normalView);

    gl_Position = positionClip;
}
