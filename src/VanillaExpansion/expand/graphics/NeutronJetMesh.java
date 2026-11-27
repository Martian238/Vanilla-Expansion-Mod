package VanillaExpansion.expand.graphics;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;
public class NeutronJetMesh implements GenericMesh{
    private static final Mat3D tmpMat = new Mat3D();
    private MultiMesh mesh;
    private Planet planet;
    public float rotateSpeed = 9f;
    public NeutronJetMesh(Planet planet, float length, float radius, int particles, float twist, float scatter){
        this.planet = planet;
        this.mesh = buildJet(planet, length, radius, particles, twist, scatter);
    }
    public NeutronJetMesh(Planet planet){
        this(planet, 12f, 0.4f, 240, 14f, 0.4f);
    }
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        if(params.planet == planet && Mathf.zero(1f - params.uiAlpha, 0.01f)) return;
        Shaders.clouds.bind();
        Shaders.clouds.setUniformMatrix4("u_proj", projection.val);
        tmpMat.set(transform);
        tmpMat.rotate(Vec3.Y, Time.globalTime * rotateSpeed);
        Shaders.clouds.planet = planet;
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
        Shaders.clouds.setUniformMatrix4("u_trans", tmpMat.val);
        Shaders.clouds.apply();
        mesh.render(params, projection, tmpMat);
    }
    @Override
    public void dispose(){
        mesh.dispose();
    }
    private static MultiMesh buildJet(Planet planet, float length, float radius, int particles, float twist, float scatter){
        Seq<GenericMesh> seq = new Seq<>();
        Color c1 = Color.valueOf("aaddff"), c2 = Color.valueOf("3355aa"), c3 = Color.valueOf("ddeeff");
        int core = particles / 3;
        float baseR = planet.radius;
        float persistence = 1f, noiseScale = 0.2f, noiseMag = 0.15f;
        int noiseOctaves = 2;
        int colorOctaves = 2;
        float colorPersistence = 0.6f;
        float colorScale = 0.2f;
        float colorThreshold = 0.5f;
        int half = particles / 2;
        for(int i = 0; i < particles; i++){
            boolean isCore = i < core;
            boolean up = i < half;
            int idx = up ? i : i - half;
            int total = up ? half : particles - half;
            float t = total <= 1 ? 0f : idx / (float)(total - 1);
            float sign = up ? 1f : -1f;
            float h = sign * (baseR + t * length);
            float hFac = 1f - t * 0.5f;
            float a, r;
            if(isCore){
                a = Mathf.randomSeed(i * 7 + 1, 0f, 360f);
                r = radius * 0.2f * Mathf.randomSeed(i * 11 + 3, 0.3f, 1f) * hFac;
            }else{
                a = i * twist + Mathf.randomSeed(i * 7 + 1, -scatter * 60f, scatter * 60f);
                r = radius * hFac * (1f + Mathf.randomSeed(i * 11 + 3, -scatter, scatter));
            }
            float px = Angles.trnsx(a, r);
            float py = h;
            float pz = Angles.trnsy(a, r);
            Vec3 pos = new Vec3(px, py, pz);
            Color col = isCore ? c3.cpy().lerp(c1, Mathf.randomSeed(i * 13 + 5, 0f, 0.5f)) :
                c1.cpy().lerp(c2, Mathf.randomSeed(i * 13 + 5, 0f, 0.7f));
            Color beltTint = col.cpy().a(0.4f);
            float pr = radius * (isCore ? Mathf.randomSeed(i * 17 + 7, 0.06f, 0.15f) :
                Mathf.randomSeed(i * 19 + 9, 0.1f, 0.25f));
            GenericMesh particle = new NoiseMesh(
                planet, i * 37 + 1, 1,
                pr,
                noiseOctaves, persistence, noiseScale, noiseMag,
                col, beltTint,
                colorOctaves, colorPersistence, colorScale, colorThreshold
            );
            seq.add(new MatMesh(particle, new Mat3D().setToTranslation(pos.x, pos.y, pos.z)));
        }
        return new MultiMesh(seq.toArray(GenericMesh.class));
    }
}
