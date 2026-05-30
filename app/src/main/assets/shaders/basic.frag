#version 300 es
precision mediump float;

in vec3 vNormal;
in vec3 vFragPos;
in vec2 vTexCoord;

uniform vec3 uLightDir;
uniform vec3 uLightColor;
uniform vec3 uObjectColor;
uniform vec3 uAmbientColor;

out vec4 fragColor;

void main() {
    vec3 norm = normalize(vNormal);
    vec3 lightDir = normalize(-uLightDir);

    float diff = max(dot(norm, lightDir), 0.0);
    vec3 ambient = uAmbientColor * uObjectColor;
    vec3 diffuse = diff * uLightColor * uObjectColor;

    fragColor = vec4(ambient + diffuse, 1.0);
}
