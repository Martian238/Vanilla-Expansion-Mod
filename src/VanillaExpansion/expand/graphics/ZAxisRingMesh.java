package VanillaExpansion.expand.graphics;

import arc.graphics.Color;
import arc.graphics.Gl;
import arc.math.Mathf;
import arc.math.geom.Mat3D;
import arc.math.geom.Vec3;
import arc.util.Time;
import mindustry.graphics.Shaders;
import mindustry.graphics.g3d.PlanetMesh;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.Planet;

/**
 * Z轴旋转的星环网格类
 * 扁平的圆盘状星环，围绕行星赤道旋转
 */
public class ZAxisRingMesh extends PlanetMesh {
    // 使用非静态矩阵，避免与其他类冲突
    private final Mat3D mat = new Mat3D();

    public float rotateSpeed = 1.0f;

    /**
     * 旧构造函数 - 兼容之前的调用方式
     */
    public ZAxisRingMesh(Planet planet, float radius, float height, int seed, Color color, Color color2) {
        super(planet, CylinderRingMeshBuilder.build(radius, radius * 0.65f, height, 120, color, color2), Shaders.clouds);
    }

    /**
     * 新构造函数 - 支持自定义内外环半径
     * @param planet 行星
     * @param outerRadius 外环半径
     * @param innerRadius 内环半径
     * @param height 环厚度
     * @param segments 分段数
     * @param seed 随机种子（不再使用）
     * @param color 主颜色
     * @param color2 次颜色
     */
    public ZAxisRingMesh(Planet planet, float outerRadius, float innerRadius, float height, int segments, int seed, Color color, Color color2) {
        super(planet, CylinderRingMeshBuilder.build(outerRadius, innerRadius, height, segments, color, color2), Shaders.clouds);
    }

    public ZAxisRingMesh(){}

    public float relRot(){
        // 只返回时间相关的旋转，不叠加行星旋转
        return Time.globalTime * rotateSpeed / 40f;
    }

    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        if(params.planet == planet && Mathf.zero(1f - params.uiAlpha, 0.01f)) return;

        preRender(params);
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        //只围绕Z轴（温度轴）旋转，不叠加行星自身旋转
        shader.setUniformMatrix4("u_trans", mat.idt()
                .translate(planet.position.x, planet.position.y, planet.position.z)
                .rotate(Vec3.Z, relRot()).val);
        shader.apply();
        mesh.render(shader, Gl.triangles);
    }

    @Override
    public void preRender(PlanetParams params){
        Shaders.clouds.planet = planet;
        // 光照方向也只考虑星环自身的Z轴旋转
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position)
                .rotate(Vec3.Z, relRot())
                .nor();
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
    }
}
