// $shader_type: vertex

#version 330

layout(location = 0) in vec3 position;

out vec2 textureUV;
noperspective out vec3 viewRay;
out vec3 lightDirectionView;

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
