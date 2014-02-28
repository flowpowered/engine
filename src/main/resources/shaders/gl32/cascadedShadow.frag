// $shader_type: fragment

// $texture_layout: normals = 0
// $texture_layout: depths = 1
// $texture_layout: lightDepths = 2
// $texture_layout: lightDepths2 = 3
// $texture_layout: lightDepths3 = 4
// $texture_layout: noise = 5

#version 330

const int MAX_KERNEL_SIZE = 32;

in vec2 textureUV;
noperspective in vec3 viewRay;
in vec3 lightDirectionView;

layout(location = 0) out float outputShadow;

uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2DShadow lightDepths;
uniform sampler2DShadow lightDepths2;
uniform sampler2DShadow lightDepths3;
uniform sampler2D noise;
uniform mat4 inverseViewMatrix;
uniform mat4 lightViewMatrix;
uniform mat4 lightViewMatrix2;
uniform mat4 lightViewMatrix3;
uniform mat4 lightProjectionMatrix;
uniform mat4 lightProjectionMatrix2;
uniform mat4 lightProjectionMatrix3;
uniform vec2 slices;
uniform vec2 projection;
uniform int kernelSize;
uniform vec2[MAX_KERNEL_SIZE] kernel;
uniform vec2 noiseScale;
uniform float bias;
uniform float radius;

float linearizeDepth(float depth) {
    return projection.y / (depth - projection.x);
}

float sampleCascade(float fragDepth, vec3 position, float bias) {
    if (fragDepth < slices.x) {
        return texture(lightDepths, vec3(position.xy, position.z - bias));
    } else if (fragDepth < slices.y) {
        return texture(lightDepths2, vec3(position.xy, position.z - bias));
    } else {
        return texture(lightDepths3, vec3(position.xy, position.z - bias));
    }
}

void main() {
    vec4 rawNormalView = texture(normals, textureUV);
    if (rawNormalView.a <= 0) {
        return;
    }
    vec3 normalView = normalize(rawNormalView.xyz * 2 - 1);

    vec3 positionView = viewRay * linearizeDepth(texture(depths, textureUV).r);

    float normalDotLight = dot(normalView, -lightDirectionView);
    float slopedBias = clamp(tan(acos(normalDotLight)) * bias, bias / 2, bias * 2);

    vec2 noiseVector = texture(noise, textureUV * noiseScale).xy * 2 - 1;
    vec2 orthogonalVector = vec2(noiseVector.y, -noiseVector.x);
    mat2 basis = mat2(noiseVector, orthogonalVector);

    mat4 sliceLightViewMatrix;
    mat4 sliceLightProjectionMatrix;
    float fragDepth = -positionView.z;
    if (fragDepth < slices.x) {
        sliceLightViewMatrix = lightViewMatrix;
        sliceLightProjectionMatrix = lightProjectionMatrix;
    } else if (fragDepth < slices.y) {
        sliceLightViewMatrix = lightViewMatrix2;
        sliceLightProjectionMatrix = lightProjectionMatrix2;
    } else {
        sliceLightViewMatrix = lightViewMatrix3;
        sliceLightProjectionMatrix = lightProjectionMatrix3;
    }

    mat4 sliceLightProjectionViewMatrix = sliceLightProjectionMatrix * sliceLightViewMatrix;
    vec4 positionWorld = inverseViewMatrix * vec4(positionView, 1);

    for (int i = 0; i < kernelSize; i++) {
        vec4 offsetPositionWorld = vec4(positionWorld);
        offsetPositionWorld.xz += basis * kernel[i] * radius;

        vec4 positionLightClip = sliceLightProjectionViewMatrix * offsetPositionWorld;
        positionLightClip.xyz = positionLightClip.xyz / positionLightClip.w * 0.5 + 0.5;

        outputShadow += sampleCascade(fragDepth, positionLightClip.xyz, slopedBias);
    }

    outputShadow /= kernelSize;
}
