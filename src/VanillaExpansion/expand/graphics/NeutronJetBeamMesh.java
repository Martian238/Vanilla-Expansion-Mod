package VanillaExpansion.expand.graphics;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.graphics.g3d.*;
import mindustry.type.Planet;
public class NeutronJetBeamMesh implements GenericMesh{
    public Planet planet;
    public float length;
    public float outerWidth;
    public float innerWidth;
    public int segments = 24;
    public Color color = Color.valueOf("88bbff");
    public float outerAlpha = 0.25f;

    public boolean drawSector = true;
    public float sectorTilt = 15f;
    public float sectorAngle = 5f;
    public float sectorLength = 3f;
    public int sectorRings = 16;
    public int sectorRadial = 32;
    public Color sectorColor = Color.valueOf("87CEEB");
    public float sectorAlpha = 0.25f;
    public float sectorGlow = 0.5f;
    public Color sectorGlowColor = Color.valueOf("cceeff");
    public float sectorBloom = 0.3f;
    public float sectorBloomScale = 2.5f;
    public Color sectorBloomColor = Color.valueOf("ffffff");
    private final Color tmpColor = new Color();
    private final Vec3 va = new Vec3();
    private final Vec3 vb = new Vec3();
    private final Vec3 vc = new Vec3();
    private final Vec3 vd = new Vec3();
    public NeutronJetBeamMesh(Planet planet, float length, float outerWidth, float innerWidth){
        this.planet = planet;
        this.length = length;
        this.outerWidth = outerWidth;
        this.innerWidth = innerWidth;
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
        float time = Time.time / 60f;
        float wobblePhase = time * 128f;
        drawBeam(b, tx, ty, tz, wobblePhase, 1f);
        if(drawSector){renderSector(b, tx, ty, tz);}
        b.flush(Gl.triangles);
        Gl.depthMask(true);
        Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
        Gl.enable(Gl.cullFace);
    }
    private void renderSector(VertexBatch3D b, float tx, float ty, float tz){
        float cx = Mathf.cosDeg(sectorTilt), sx = Mathf.sinDeg(sectorTilt);
        float cz = 1f, sz = 0f;
        float coneRad = sectorAngle * Mathf.degRad;
        float baseR = planet.radius;
        if(sectorBloom > 0f){
            renderSectorPass(b, tx, ty, tz, cx, sx, cz, sz, coneRad, baseR, sectorBloomScale, sectorBloomColor, sectorAlpha * sectorBloom);
        }
        renderSectorPass(b, tx, ty, tz, cx, sx, cz, sz, coneRad, baseR, 1f, sectorColor, sectorAlpha);
    }
    private void renderSectorPass(VertexBatch3D b, float tx, float ty, float tz, float cx, float sx, float cz, float sz, float coneRad, float baseR, float scale, Color baseColor, float baseAlpha){
        Color glowTmp = new Color();
        for(int dir = -1; dir <= 1; dir += 2){
            for(int i = 0; i < sectorRings; i++){
                float t0 = (float)i / sectorRings;
                float t1 = (float)(i + 1) / sectorRings;
                float fade = 1f - t1 * t1;
                float r0 = t0 * sectorLength;
                float r1 = t1 * sectorLength;
                float coneR0 = r0 * (float)Math.tan(coneRad) * scale;
                float coneR1 = r1 * (float)Math.tan(coneRad) * scale;
                float y0 = dir * (baseR + r0);
                float y1 = dir * (baseR + r1);
                for(int j = 0; j < sectorRadial; j++){
                    float a0 = (j / (float)sectorRadial) * Mathf.PI2;
                    float a1 = ((j + 1f) / sectorRadial) * Mathf.PI2;
                    float glowWeight = sectorGlow * fade;
                    if(scale > 1f){
                        glowTmp.set(sectorBloomColor);
                        glowTmp.a(baseAlpha * fade);
                    }else{
                        glowTmp.set(baseColor).lerp(sectorGlowColor, glowWeight);
                        glowTmp.a(baseAlpha * fade * (0.5f + 0.5f * sectorGlow));
                    }
                    tmpColor.set(glowTmp);
                    float c0 = Mathf.cos(a0), s0 = Mathf.sin(a0);
                    float c1 = Mathf.cos(a1), s1 = Mathf.sin(a1);
                    rotate(coneR0 * c0, y0, coneR0 * s0, va, cx, sx, cz, sz);
                    rotate(coneR0 * c1, y0, coneR0 * s1, vb, cx, sx, cz, sz);
                    rotate(coneR1 * c0, y1, coneR1 * s0, vc, cx, sx, cz, sz);
                    rotate(coneR1 * c1, y1, coneR1 * s1, vd, cx, sx, cz, sz);
                    b.tri(
                        tx + va.x, ty + va.y, tz + va.z,
                        tx + vb.x, ty + vb.y, tz + vb.z,
                        tx + vc.x, ty + vc.y, tz + vc.z,
                        tmpColor
                    );
                    b.tri(
                        tx + vc.x, ty + vc.y, tz + vc.z,
                        tx + vb.x, ty + vb.y, tz + vb.z,
                        tx + vd.x, ty + vd.y, tz + vd.z,
                        tmpColor
                    );
                }
            }
        }
    }
    private void drawBeam(VertexBatch3D b, float tx, float ty, float tz, float wobblePhase, float alphaMul){
        float tiltX = 15f + 5f * Mathf.cos(wobblePhase);
        float tiltZ = 5f * Mathf.sin(wobblePhase);
        float cx = Mathf.cosDeg(tiltX), sx = Mathf.sinDeg(tiltX);
        float cz = Mathf.cosDeg(tiltZ), sz = Mathf.sinDeg(tiltZ);
        float baseR = planet.radius;
        int half = segments / 2;
        for(int dir = -1; dir <= 1; dir += 2){
            for(int i = 0; i < half; i++){
                float t0 = (float)i / (half - 1);
                float t1 = (float)(i + 1) / (half - 1);
                float rf0 = 1f - t0 * 1.8f;
                float rf1 = 1f - t1 * 1.8f;
                if(rf0 < 0.05f) continue;
                float y0 = dir * (baseR + t0 * length);
                float y1 = dir * (baseR + t1 * length);
                float ow0 = outerWidth * rf0, ow1 = outerWidth * rf1;
                float iw0 = innerWidth * rf0, iw1 = innerWidth * rf1;
                drawBeamTri(b, tx, ty, tz, y0, y1, ow0, ow1, outerAlpha * rf0 * alphaMul, cx, sx, cz, sz);
                drawBeamTri(b, tx, ty, tz, y0, y1, iw0, iw1, rf0 * alphaMul, cx, sx, cz, sz);
            }
        }
    }
    private void drawBeamTri(VertexBatch3D b, float tx, float ty, float tz, float y0, float y1, float w0, float w1, float alpha, float cx, float sx, float cz, float sz){
        float hw0 = w0 / 2f, hw1 = w1 / 2f;
        tmpColor.set(color);
        tmpColor.a(alpha);
        drawLocalTri(b, tx, ty, tz, -hw0, y0, 0, hw0, y0, 0, hw1, y1, 0, cx, sx, cz, sz);
        drawLocalTri(b, tx, ty, tz, -hw0, y0, 0, hw1, y1, 0, -hw1, y1, 0, cx, sx, cz, sz);
        drawLocalTri(b, tx, ty, tz, 0, y0, -hw0, 0, y0, hw0, 0, y1, hw1, cx, sx, cz, sz);
        drawLocalTri(b, tx, ty, tz, 0, y0, -hw0, 0, y1, hw1, 0, y1, -hw1, cx, sx, cz, sz);
    }
    private void drawLocalTri(VertexBatch3D b, float tx, float ty, float tz, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float cx, float sx, float cz, float sz){
        rotate(x0, y0, z0, va, cx, sx, cz, sz);
        rotate(x1, y1, z1, vb, cx, sx, cz, sz);
        rotate(x2, y2, z2, vc, cx, sx, cz, sz);
        b.tri(
            tx + va.x, ty + va.y, tz + va.z,
            tx + vb.x, ty + vb.y, tz + vb.z,
            tx + vc.x, ty + vc.y, tz + vc.z,
            tmpColor
        );
    }
    private void rotate(float x, float y, float z, Vec3 out, float cx, float sx, float cz, float sz){
        float rx = x * cz - y * sz;
        float ry = x * sz + y * cz;
        out.x = rx;
        out.y = ry * cx - z * sx;
        out.z = ry * sx + z * cx;
    }
    @Override
    public void dispose(){}
}
