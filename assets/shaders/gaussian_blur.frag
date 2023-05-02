
uniform lowp sampler2D u_texture0;
uniform lowp sampler2D u_texture1;
varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying vec2 v_texCoords3;
varying vec2 v_texCoords4;
const float center = 0.2270270270;
const float close = 0.3162162162;
const float far = 0.0702702703;

void main(){
    vec4 blur = texture2D(u_texture0, v_texCoords2);
    vec3 color = texture2D(u_texture1, v_texCoords2).rgb;

    if(blur.a > 0.01) {
        vec3 blurColor =
        far * texture2D(u_texture1, v_texCoords0).rgb
        + close * texture2D(u_texture1, v_texCoords1).rgb
        + center * texture2D(u_texture1, v_texCoords2).rgb
        + close * texture2D(u_texture1, v_texCoords3).rgb
        + far * texture2D(u_texture1, v_texCoords4).rgb;

        gl_FragColor.rgb = mix(color, blurColor, blur.a);
    }
    else gl_FragColor.rgb = color;
}	