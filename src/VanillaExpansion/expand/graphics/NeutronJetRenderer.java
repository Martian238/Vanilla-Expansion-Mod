package VanillaExpansion.expand.graphics;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
public class NeutronJetRenderer{
    private static Shader shader;
    private static boolean loaded = false;

    public float length = 400f;
    public float baseWidth = 24f;
    public float rotationSpeed = 10.5f;
    public float scatterAmount = 0.35f;
    public float twist = 2.5f;
    public float viewTilt = 50f;
    public float brightness = 24f;
    public int layers = 24;
    public int particlesPerLayer = 12;
    public float particleSize = 6f;
    public Color color1 = Color.valueOf("ffffff");
    public Color color2 = Color.valueOf("ff8844");
    private final Vec3 v = new Vec3();
    private final float[] verts = new float[24];
    public static void load(){
        if(loaded) return;
        shader = new Shader(VEShaders.defaultVert, VEShaders.file("neutronjet.frag").readString());
        loaded = true;
    }
    public void draw(float cx, float cy){
        if(shader == null) return;
        float time = Time.time / 60f;
        float rotAngle = time * rotationSpeed;
        Draw.shader(shader);
        shader.bind();
        shader.setUniformf("u_time", time);
        shader.setUniformf("u_color1", color1.r, color1.g, color1.b, color1.a);
        shader.setUniformf("u_color2", color2.r, color2.g, color2.b, color2.a);
        shader.setUniformf("u_brightness", brightness);
        float colorBits = Draw.getColor().toFloatBits();
        float mcolorBits = Color.clearFloatBits;
        Texture texture = Core.atlas.white().texture;
        for(int i = 0; i < layers; i++){
            float h = (i / (float)(layers - 1)) - 0.5f;
            float heightPos = h * length;
            float radiusFactor = 1f - Math.abs(h) * 1.8f;
            if(radiusFactor < 0.05f) continue;
            float layerR = baseWidth * radiusFactor;
            for(int j = 0; j < particlesPerLayer; j++){
                float pAngle = (j / (float)particlesPerLayer) * Mathf.PI2;
                float totalAngle = rotAngle + pAngle + h * twist * Mathf.PI;
                float rOff = layerR * (1f + scatterAmount * Mathf.sin(time * 5f + j * 2.1f + h * 8f));
                float x3d = rOff * Mathf.cos(totalAngle);
                float z3d = rOff * Mathf.sin(totalAngle);
                float y3d = heightPos;
                v.set(x3d, y3d, z3d).rotate(Vec3.X, viewTilt);
                if(v.z >= 690f) continue;
                float sz = 700f / (700f - v.z);
                float px = v.x * sz + cx;
                float py = v.y * sz + cy;
                float baseZ = Draw.z();
                Draw.z(baseZ + v.z * 0.0005f);
                float size = particleSize * (0.4f + 0.6f * radiusFactor) * (1f + 0.3f * Mathf.sin(time * 8f + j * 3.7f + h * 11f));
                float hs = size / 2f;
                float u1 = 0f, v1 = 1f, u2 = 1f, v2 = 0f;
                verts[0] = px - hs;
                verts[1] = py - hs;
                verts[2] = colorBits;
                verts[3] = u1;
                verts[4] = v1;
                verts[5] = mcolorBits;
                verts[6] = px + hs;
                verts[7] = py - hs;
                verts[8] = colorBits;
                verts[9] = u2;
                verts[10] = v1;
                verts[11] = mcolorBits;
                verts[12] = px + hs;
                verts[13] = py + hs;
                verts[14] = colorBits;
                verts[15] = u2;
                verts[16] = v2;
                verts[17] = mcolorBits;
                verts[18] = px - hs;
                verts[19] = py + hs;
                verts[20] = colorBits;
                verts[21] = u1;
                verts[22] = v2;
                verts[23] = mcolorBits;
                Draw.vert(texture, verts, 0, 24);
                Draw.z(baseZ);
            }
        }
        Draw.shader();
    }
    public void draw(float cx, float cy, float len, float width, float speed){
        length = len;
        baseWidth = width;
        rotationSpeed = speed;
        draw(cx, cy);
    }
}
