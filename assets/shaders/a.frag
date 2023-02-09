uniform sampler2D u_texture;
uniform float dispersion;

uniform float maxThreshold;
uniform float minThreshold;

varying vec4 v_color;
varying vec2 v_texCoords;

void main(){
    float x = (v_texCoords.x - 0.5)*2.0;
    float y = (v_texCoords.y - 0.5)*4.0;

    vec4 c = texture2D(u_texture, v_texCoords);
    vec4 mixed = v_color*c;

    float gradMod = %gradMod%;
    float alpha = dispersion*gradMod/(abs(%fx%) + dispersion*gradMod);

    alpha = max(min((alpha - minThreshold)/(maxThreshold - minThreshold), 1.0), 0.0)*mixed.a;

    gl_FragColor = vec4(mixed.r, mixed.g, mixed.b, alpha);
}