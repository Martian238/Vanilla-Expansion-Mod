package VanillaExpansion.expand.graphics;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.Vars;
import mindustry.graphics.g3d.*;
import mindustry.type.Planet;
public class NeutronJetSpinBeam implements GenericMesh{
    public Planet planet;
    public float length;
    public float outerWidth;
    public float innerWidth;
    public int segments = 32;
    public Color color = Color.valueOf("ddeeff");
    public float outerAlpha = 0.4f;
    public float opacity = 0.6f;
    private static final int LAYERS = 3;
    private final Color[] layerColors = new Color[LAYERS];
    public float[] layerWidths = {2.0f, 1.0f, 0.7f};
    public float headLength = 4f;
    public float headWidthMul = 3f;
    public float tipLength = 20f;
    public float fadeStart = 10f;
    private final Color tmpColor = new Color();
    private final Vec3 va = new Vec3(), vb = new Vec3(), vc = new Vec3();
    public NeutronJetSpinBeam(Planet planet, float length, float outerWidth, float innerWidth){
        this.planet = planet;
        this.length = length;
        this.outerWidth = outerWidth;
        this.innerWidth = innerWidth;
        layerColors[0] = color.cpy().a(outerAlpha);
        layerColors[1] = color.cpy();
        layerColors[2] = Color.white.cpy();
    }
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        VertexBatch3D b = Vars.renderer.planets.batch;
        float tx = transform.val[12], ty = transform.val[13], tz = transform.val[14];
        Gl.enable(Gl.depthTest);
        Gl.depthFunc(Gl.lequal);
        Gl.disable(Gl.cullFace);
        Gl.enable(Gl.blend);
        Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
        Gl.depthMask(false);
        b.proj(projection);
        float pulse = Mathf.absin(8f, 0.1f) + 1f;
        float cx = Mathf.cosDeg(ParticleMesh.activeTiltX), sx = Mathf.sinDeg(ParticleMesh.activeTiltX);
        float cz = Mathf.cosDeg(ParticleMesh.activeTiltZ), sz = Mathf.sinDeg(ParticleMesh.activeTiltZ);
        for(int i = 0; i < LAYERS; i++){
            float bw = outerWidth * layerWidths[i] * pulse;
            tmpColor.set(layerColors[i]).a(layerColors[i].a * opacity);
            drawLayer(b, tx, ty, tz, cx, sx, cz, sz, bw);
        }
        b.flush(Gl.triangles);
        Gl.depthMask(true);
        Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
        Gl.enable(Gl.cullFace);
        Gl.enable(Gl.depthTest);
    }
    private void drawLayer(VertexBatch3D b, float tx, float ty, float tz, float cx, float sx, float cz, float sz, float bw){
        float baseA = tmpColor.a;
        float baseR = planet.radius;
        float hw = bw * headWidthMul;
        int half = segments / 2;
        for(int dir = -1; dir <= 1; dir += 2){
            for(int i = 0; i < half; i++){
                float t0 = (float)i / half;
                float t1 = (float)(i + 1) / half;
                float y0 = dir * (baseR + t0 * headLength);
                float y1 = dir * (baseR + t1 * headLength);
                float w0 = Mathf.lerp(hw, bw, t0);
                float w1 = Mathf.lerp(hw, bw, t1);
                float hw0 = w0 / 2f, hw1 = w1 / 2f;
                float farD = t1 * headLength;
                float am = farD <= fadeStart ? 1f : 1f - Mathf.clamp((farD - fadeStart) / (length - fadeStart));
                tmpColor.a(baseA * am);
                drawTri(b, tx, ty, tz, -hw0, y0, 0, hw0, y0, 0, hw1, y1, 0, cx, sx, cz, sz);
                drawTri(b, tx, ty, tz, -hw0, y0, 0, hw1, y1, 0, -hw1, y1, 0, cx, sx, cz, sz);
                drawTri(b, tx, ty, tz, 0, y0, -hw0, 0, y0, hw0, 0, y1, hw1, cx, sx, cz, sz);
                drawTri(b, tx, ty, tz, 0, y0, -hw0, 0, y1, hw1, 0, y1, -hw1, cx, sx, cz, sz);
            }
            if(headLength < length){
                float tailLen = length - headLength;
                for(int i = 0; i < half; i++){
                    float t0 = (float)i / half;
                    float t1 = (float)(i + 1) / half;
                    float y0 = dir * (baseR + headLength + t0 * tailLen);
                    float y1 = dir * (baseR + headLength + t1 * tailLen);
                    float hbw = bw / 2f;
                    float farD = headLength + t1 * tailLen;
                    float am = farD <= fadeStart ? 1f : 1f - Mathf.clamp((farD - fadeStart) / (length - fadeStart));
                    tmpColor.a(baseA * am);
                    drawTri(b, tx, ty, tz, -hbw, y0, 0, hbw, y0, 0, hbw, y1, 0, cx, sx, cz, sz);
                    drawTri(b, tx, ty, tz, -hbw, y0, 0, hbw, y1, 0, -hbw, y1, 0, cx, sx, cz, sz);
                    drawTri(b, tx, ty, tz, 0, y0, -hbw, 0, y0, hbw, 0, y1, hbw, cx, sx, cz, sz);
                    drawTri(b, tx, ty, tz, 0, y0, -hbw, 0, y1, hbw, 0, y1, -hbw, cx, sx, cz, sz);
                }
            }
            float yT = dir * (baseR + length);
            float tbw = bw / 2f;
            float bd = (length - tipLength + length + length) / 3f;
            float bam = bd <= fadeStart ? 1f : 1f - Mathf.clamp((bd - fadeStart) / (length - fadeStart));
            tmpColor.a(baseA * bam);
            drawTri(b, tx, ty, tz, 0, yT - dir * tipLength, 0, -tbw, yT, 0, 0, yT, tbw, cx, sx, cz, sz);
            drawTri(b, tx, ty, tz, 0, yT - dir * tipLength, 0, 0, yT, tbw, tbw, yT, 0, cx, sx, cz, sz);
            drawTri(b, tx, ty, tz, 0, yT - dir * tipLength, 0, tbw, yT, 0, 0, yT, -tbw, cx, sx, cz, sz);

        }
    }
    private void drawTri(VertexBatch3D b, float tx, float ty, float tz, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float cx, float sx, float cz, float sz){
        vec3(x0, y0, z0, va, cx, sx, cz, sz);
        vec3(x1, y1, z1, vb, cx, sx, cz, sz);
        vec3(x2, y2, z2, vc, cx, sx, cz, sz);
        b.tri(tx + va.x, ty + va.y, tz + va.z, tx + vb.x, ty + vb.y, tz + vb.z, tx + vc.x, ty + vc.y, tz + vc.z, tmpColor);
    }
    private void vec3(float x, float y, float z, Vec3 out, float cx, float sx, float cz, float sz){
        float rx = x * cz - y * sz;
        float ry = x * sz + y * cz;
        out.x = rx;
        out.y = ry * cx - z * sx;
        out.z = ry * sx + z * cx;
    }
    @Override
    public void dispose(){
    }
}
