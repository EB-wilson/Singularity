
uniform sampler2D u_texture;
uniform float strength;

varying vec2 v_texCoords;

void main() {
    gl_FragColor = vec4(texture(u_texture, v_texCoords).rgb, 1);
}
