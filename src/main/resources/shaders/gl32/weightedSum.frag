// $shader_type: fragment

#version 330

in vec3 positionView;
in vec3 normalView;
in vec3 lightDirectionView;
in vec4 positionClip;
in vec4 previousPositionClip;

layout(location = 0) out vec4 outputWeightedColor;
layout(location = 1) out vec2 outputWeightedVelocity;
layout(location = 2) out float outputLayerCount;

uniform vec4 modelColor;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

void main() {
    vec3 forwardNormalView = faceforward(normalView, lightDirectionView, normalView);

    float ambientTerm = ambientIntensity;

    float diffuseTerm;
    float normalDotLight = max(0, dot(forwardNormalView, -lightDirectionView));
    diffuseTerm = normalDotLight;

    float specularTerm;
    if (normalDotLight > 0) {
        specularTerm = pow(max(0, dot(reflect(-lightDirectionView, forwardNormalView), normalize(positionView))), 20);
    } else {
        specularTerm = 0;
    }

    vec4 color = modelColor;
    color.rgb *= (diffuseTerm + specularTerm + ambientTerm) * color.a;

    vec2 velocity = (positionClip.xy / positionClip.w - previousPositionClip.xy / previousPositionClip.w) * 0.5 * color.a;

    outputWeightedColor = color;
    outputWeightedVelocity = velocity;
    outputLayerCount = 1;
}
