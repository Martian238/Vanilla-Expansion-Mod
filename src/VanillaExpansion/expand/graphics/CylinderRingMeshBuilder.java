package VanillaExpansion.expand.graphics;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Mesh;
import arc.graphics.VertexAttribute;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.struct.Seq;

import java.nio.FloatBuffer;

/**
 * 构建扁平圆盘状的星环网格
 */
public class CylinderRingMeshBuilder {
    private static final boolean packNormals =
            Core.gl30 != null && (Core.app.isMobile() || Core.graphics.getGLVersion().atLeast(3, 3));

    /**
     * 构建土星环网格
     * @param outerRadius 外环半径
     * @param innerRadius 内环半径
     * @param height 环的厚度（非常小，形成扁平效果）
     * @param segments 圆周分段数
     * @param color 主颜色
     * @param color2 次颜色（用于条纹效果）
     */
    public static Mesh build(float outerRadius, float innerRadius, float height, int segments, Color color, Color color2){
        // 每个分段生成: 上表面(2三角=6顶点) + 下表面(2三角=6顶点) + 外环边缘(2三角=6顶点) + 内环边缘(2三角=6顶点) = 24个顶点
        int vertices = segments * 24;

        Seq<VertexAttribute> attributes = Seq.with(
                VertexAttribute.position3,
                packNormals ? VertexAttribute.packedNormal : VertexAttribute.normal,
                VertexAttribute.color
        );

        Mesh mesh = new Mesh(true, vertices, 0, attributes.toArray(VertexAttribute.class));
        mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().capacity());
        mesh.getVerticesBuffer().position(0);

        FloatBuffer buf = mesh.getVerticesBuffer();
        buf.clear();

        int stride = packNormals ? 5 : 7;
        float[] floats = new float[stride];

        float half = height / 2f;

        for(int i = 0; i < segments; i++){
            // 每隔几个分段改变颜色，形成条纹效果
            float col = (i % 8 == 0 || i % 8 == 4) ? color2.toFloatBits() : color.toFloatBits();

            float a1 = (float)i / segments * Mathf.PI2;
            float a2 = (float)(i + 1) / segments * Mathf.PI2;

            // 外环顶点
            Vec3 outer1 = new Vec3(Mathf.cos(a1) * outerRadius, half, Mathf.sin(a1) * outerRadius);
            Vec3 outer2 = new Vec3(Mathf.cos(a2) * outerRadius, half, Mathf.sin(a2) * outerRadius);
            Vec3 outer3 = new Vec3(Mathf.cos(a2) * outerRadius, -half, Mathf.sin(a2) * outerRadius);
            Vec3 outer4 = new Vec3(Mathf.cos(a1) * outerRadius, -half, Mathf.sin(a1) * outerRadius);

            // 内环顶点
            Vec3 inner1 = new Vec3(Mathf.cos(a1) * innerRadius, half, Mathf.sin(a1) * innerRadius);
            Vec3 inner2 = new Vec3(Mathf.cos(a2) * innerRadius, half, Mathf.sin(a2) * innerRadius);
            Vec3 inner3 = new Vec3(Mathf.cos(a2) * innerRadius, -half, Mathf.sin(a2) * innerRadius);
            Vec3 inner4 = new Vec3(Mathf.cos(a1) * innerRadius, -half, Mathf.sin(a1) * innerRadius);

            // 上表面法向量
            Vec3 normalUp = new Vec3(0, 1, 0);
            // 下表面法向量
            Vec3 normalDown = new Vec3(0, -1, 0);
            // 外环法向量
            Vec3 normalOuter = new Vec3(outer1.x, 0, outer1.z).nor();
            // 内环法向量
            Vec3 normalInner = new Vec3(inner1.x, 0, inner1.z).nor();

            // 上表面（逆时针）
            vert(buf, floats, outer1, normalUp, col);
            vert(buf, floats, inner1, normalUp, col);
            vert(buf, floats, inner2, normalUp, col);

            vert(buf, floats, outer1, normalUp, col);
            vert(buf, floats, inner2, normalUp, col);
            vert(buf, floats, outer2, normalUp, col);

            // 下表面（顺时针，确保背面渲染正确）
            vert(buf, floats, outer4, normalDown, col);
            vert(buf, floats, outer3, normalDown, col);
            vert(buf, floats, inner3, normalDown, col);

            vert(buf, floats, outer4, normalDown, col);
            vert(buf, floats, inner3, normalDown, col);
            vert(buf, floats, inner4, normalDown, col);

            // 外环边缘
            vert(buf, floats, outer1, normalOuter, col);
            vert(buf, floats, outer2, normalOuter, col);
            vert(buf, floats, outer3, normalOuter, col);

            vert(buf, floats, outer1, normalOuter, col);
            vert(buf, floats, outer3, normalOuter, col);
            vert(buf, floats, outer4, normalOuter, col);

            // 内环边缘
            vert(buf, floats, inner2, normalInner, col);
            vert(buf, floats, inner1, normalInner, col);
            vert(buf, floats, inner4, normalInner, col);

            vert(buf, floats, inner2, normalInner, col);
            vert(buf, floats, inner4, normalInner, col);
            vert(buf, floats, inner3, normalInner, col);
        }

        mesh.getVerticesBuffer().limit(mesh.getVerticesBuffer().position());
        return mesh;
    }

    /**
     * 兼容旧版本的build方法
     */
    public static Mesh build(float radius, float height, int segments, Color color, Color color2){
        // 使用半径的85%作为内环半径，形成一个较宽的环
        return build(radius, radius * 0.65f, height, segments, color, color2);
    }

    private static void vert(FloatBuffer buf, float[] floats, Vec3 p, Vec3 normal, float color){
        floats[0] = p.x;
        floats[1] = p.y;
        floats[2] = p.z;

        if(packNormals){
            floats[3] = packNormal(normal.x, normal.y, normal.z);
            floats[4] = color;
        }else{
            floats[3] = normal.x;
            floats[4] = normal.y;
            floats[5] = normal.z;
            floats[6] = color;
        }

        buf.put(floats);
    }

    private static float packNormal(float x, float y, float z){
        int xs = x < -1f/512f ? 1 : 0;
        int ys = y < -1f/512f ? 1 : 0;
        int zs = z < -1f/512f ? 1 : 0;

        int vi =
                zs << 29 | ((int)(z * 511 + (zs << 9)) & 511) << 20 |
                        ys << 19 | ((int)(y * 511 + (ys << 9)) & 511) << 10 |
                        xs << 9  | ((int)(x * 511 + (xs << 9)) & 511);

        return Float.intBitsToFloat(vi);
    }
}
