package VanillaExpansion.expand.graphics;

import mindustry.graphics.CacheLayer;
import mindustry.graphics.Shaders;

import javax.xml.catalog.CatalogException;

public class VECacheLayer extends CacheLayer {
    public static CacheLayer

            dysharmony, lava, acid;

    public static void init(){
        addLast(
                dysharmony = new ShaderLayer(VEShaders.dysharmony),
                lava = new ShaderLayer(VEShaders.lava),
                acid = new ShaderLayer(VEShaders.acid)
        );
    }
}
