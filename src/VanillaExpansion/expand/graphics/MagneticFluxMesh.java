package VanillaExpansion.expand.graphics;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.graphics.g3d.*;
import mindustry.type.Planet;
public class MagneticFluxMesh implements GenericMesh{
    public Planet planet;
    public int rings = 16;
    public int ringSegments = 48;
    public float minRadius = 2.2f;
    public float maxRadius = 4.0f;
    public Color color1 = Color.valueOf("aa66ff");
    public Color color2 = Color.valueOf("ddaaff");
    public MagneticFluxMesh(Planet planet){
        this.planet = planet;
    }
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        VertexBatch3D b = Vars.renderer.planets.batch;
        float tx = transform.val[12], ty = transform.val[13], tz = transform.val[14];
        Gl.enable(Gl.depthTest);
        Gl.depthFunc(Gl.lequal);
        Gl.disable(Gl.cullFace);
        Gl.enable(Gl.blend);
        Gl.blendFunc(Gl.srcAlpha, Gl.one);
        Gl.depthMask(false);
        b.proj(projection);
        float time = Time.time / 60f;
        Color c = new Color();
        for(int r = 0; r < rings; r++){
            float tiltAngle = (float)r / rings * 180f - 90f;
            float rad = minRadius + (maxRadius - minRadius) * (1f - Math.abs(Mathf.cosDeg(tiltAngle * 2f)));
            if(rad < minRadius) rad = minRadius;
            c.set(color1).lerp(color2, (float)r / rings);
            c.a = 0.4f + 0.2f * Mathf.sin(time + r);
            for(int i = 0; i < ringSegments; i++){
                float a1 = (float)i / ringSegments * 360f;
                float a2 = (float)(i + 1) / ringSegments * 360f;
                float lx1 = rad * Mathf.cosDeg(a1);
                float lz1 = rad * Mathf.sinDeg(a1);
                float lx2 = rad * Mathf.cosDeg(a2);
                float lz2 = rad * Mathf.sinDeg(a2);
                float tiltRad = tiltAngle * Mathf.degRad;
                float cosT = Mathf.cos(tiltRad), sinT = Mathf.sin(tiltRad);
                float tx1 = lx1, ty1 = lx1 * sinT, tz1 = lz1 * cosT;
                float tx2 = lx2, ty2 = lx2 * sinT, tz2 = lz2 * cosT;
                float hs = 0.015f;
                b.tri(
                    tx + tx1 - hs, ty + ty1, tz + tz1,
                    tx + tx1 + hs, ty + ty1, tz + tz1,
                    tx + tx2, ty + ty2, tz + tz2,
                    c
                );
            }
        }
        b.flush(Gl.triangles);
        Gl.depthMask(true);
        Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
        Gl.enable(Gl.cullFace);
        Gl.enable(Gl.depthTest);
    }
    @Override
    public void dispose(){}
}
