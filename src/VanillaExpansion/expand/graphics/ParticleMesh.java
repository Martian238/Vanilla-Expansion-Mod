package VanillaExpansion.expand.graphics;
import arc.graphics.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.graphics.g3d.*;
import mindustry.type.Planet;
public abstract class ParticleMesh implements GenericMesh{
    protected Particle[] particles;
    protected int particleCount;
    protected Planet planet;
    protected float time;
    protected float tiltDeg;
    protected float wobbleAmp = 5f;
    protected float wobbleSpeed = 45f;
    public static float activeTiltX, activeTiltZ;
    protected static class Particle{
        public float x, y, z;
        public float size;
        public float life, maxLife;
        public Color color = new Color();
    }
    public ParticleMesh(Planet planet, int maxParticles){
        this.planet = planet;
        this.particleCount = maxParticles;
        this.particles = new Particle[maxParticles];
        for(int i = 0; i < maxParticles; i++){
            particles[i] = new Particle();
        }
    }
    public abstract void updateParticles(float delta);
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        updateParticles(Time.delta / 60f);
        Vec3 cpos = Vars.renderer.planets.cam.position;
        VertexBatch3D b = Vars.renderer.planets.batch;
        float tx = transform.val[12], ty = transform.val[13], tz = transform.val[14];
        Gl.enable(Gl.depthTest);
        Gl.depthFunc(Gl.lequal);
        Gl.disable(Gl.cullFace);
        Gl.enable(Gl.blend);
        Gl.blendFunc(Gl.srcAlpha, Gl.one);
        Gl.depthMask(false);
        b.proj(projection);
        float wobble = time * wobbleSpeed;
        float tiltX = tiltDeg + wobbleAmp * Mathf.cos(wobble);
        float tiltZ = wobbleAmp * Mathf.sin(wobble);
        activeTiltX = tiltX;
        activeTiltZ = tiltZ;
        float cosTx = Mathf.cosDeg(tiltX), sinTx = Mathf.sinDeg(tiltX);
        float cosTz = Mathf.cosDeg(tiltZ), sinTz = Mathf.sinDeg(tiltZ);
        for(int i = 0; i < particleCount; i++){
            Particle p = particles[i];
            float ly = p.y * cosTx - p.z * sinTx;
            float lz = p.y * sinTx + p.z * cosTx;
            float lx = p.x * cosTz - ly * sinTz;
            ly = p.x * sinTz + ly * cosTz;
            float wx = tx + lx, wy = ty + ly, wz = tz + lz;
            Vec3 dir = new Vec3(cpos.x - wx, cpos.y - wy, cpos.z - wz).nor();
            Vec3 rv = new Vec3(dir).crs(Vec3.Y).nor();
            if(rv.isZero()) rv.set(1, 0, 0);
            Vec3 uv = new Vec3(rv).crs(dir).nor();
            float hs = p.size * 0.5f;
            b.tri(
                wx - rv.x*hs - uv.x*hs, wy - rv.y*hs - uv.y*hs, wz - rv.z*hs - uv.z*hs,
                wx + rv.x*hs - uv.x*hs, wy + rv.y*hs - uv.y*hs, wz + rv.z*hs - uv.z*hs,
                wx + rv.x*hs + uv.x*hs, wy + rv.y*hs + uv.y*hs, wz + rv.z*hs + uv.z*hs,
                p.color
            );
            b.tri(
                wx - rv.x*hs - uv.x*hs, wy - rv.y*hs - uv.y*hs, wz - rv.z*hs - uv.z*hs,
                wx + rv.x*hs + uv.x*hs, wy + rv.y*hs + uv.y*hs, wz + rv.z*hs + uv.z*hs,
                wx - rv.x*hs + uv.x*hs, wy - rv.y*hs + uv.y*hs, wz - rv.z*hs + uv.z*hs,
                p.color
            );
        }
        b.flush(Gl.triangles);
        Gl.depthMask(true);
        Gl.blendFunc(Gl.srcAlpha, Gl.oneMinusSrcAlpha);
        Gl.enable(Gl.cullFace);
        Gl.enable(Gl.depthTest);
    }
    @Override
    public void dispose(){
    }
}
