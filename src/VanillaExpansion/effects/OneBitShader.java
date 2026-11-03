package VanillaExpansion.effects;

import arc.graphics.gl.Shader;

public class OneBitShader extends Shader {
    public OneBitShader() {
        super(
                // 顶点着色器：标准全屏四边形
                """
                attribute vec4 a_position;
                attribute vec2 a_texCoord0;
                varying vec2 v_texCoords;
                void main() {
                    v_texCoords = a_texCoord0;
                    gl_Position = a_position;
                }
                """,
                // 片段着色器：1-bit 二值化
                """
                varying vec2 v_texCoords;
                uniform sampler2D u_texture;
                uniform float u_threshold;
                void main() {
                    vec4 color = texture2D(u_texture, v_texCoords);
                    float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                    float bit = step(u_threshold, lum);
                    gl_FragColor = vec4(vec3(bit), 1.0);
                }
                """
        );
    }

    public void apply(float threshold) {
        this.bind();
        this.setUniformf("u_threshold", threshold);
    }
}
