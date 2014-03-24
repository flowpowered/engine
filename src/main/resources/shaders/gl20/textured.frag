// $shader_type: fragment

// $texture_layout: diffuse = 0
// $texture_layout: normals = 1
// $texture_layout: specular = 2

#version 120

varying vec4 positionClip;
varying vec4 previousPositionClip;
varying vec3 normalView;
varying vec2 textureUV;
varying mat3 tangentMatrix;

uniform sampler2D diffuse;
uniform sampler2D normals;
uniform sampler2D specular;
uniform float diffuseIntensity;
uniform float ambientIntensity;
uniform float shininess;

void main() {
    gl_FragData[0] = texture2D(diffuse, textureUV);

    vec3 textureNormalView = tangentMatrix * (texture2D(normals, textureUV).xyz * 2 - 1);
    gl_FragData[1] = vec4((textureNormalView + 1) / 2, 1);

    gl_FragData[2] = vec4((normalView + 1) / 2, 1);

    float specularIntensity = texture2D(specular, textureUV).r;
    gl_FragData[3] = vec4(diffuseIntensity, specularIntensity, ambientIntensity, shininess);

    gl_FragData[4] = vec4((positionClip.xy / positionClip.w - previousPositionClip.xy / previousPositionClip.w) * 0.5, 0, 1);
}
