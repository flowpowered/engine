// Edge detection anti aliasing
// Adapted from: http://http.developer.nvidia.com/GPUGems2/gpugems2_chapter09.html (Example 9-2)

// $shader_type: fragment

// $texture_layout: diffuse = 0
// $texture_layout: normals = 1
// $texture_layout: depths = 2

#version 330

const vec2 LT = vec2(-1, 1);
const vec2 RB = vec2(1, -1);
const vec2 RT = vec2(1, 1);
const vec2 LB = vec2(-1, -1);
const vec2 L = vec2(-1, 0);
const vec2 R = vec2(1, 0);
const vec2 T = vec2(0, 1);
const vec2 B = vec2(0, -1);

in vec2 textureUV;

layout(location = 0) out vec4 outputColor;

uniform sampler2D diffuse;
uniform sampler2D normals;
uniform sampler2D depths;
uniform vec2 projection;
uniform vec2 resolution;
uniform vec2 barriers; // x = normal, y = depth
uniform vec2 weights; // x = normal, y = depth
uniform float kernel; // 0 = no aa, 1 = full aa

float linearizeDepth(float depth) {
    return -projection.y / (depth - projection.x);
}

void main() {
    // fragment and its neighbours
    vec2 tc0 = textureUV;
    vec2 tc1 = textureUV + LT / resolution;
    vec2 tc2 = textureUV + RB / resolution;
    vec2 tc3 = textureUV + RT / resolution;
    vec2 tc4 = textureUV + LB / resolution;
    vec2 tc5 = textureUV + L / resolution;
    vec2 tc6 = textureUV + R / resolution;
    vec2 tc7 = textureUV + T / resolution;
    vec2 tc8 = textureUV + B / resolution;

    // normal discontinuity filter
    vec3 nc = texture(normals, tc0).xyz * 2 - 1;
    vec4 nd;
    nd.x = dot(nc, texture(normals, tc1).xyz * 2 - 1);
    nd.y = dot(nc, texture(normals, tc2).xyz * 2 - 1);
    nd.z = dot(nc, texture(normals, tc3).xyz * 2 - 1);
    nd.w = dot(nc, texture(normals, tc4).xyz * 2 - 1);
    nd -= barriers.x;
    nd = step(vec4(0, 0, 0, 0), nd);
    float ne = clamp(dot(nd, vec4(weights.x, weights.x, weights.x, weights.x)), 0, 1);

    // depth gradient difference filter
    float dc = linearizeDepth(texture(depths, tc0).x);
    vec4 dd;
    dd.x = linearizeDepth(texture(depths, tc1).x) + linearizeDepth(texture(depths, tc2).x);
    dd.y = linearizeDepth(texture(depths, tc3).x) + linearizeDepth(texture(depths, tc4).x);
    dd.z = linearizeDepth(texture(depths, tc5).x) + linearizeDepth(texture(depths, tc6).x);
    dd.w = linearizeDepth(texture(depths, tc7).x) + linearizeDepth(texture(depths, tc8).x);
    dd = abs(2 * dc - dd) - barriers.y;
    dd = step(dd, vec4(0, 0, 0, 0));
    float de = clamp(dot(dd, vec4(weights.y, weights.y, weights.y, weights.y)), 0, 1);

    // combined weight
    float w = (1 - de * ne) * kernel;

    // smoothed color
    vec2 offset = tc0 * (1 - w);
    vec4 s0 = texture(diffuse, offset + tc1 * w);
    vec4 s1 = texture(diffuse, offset + tc2 * w);
    vec4 s2 = texture(diffuse, offset + tc3 * w);
    vec4 s3 = texture(diffuse, offset + tc4 * w);
    outputColor = (s0 + s1 + s2 + s3) / 4;
}
