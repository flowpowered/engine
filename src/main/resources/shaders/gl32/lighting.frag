// $shader_type: fragment

// $texture_layout: colors = 0
// $texture_layout: normals = 1
// $texture_layout: depths = 2
// $texture_layout: materials = 3
// $texture_layout: occlusions = 4
// $texture_layout: shadows = 5

#version 330

in vec2 textureUV;
noperspective in vec3 viewRay;
in vec3 lightDirectionView;

layout(location = 0) out vec4 outputColor;

uniform sampler2D colors;
uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2D materials;
uniform sampler2D occlusions;
uniform sampler2D shadows;

void main() {
    outputColor = texture(colors, textureUV);

    vec4 rawNormalView = texture(normals, textureUV);
    if (rawNormalView.a <= 0) {
        return;
    }

    float occlusion = texture(occlusions, textureUV).r;
    float shadow = texture(shadows, textureUV).r;
    vec4 material = texture(materials, textureUV);

    float ambientTerm = material.z * occlusion;
    float diffuseTerm = 0;
    float specularTerm = 0;

    if (shadow > 0) {
        vec3 normalView = normalize(rawNormalView.xyz * 2 - 1);
        float normalDotLight = max(0, dot(normalView, -lightDirectionView));

        diffuseTerm = material.x * shadow * normalDotLight;

        if (normalDotLight > 0) {
            specularTerm = material.y * shadow * pow(max(0, dot(reflect(-lightDirectionView, normalView), normalize(viewRay))), material.w * 100);
        }
    }

    outputColor.rgb *= (diffuseTerm + specularTerm + ambientTerm);
}
