// $shader_type: fragment

#version 330

in vec4 positionClip;
in vec4 previousPositionClip;
in vec3 normalView;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec4 outputNormal;
layout(location = 2) out vec4 outputVertexNormal;
layout(location = 3) out vec4 outputMaterial;
layout(location = 4) out vec2 outputVelocity;

uniform vec4 modelColor;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;
uniform float shininess;

void main() {
    outputColor = modelColor;

    outputNormal = vec4((normalView + 1) / 2, 1);

    outputVertexNormal = outputNormal;

    outputMaterial = vec4(diffuseIntensity, specularIntensity, ambientIntensity, shininess);

    outputVelocity = (positionClip.xy / positionClip.w - previousPositionClip.xy / previousPositionClip.w) * 0.5;
}
