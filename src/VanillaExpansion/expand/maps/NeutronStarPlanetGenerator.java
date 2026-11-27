package VanillaExpansion.expand.maps;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.noise.*;
import mindustry.game.*;
import mindustry.maps.generators.*;
import mindustry.type.*;
public class NeutronStarPlanetGenerator extends BlankPlanetGenerator{
    public float scl = 1.5f;
    public float emissiveMul = 1f;
    public Color color1 = Color.valueOf("88bbff");
    public Color color2 = Color.valueOf("1a2a6e");
    public Color emissiveColor1 = Color.valueOf("ccf0ff");
    public Color emissiveColor2 = Color.valueOf("4477cc");
    private final Color tmp = new Color();

    @Override
    public float getHeight(Vec3 position){
        return 0.02f * Simplex.noise3d(seed, 3, 0.5f, 1f / 2f, position.x, position.y, position.z);
    }
    @Override
    public void getColor(Vec3 position, Color out){
        float n = noise(position);
        float n2 = Ridged.noise3d(seed + 2, position.x, position.y, position.z, 2, 12f) * 0.5f + 0.5f;
        float blend = Mathf.clamp(n + n2 * 0.3f);
        out.set(color1).lerp(color2, 1f - blend);
        out.a = 1f;
    }
    @Override
    public void getEmissiveColor(Vec3 position, Color out){
        float n = noise(position);
        float n2 = Ridged.noise3d(seed + 2, position.x, position.y, position.z, 2, 12f) * 0.5f + 0.5f;
        float n3 = Mathf.pow(Simplex.noise3d(seed + 4, 3, 0.6f, 1f / 0.6f, position.x * 3f, position.y * 3f, position.z * 3f), 2f);
        float brightness = Mathf.clamp(n * 0.9f + n2 * 0.2f + n3 * 0.6f) * emissiveMul;
        tmp.set(emissiveColor1).lerp(emissiveColor2, 1f - brightness);
        out.set(tmp.r * brightness, tmp.g * brightness, tmp.b * brightness, brightness);
    }
    @Override
    public boolean isEmissive(){
        return true;
    }
    @Override
    protected void generate(){}
    @Override
    public void addWeather(Sector sector, Rules rules){}
    private float noise(Vec3 position){
        float v = Simplex.noise3d(seed, 4, 0.5f, 1f / scl, position.x * scl, position.y * scl, position.z * scl);
        return Mathf.clamp(v * 1.2f);
    }
}
