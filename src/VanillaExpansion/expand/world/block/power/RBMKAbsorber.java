package VanillaExpansion.expand.world.block.power;

import arc.math.*;

/**
 * RBMK 吸收器 —— 参考 HBM's Nuclear Tech 的 RBMKAbsorber / TileEntityRBMKAbsorber。
 * <p>
 * 无源吸收柱：本身不产生能量，只在中子流穿越时吸收中子并把它们转化为自身柱体热。
 * 对应 HBM，{@link RBMKRod.RBMKRodBuild#spreadFlux} 遇到吸收柱时：
 * <ul>
 * <li>把射入通量按 {@link RBMKDials#absorberHeatConversion}（默认 0.05°C/通量）累加到本柱热。</li>
 * <li>{@link RBMKDials#absorberEfficiency} 为 1（完全吸收）：吸收该条流并停止传播。</li>
 * <li>效率非 1（部分吸收）：吸收 (1 - 效率) 部分，剩余通量乘以效率后继续向后穿透。</li>
 * </ul>
 * 是唯一"既衰减通量又产生热量"的被动组件，充当中子吸收兼散热负担。
 * <p>
 * 熔毁时散落空白碎片（对应 HBM onMelt：1 + rand(2) 块 BLANK）。
 */
public class RBMKAbsorber extends RBMKBase {

    public RBMKAbsorber(String name) {
        super(name);
        consoleType = ColumnType.ABSORBER;
        buildType = RBMKAbsorberBuild::new;
    }

    public class RBMKAbsorberBuild extends RBMKBaseBuild {

        @Override
        public RBMKType getRBMKType() {
            return RBMKType.ABSORBER;
        }

        @Override
        public ColumnType getConsoleType() {
            return ColumnType.ABSORBER;
        }

        // ---------- 熔毁（对应 HBM onMelt：1 + rand(2) 块 BLANK 碎片） ----------

        @Override
        public void onMelt(int reduce) {
            int count = 1 + Mathf.random(2);
            for (int i = 0; i < count; i++) {
                spawnDebris(DebrisType.BLANK);
            }
            super.onMelt(reduce);
        }
    }
}