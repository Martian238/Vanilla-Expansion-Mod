package VanillaExpansion.expand.graphics;
import mindustry.graphics.CacheLayer;
public class VECacheLayer extends CacheLayer {
    public static CacheLayer

            dysharmony, lava, acid;

    public static void init(){
        add(
                dysharmony = new ShaderLayer(VEShaders.dysharmony),
                lava = new ShaderLayer(VEShaders.lava),
                acid = new ShaderLayer(VEShaders.acid)
        );
    }
}
