package VanillaExpansion.expand.world.block.power;

import arc.Core;
import VanillaExpansion.expand.world.block.SixteenDirectionBlock;
import mindustry.world.meta.BuildVisibility;

/**
 * RBMK 熔毁残骸方块 —— 复用 16 向基类 {@link SixteenDirectionBlock}。
 * 熔毁爆炸后以 60% 概率被留在爆炸中心，放置时随机 16 向朝向（22.5° 步进）。
 * 仅在熔毁事件中程序化生成，不进入建造栏。
 */
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

    /** 手动加载贴图（兼容带/不带 mod 前缀的命名） */
    @Override
    public void load() {
        super.load();
        if (region == null || !region.found()) {
            region = Core.atlas.find(name.substring(name.indexOf('-') + 1), region);
        }
    }
}
