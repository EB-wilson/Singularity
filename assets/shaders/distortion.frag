
uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform float strength;

varying vec2 v_texCoords;

void main() {
    vec4 col = texture(u_texture0, v_texCoords);

    if(col.a > 0) {
        float lerp = col.b * col.a;
        float rx = (col.r - 0.5) * 2;
        float gy = (col.g - 0.5) * 2;

        vec2 off = vec2(rx * lerp, gy * lerp);

        vec2 len = strength / textureSize(u_texture1, 0);

        gl_FragColor = vec4(texture(u_texture1, v_texCoords + vec2(off.r * len.x, off.g * len.y)).rgb, 1);
    }
    else{
        gl_FragColor = vec4(0);
    }
}
