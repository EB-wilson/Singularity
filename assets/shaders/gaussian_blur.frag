
uniform lowp sampler2D u_texture0;
uniform lowp sampler2D u_texture1;

uniform mat4 convolution;
uniform float conv_len;
uniform vec2 dir;
uniform vec2 size;

varying vec2 v_texCoords;

void main(){
    vec2 len = dir/size;

    vec4 blur = texture2D(u_texture0, v_texCoords);
    vec3 color = texture2D(u_texture1, v_texCoords).rgb;

    if(blur.a > 0.01) {
        vec3 blurColor = vec3(0);

        float offset = -(conv_len - 1.0)/2.0;
        float up = (conv_len - 1.0)/2.0;
        for (int y = 0; y < 4; y++) {
           for (int x = 0; x < 4; x++){
               blurColor += convolution[y][x]*texture2D(u_texture1, v_texCoords + len*offset).rgb;
               offset += 1.0;

               if(offset > up) break;
           }
        }

        gl_FragColor.rgb = mix(color, blurColor, blur.a);
    }
    else{
        gl_FragColor.rgb = color;
    }

    gl_FragColor.a = 1.0;
}