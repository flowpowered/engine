// $shader_type: fragment

#version 330

in vec3 positionView;
in vec3 normalView;
in vec3 lightDirectionView;

layout(location = 0) out vec4 outputWeightedColor;
layout(location = 1) out float outputLayerCount;

uniform vec4 modelColor;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;
uniform float shininess;

void main() {
    vec3 forwardNormalView = normalize(faceforward(normalView, lightDirectionView, normalView));

    float ambientTerm = ambientIntensity;

    float diffuseTerm;
    float normalDotLight = max(0, dot(forwardNormalView, -lightDirectionView));
    diffuseTerm = normalDotLight;

    float specularTerm;
    if (normalDotLight > 0) {
        specularTerm = pow(max(0, dot(reflect(-lightDirectionView, forwardNormalView), normalize(positionView))), shininess * 100);
    } else {
        specularTerm = 0;
    }

    vec4 color = modelColor;
    color.rgb *= (diffuseTerm + specularTerm + ambientTerm) * color.a;

    outputWeightedColor = color;
    outputLayerCount = 1;
}
