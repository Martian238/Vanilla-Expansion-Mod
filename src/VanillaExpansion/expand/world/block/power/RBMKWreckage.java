package VanillaExpansion.expand.world.block.power;
import arc.Core;
import VanillaExpansion.expand.world.block.SixteenDirectionBlock;
import mindustry.world.meta.BuildVisibility;
public class RBMKWreckage extends SixteenDirectionBlock {

    public RBMKWreckage(String name) {
        super(name);
        size = 1;
        solid = false;
        destructible = true;
        update = false;
        sync = true;
        alwaysUnlocked = true;
        buildVisibility = BuildVisibility.hidden;
    }
    @Override
    public void load() {
        super.load();
        if (region == null || !region.found()) {
            region = Core.atlas.find(name.substring(name.indexOf('-') + 1), region);
        }
    }
}
