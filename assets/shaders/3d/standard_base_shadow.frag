
varying vec2 v_texCoords;
varying vec4 v_color;
varying vec3 v_fragPos;
varying vec3 v_normal;

uniform sampler2D u_texture;
//uniform samplerCube u_shadowCube[LIGHTS];
//$samplpers$

uniform float u_shadowRadius;
uniform float u_shadowBias;

uniform vec3 u_cameraPos;

uniform vec3 u_lightDir;
uniform vec4 u_lightColor;
uniform vec4 u_ambientColor;

struct LightSource {
    vec3 position;
    vec4 color;
    float radius;
    float attenuation;
};

uniform LightSource u_light[LIGHTS];
uniform int u_activeLights;

float shadow(LightSource light, samplerCube shadowCube){
    vec3 fragToLight = v_fragPos - light.position;
    float viewDistance = length(u_cameraPos - v_fragPos);
    float currentDepth = length(fragToLight);
    float shadow = 0.0;

    float diskRadius = (1.0 + (viewDistance/u_shadowRadius)) * 0.03;

    vec4 closestDepth = textureCube(shadowCube, fragToLight + vec3(0,           0, 0))
                       + textureCube(shadowCube, fragToLight + vec3(-diskRadius, 0, diskRadius))
                       + textureCube(shadowCube, fragToLight + vec3(-diskRadius, 0, -diskRadius))
                       + textureCube(shadowCube, fragToLight + vec3(diskRadius,  0, -diskRadius))
                       + textureCube(shadowCube, fragToLight + vec3(diskRadius,  0, diskRadius))
                       ;

    float depth = closestDepth.r * u_shadowRadius * 0.2;

    return step(depth, currentDepth - u_shadowBias);
}

vec3 calculateDirLighting(vec3 objectColor){
    vec3 lightDir = normalize(-u_lightDir);
    vec3 cameraDir = normalize(u_cameraPos - v_fragPos);

    float diff = dot(lightDir, v_normal);
    vec3 diffuse = diff * objectColor * u_lightColor.rgb;

    vec3 halfwayDir = normalize(lightDir + cameraDir);
    float spec = pow(max(dot(v_normal, halfwayDir), 0.0), 32.0);

    vec3 specular = spec * u_lightColor.rgb;

    return (diffuse + spec)*u_lightColor.a;
}

vec3 calculateLighting(LightSource light, vec3 objectColor) {
    vec3 lightDir = normalize(light.position - v_fragPos);
    vec3 normal = normalize(v_normal)*(float(gl_FrontFacing) - 0.5)*2.0;

    float dst = length(light.position - v_fragPos) + length(u_cameraPos - v_fragPos);
    float inten = light.color.a*pow(clamp(1.0 - dst/light.radius, 0.0, 1.0), light.attenuation);

    float diff = dot(lightDir, normal);
    vec3 diffuse = objectColor * light.color.rgb * diff;

    vec3 cameraDir = normalize(u_cameraPos - v_fragPos);
    vec3 halfwayDir = normalize(lightDir + cameraDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), 32.0);

    vec3 specular = light.color.rgb * spec;

    return inten*(diffuse + specular);
}

void main() {
    vec4 color = texture2D(u_texture, v_texCoords) * v_color;
    vec3 lightedColor = u_ambientColor.rgb * u_ambientColor.a;

    lightedColor += calculateDirLighting(color.rgb);

    //for (int i = 0; i < u_activeLights; i++) {
    //  if (i < u_activeLights){
    //    LightSource l = u_light[i];
    //    float shadow = 1.0 - shadow(l, u_shadowCube[i]);
    //    lightedColor += shadow * calculateLighting(l, color.rgb);
    //  }
    //}
    //$calculateShadowLight$

    gl_FragColor = vec4(lightedColor, color.a);
}
