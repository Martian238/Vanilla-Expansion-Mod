package VanillaExpansion.expand.graphics;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.graphics.Shaders;
import mindustry.graphics.g3d.*;
import mindustry.type.Planet;
public class AsteroidBeltMesh implements GenericMesh {
    private static final Mat3D beltTransform = new Mat3D();
    private MultiMesh mesh;
    private Planet planet;
    public float rotateSpeed = 1.0f;
    public AsteroidBeltMesh(Planet planet, float innerRadius, float outerRadius, int asteroidCount, int seed, arc.graphics.Color color) {
        this.planet = planet;
        this.mesh = buildAsteroidBelt(planet, innerRadius, outerRadius, asteroidCount, seed, color);
    }
    public AsteroidBeltMesh() {}
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform) {
        if (params.planet == planet && Mathf.zero(1f - params.uiAlpha, 0.01f)) return;
        Shaders.clouds.bind();
        Shaders.clouds.setUniformMatrix4("u_proj", projection.val);
        beltTransform.set(transform);
        beltTransform.rotate(Vec3.Z, Time.globalTime * rotateSpeed / 40f);
        Shaders.clouds.planet = planet;
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
        Shaders.clouds.setUniformMatrix4("u_trans", beltTransform.val);
        Shaders.clouds.apply();
        mesh.render(params, projection, beltTransform);
    }
    @Override
    public void dispose() {
        mesh.dispose();
    }
    private static MultiMesh buildAsteroidBelt(Planet planet, float innerRadius, float outerRadius, int asteroidCount, int seed, arc.graphics.Color color) {
        Seq<GenericMesh> meshes = new Seq<>();
        float beltRadius = (innerRadius + outerRadius) / 2f;
        float beltThickness = (outerRadius - innerRadius) / 2f;
        float beltHeight = 0.12f;
        int seedBase = seed;
        float persistence = 0.58f;
        float noiseScale = 0.42f;
        float noiseMag = 18f;
        int noiseOctaves = 2;
        int colorOctaves = 3;
        float colorPersistence = 0.6f;
        float colorScale = 0.38f;
        float colorThreshold = 0.54f;
        arc.graphics.Color beltTint = color.cpy().a(0.38f);
        for (int i = 0; i < asteroidCount; i++) {
            int rockSeed = seedBase + i * 37;
            float angle = i * (360f / asteroidCount) + Mathf.randomSeed(rockSeed + 1, -5f, 5f);
            float dist = beltRadius + Mathf.randomSeed(rockSeed + 2, -beltThickness, beltThickness);
            float rockRadius = Mathf.randomSeed(rockSeed + 3, 0.020f, 0.045f);
            Vec3 pos = new Vec3(
                    Angles.trnsx(angle, dist),
                    Angles.trnsy(angle, dist),
                    Mathf.randomSeed(rockSeed + 4, -beltHeight, beltHeight)
            );
            pos.rotate(Vec3.X, Mathf.randomSeed(rockSeed + 5, -20f, 20f));
            pos.rotate(Vec3.Z, Mathf.randomSeed(rockSeed + 6, -20f, 20f));
            pos.rotate(Vec3.Y, Mathf.randomSeed(rockSeed + 7, -8f, 8f));
            GenericMesh asteroid = new NoiseMesh(
                    planet,
                    rockSeed,
                    1,
                    rockRadius,
                    noiseOctaves,
                    persistence,
                    noiseScale,
                    noiseMag,
                    color,
                    beltTint,
                    colorOctaves,
                    colorPersistence,
                    colorScale,
                    colorThreshold
            );
            meshes.add(new MatMesh(
                    asteroid,
                    new Mat3D().setToTranslation(pos.x, pos.y, pos.z)
            ));
        }
        return new MultiMesh(meshes.toArray(GenericMesh.class));
    }
}
