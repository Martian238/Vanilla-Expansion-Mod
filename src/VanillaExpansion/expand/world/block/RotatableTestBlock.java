package VanillaExpansion.expand.world.block;

import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.meta.*;

/**
 * 可手动旋转的测试方块
 * 继承自RotatableBlock，支持玩家通过UI调整方向
 */
public class RotatableTestBlock extends RotatableBlock {

    public RotatableTestBlock(String name) {
        super(name);
        rotate = true;
        autoRotate = true;
        showConfigUI = true;
        solid = true;
        destructible = true;
        health = 200;
    }

    public class RotatableTestBuild extends RotatableBuild {
    }
}