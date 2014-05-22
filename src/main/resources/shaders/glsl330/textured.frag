// $shader_type: fragment

// $texture_layout: diffuse = 0
// $texture_layout: normals = 1
// $texture_layout: specular = 2

#version 330

in vec4 positionClip;
in vec4 previousPositionClip;
in vec3 normalView;
in vec2 textureUV;
in mat3 tangentMatrix;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec4 outputNormal;
layout(location = 2) out vec4 outputVertexNormal;
layout(location = 3) out vec4 outputMaterial;
layout(location = 4) out vec2 outputVelocity;

uniform sampler2D diffuse;
uniform sampler2D normals;
uniform sampler2D specular;
uniform float diffuseIntensity;
uniform float ambientIntensity;
uniform float shininess;

void main() {
    outputColor = texture(diffuse, textureUV);

    vec3 textureNormalView = tangentMatrix * (texture(normals, textureUV).xyz * 2 - 1);
    outputNormal = vec4((textureNormalView + 1) / 2, 1);

    outputVertexNormal = vec4((normalView + 1) / 2, 1);

    float specularIntensity = texture(specular, textureUV).r;
    outputMaterial = vec4(diffuseIntensity, specularIntensity, ambientIntensity, shininess);

    outputVelocity = (positionClip.xy / positionClip.w - previousPositionClip.xy / previousPositionClip.w) * 0.5;
}
