// $shader_type: fragment

#version 120

varying vec4 positionClip;
varying vec4 previousPositionClip;
varying vec3 normalView;

uniform vec4 modelColor;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;
uniform float shininess;

void main() {
    gl_FragData[0] = modelColor;

    gl_FragData[1] = vec4((normalView + 1) / 2, 1);

    gl_FragData[2] = gl_FragData[1];

    gl_FragData[3] = vec4(diffuseIntensity, specularIntensity, ambientIntensity, shininess);

    gl_FragData[4] = vec4((positionClip.xy / positionClip.w - previousPositionClip.xy / previousPositionClip.w) * 0.5, 0, 1);
}
