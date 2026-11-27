package VanillaExpansion.expand.graphics;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.graphics.g3d.*;
public class RotatingMesh implements GenericMesh{
    public GenericMesh mesh;
    public float speed;
    public float tiltDeg = 15f;
    public float wobbleAmp = 5f;
    public float wobbleSpeed = 128f;
    private Vec3 axis = new Vec3();
    public RotatingMesh(GenericMesh mesh, float speed){
        this.mesh = mesh;
        this.speed = speed;
    }
    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        float t = Time.time / 60f;
        float wobble = t * wobbleSpeed;
        float tiltX = tiltDeg + wobbleAmp * Mathf.cos(wobble);
        float tiltZ = wobbleAmp * Mathf.sin(wobble);
        float cosTx = Mathf.cosDeg(tiltX), sinTx = Mathf.sinDeg(tiltX);
        float cosTz = Mathf.cosDeg(tiltZ), sinTz = Mathf.sinDeg(tiltZ);
        axis.set(-cosTx * sinTz, cosTx * cosTz, sinTx).nor();
        transform.rotate(axis, t * speed);
        mesh.render(params, projection, transform);
    }
    @Override
    public void dispose(){
        mesh.dispose();
    }
}
