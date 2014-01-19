// $shader_type: fragment

// $texture_layout: colors = 0
// $texture_layout: normals = 1
// $texture_layout: depths = 2
// $texture_layout: materials = 3
// $texture_layout: occlusions = 4
// $texture_layout: shadows = 5

#version 120

varying vec2 textureUV;
varying vec3 viewRay;
varying vec3 lightDirectionView;

uniform sampler2D colors;
uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2D materials;
uniform sampler2D occlusions;
uniform sampler2D shadows;
uniform vec2 projection;
uniform float lightAttenuation;
uniform	float spotCutoff;

void main() {
    gl_FragColor = texture2D(colors, textureUV);

    vec4 rawNormalView = texture2D(normals, textureUV);
    if (rawNormalView.a <= 0) {
        return;
    }

    float occlusion = texture2D(occlusions, textureUV).r;
    float shadow = texture2D(shadows, textureUV).r;
    vec4 material = texture2D(materials, textureUV);

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

    gl_FragColor.rgb *= (diffuseTerm + specularTerm + ambientTerm);
}
