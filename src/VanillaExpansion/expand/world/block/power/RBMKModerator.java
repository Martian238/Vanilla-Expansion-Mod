package VanillaExpansion.expand.world.block.power;

import arc.math.*;

/**
 * RBMK 慢化剂 —— 参考 HBM's Nuclear Tech 的 RBMKModerator / TileEntityRBMKModerator。
 * <p>
 * 无源慢化柱：本身不产生/吸收能量，只在中子流穿越时把快中子变为慢中子。
 * 对应 HBM，中子流在 {@link RBMKRod.RBMKRodBuild#spreadFlux} 中按
 * {@link RBMKDials#moderatorEfficiency} 逐格衰减其快中子比例
 * （{@code fluxRatio *= 1 - moderatorEfficiency}），从而让下游 SLOW / ANY 型燃料更增益裂变。
 * <p>
 * 熔毁时散落石墨碎片（对应 HBM onMelt：2 + rand(2) 块 GRAPHITE）。
 */
public class RBMKModerator extends RBMKBase {

    public RBMKModerator(String name) {
        super(name);
        consoleType = ColumnType.MODERATOR;
        buildType = RBMKModeratorBuild::new;
    }

    public class RBMKModeratorBuild extends RBMKBaseBuild {

        @Override
        public RBMKType getRBMKType() {
            return RBMKType.MODERATOR;
        }

        @Override
        public ColumnType getConsoleType() {
            return ColumnType.MODERATOR;
        }

        @Override
        public boolean isModerated() {
            // 慢化柱自身即慢化体（控制台据此把它标记为 moderated 列）
            return true;
        }

        // ---------- 熔毁（对应 HBM onMelt：2 + rand(2) 块 GRAPHITE 碎片） ----------

        @Override
        public void onMelt(int reduce) {
            int mCount = 2 + Mathf.random(2);
            for (int i = 0; i < mCount; i++) {
                spawnDebris(DebrisType.GRAPHITE);
            }
            super.onMelt(reduce);
        }
    }
}