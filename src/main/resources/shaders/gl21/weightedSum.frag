// $shader_type: fragment

#version 120

varying vec3 positionView;
varying vec3 normalView;
varying vec3 lightDirectionView;

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

    gl_FragData[0] = color;
    gl_FragData[1] = vec4(1, 1, 1, 1);
}
