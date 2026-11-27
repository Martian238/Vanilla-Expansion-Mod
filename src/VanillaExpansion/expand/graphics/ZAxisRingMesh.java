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
public class ZAxisRingMesh extends PlanetMesh {
    private final Mat3D mat = new Mat3D();

    public float rotateSpeed = 1.0f;
    public ZAxisRingMesh(Planet planet, float radius, float height, int seed, Color color, Color color2) {
        super(planet, CylinderRingMeshBuilder.build(radius, radius * 0.65f, height, 120, color, color2), Shaders.clouds);
    }
    public ZAxisRingMesh(Planet planet, float outerRadius, float innerRadius, float height, int segments, int seed, Color color, Color color2) {
        super(planet, CylinderRingMeshBuilder.build(outerRadius, innerRadius, height, segments, color, color2), Shaders.clouds);
    }
    public ZAxisRingMesh(){}
    public float relRot(){
        return Time.globalTime * rotateSpeed / 40f;
    }
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        if(params.planet == planet && Mathf.zero(1f - params.uiAlpha, 0.01f)) return;
        preRender(params);
        shader.bind();
        shader.setUniformMatrix4("u_proj", projection.val);
        shader.setUniformMatrix4("u_trans", mat.idt()
                .translate(planet.position.x, planet.position.y, planet.position.z)
                .rotate(Vec3.Z, relRot()).val);
        shader.apply();
        mesh.render(shader, Gl.triangles);
    }
    @Override
    public void preRender(PlanetParams params){
        Shaders.clouds.planet = planet;
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position)
                .rotate(Vec3.Z, relRot())
                .nor();
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        Shaders.clouds.alpha = params.planet == planet ? 1f - params.uiAlpha : 1f;
    }
}
