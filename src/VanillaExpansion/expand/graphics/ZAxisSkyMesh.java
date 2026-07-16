package VanillaExpansion.expand.graphics;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.type.*;

/**
 * 自定义天空网格类，添加Z轴旋转和潮汐锁定支持
 */
public class ZAxisSkyMesh extends PlanetMesh{
    static Mat3D mat = new Mat3D();

    public float speed = 0f;

    public ZAxisSkyMesh(Planet planet, int seed, float speed, float radius, int divisions, Color color, int octaves, float persistence, float scl, float thresh){
        super(planet, MeshBuilder.buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 1f;
            }

            @Override
            public void getColor(Vec3 position, Color out){
                out.set(color);
            }

            @Override
            public boolean skip(Vec3 position){
                return Simplex.noise3d(7 + seed, octaves, persistence, scl, position.x, position.y * 3f, position.z) >= thresh;
            }
        }, divisions, planet.radius, radius), Shaders.clouds);

        this.speed = speed;
    }

    public ZAxisSkyMesh(){
    }

    public float relRot(){
        return Time.globalTime * speed * 2f / 40f;
    }

    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        // 不要渲染0透明度的云层
        if(params.planet == planet && Mathf.zero(1f - params.uiAlpha, 0.01f)) return;

        preRender(params);
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        
        // 设置变换矩阵：平移到行星位置，应用行星旋转和云层旋转
        // 使用Z轴旋转（温度轴）
        shader.setUniformMatrix4("u_trans", mat.setToTranslation(planet.position).rotate(Vec3.Z, planet.getRotation() - relRot()).val);
        
        // 调用apply()应用着色器参数（参考原版实现）
        shader.apply();
        
        mesh.render(shader, Gl.triangles);
    }

    @Override
    public void preRender(PlanetParams params){
        // 设置行星引用
        Shaders.clouds.planet = planet;
        
        // 计算光照方向（从太阳到行星）
        // 使用Z轴旋转与云层保持一致
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Z, planet.getRotation() - relRot()).nor();
        
        // 设置环境光颜色
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        
        // 设置透明度（参考原版实现）
        Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
    }
}
