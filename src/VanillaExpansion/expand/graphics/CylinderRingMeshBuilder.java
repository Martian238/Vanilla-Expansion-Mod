package VanillaExpansion.expand.graphics;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.Mesh;
import arc.graphics.VertexAttribute;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.struct.Seq;

import java.nio.FloatBuffer;
public class CylinderRingMeshBuilder {
    private static final boolean packNormals =
            Core.gl30 != null && (Core.app.isMobile() || Core.graphics.getGLVersion().atLeast(3, 3));
    public static Mesh build(float outerRadius, float innerRadius, float height, int segments, Color color, Color color2){
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
            float col = (i % 8 == 0 || i % 8 == 4) ? color2.toFloatBits() : color.toFloatBits();
            float a1 = (float)i / segments * Mathf.PI2;
            float a2 = (float)(i + 1) / segments * Mathf.PI2;
            Vec3 outer1 = new Vec3(Mathf.cos(a1) * outerRadius, half, Mathf.sin(a1) * outerRadius);
            Vec3 outer2 = new Vec3(Mathf.cos(a2) * outerRadius, half, Mathf.sin(a2) * outerRadius);
            Vec3 outer3 = new Vec3(Mathf.cos(a2) * outerRadius, -half, Mathf.sin(a2) * outerRadius);
            Vec3 outer4 = new Vec3(Mathf.cos(a1) * outerRadius, -half, Mathf.sin(a1) * outerRadius);
            Vec3 inner1 = new Vec3(Mathf.cos(a1) * innerRadius, half, Mathf.sin(a1) * innerRadius);
            Vec3 inner2 = new Vec3(Mathf.cos(a2) * innerRadius, half, Mathf.sin(a2) * innerRadius);
            Vec3 inner3 = new Vec3(Mathf.cos(a2) * innerRadius, -half, Mathf.sin(a2) * innerRadius);
            Vec3 inner4 = new Vec3(Mathf.cos(a1) * innerRadius, -half, Mathf.sin(a1) * innerRadius);
            Vec3 normalUp = new Vec3(0, 1, 0);
            Vec3 normalDown = new Vec3(0, -1, 0);
            Vec3 normalOuter = new Vec3(outer1.x, 0, outer1.z).nor();
            Vec3 normalInner = new Vec3(inner1.x, 0, inner1.z).nor();
            vert(buf, floats, outer1, normalUp, col);
            vert(buf, floats, inner1, normalUp, col);
            vert(buf, floats, inner2, normalUp, col);
            vert(buf, floats, outer1, normalUp, col);
            vert(buf, floats, inner2, normalUp, col);
            vert(buf, floats, outer2, normalUp, col);
            vert(buf, floats, outer4, normalDown, col);
            vert(buf, floats, outer3, normalDown, col);
            vert(buf, floats, inner3, normalDown, col);
            vert(buf, floats, outer4, normalDown, col);
            vert(buf, floats, inner3, normalDown, col);
            vert(buf, floats, inner4, normalDown, col);
            vert(buf, floats, outer1, normalOuter, col);
            vert(buf, floats, outer2, normalOuter, col);
            vert(buf, floats, outer3, normalOuter, col);
            vert(buf, floats, outer1, normalOuter, col);
            vert(buf, floats, outer3, normalOuter, col);
            vert(buf, floats, outer4, normalOuter, col);
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
    public static Mesh build(float radius, float height, int segments, Color color, Color color2){
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
