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

/**
 * 小行星带网格类 - 确保Z轴旋转
 */
public class AsteroidBeltMesh implements GenericMesh {
    private static Mat3D beltTransform = new Mat3D();
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

        // 设置着色器
        Shaders.clouds.bind();
        Shaders.clouds.setUniformMatrix4("u_proj", projection.val);

        // 创建小行星带的变换矩阵：
        // 1. 先应用行星的变换（位置和旋转）
        // 2. 然后绕世界Z轴旋转（确保是Z轴）
        beltTransform.set(transform);
        
        // 使用Z轴旋转
        beltTransform.rotate(Vec3.Z, Time.globalTime * rotateSpeed / 40f);

        // 设置变换矩阵
        Shaders.clouds.setUniformMatrix4("u_trans", beltTransform.val);
        Shaders.clouds.apply();
        
        // 使用MultiMesh的render方法
        mesh.render(params, projection, beltTransform);
    }

    @Override
    public void dispose() {
        mesh.dispose();
    }

    /**
     * 构建小行星带
     */
    private static MultiMesh buildAsteroidBelt(Planet planet, float innerRadius, float outerRadius, int asteroidCount, int seed, arc.graphics.Color color) {
        Seq<GenericMesh> meshes = new Seq<>();

        float beltRadius = (innerRadius + outerRadius) / 2f;
        float beltThickness = (outerRadius - innerRadius) / 2f;
        float beltHeight = 0.12f;
        int seedBase = seed;

        // WarHammer 参数
        float persistence = 0.58f;
        float noiseScale = 0.42f;
        float noiseMag = 18f;
        int noiseOctaves = 2;

        // 颜色噪声参数
        int colorOctaves = 3;
        float colorPersistence = 0.6f;
        float colorScale = 0.38f;
        float colorThreshold = 0.54f;

        arc.graphics.Color beltTint = color.cpy().a(0.38f);

        for (int i = 0; i < asteroidCount; i++) {
            int rockSeed = seedBase + i * 37;

            // 使用确定性随机
            float angle = i * (360f / asteroidCount) + Mathf.randomSeed(rockSeed + 1, -5f, 5f);
            float dist = beltRadius + Mathf.randomSeed(rockSeed + 2, -beltThickness, beltThickness);
            float rockRadius = Mathf.randomSeed(rockSeed + 3, 0.020f, 0.045f); 

            // 在XY平面上计算位置
            Vec3 pos = new Vec3(
                    Angles.trnsx(angle, dist),  // X坐标
                    Angles.trnsy(angle, dist),  // Y坐标（在XY平面上）
                    Mathf.randomSeed(rockSeed + 4, -beltHeight, beltHeight)  // Z坐标（高度）
            );

            // 添加随机倾斜
            pos.rotate(Vec3.X, Mathf.randomSeed(rockSeed + 5, -20f, 20f));
            pos.rotate(Vec3.Z, Mathf.randomSeed(rockSeed + 6, -20f, 20f));
            pos.rotate(Vec3.Y, Mathf.randomSeed(rockSeed + 7, -8f, 8f));

            // 使用官方 NoiseMesh
            GenericMesh asteroid = new NoiseMesh(
                    planet,
                    rockSeed,
                    1,                              // divisions
                    rockRadius,                      // radius
                    noiseOctaves,                    // octaves
                    persistence,                     // persistence
                    noiseScale,                      // scale
                    noiseMag,                        // mag
                    color,                           // color1
                    beltTint,                        // color2
                    colorOctaves,                    // coct
                    colorPersistence,                // cper
                    colorScale,                      // cscl
                    colorThreshold                   // cthresh
            );

            // 使用 MatMesh 应用位置变换
            meshes.add(new MatMesh(
                    asteroid,
                    new Mat3D().setToTranslation(pos.x, pos.y, pos.z)
            ));
        }

        return new MultiMesh(meshes.toArray(GenericMesh.class));
    }
}
