package VanillaExpansion.expand.world.block.power;

import arc.math.*;

/**
 * RBMK 反射器 —— 参考 HBM's Nuclear Tech 的 RBMKReflector / TileEntityRBMKReflector。
 * <p>
 * 无源反射柱：本身不产生/吸收能量，只在中子流传播时把逃逸中子弹回源燃料柱。
 * 对应 HBM，{@link RBMKRod.RBMKRodBuild#spreadFlux} 遇到反射柱时：
 * <ul>
 * <li>{@link RBMKDials#reflectorEfficiency} 为 1（完全反射）：把剩余通量整体回授给
 *     源燃料柱（{@code receiveFlux}），模拟中子被反射回堆芯。</li>
 * <li>效率非 1（部分反射）：剩余通量乘以效率后衰减，并继续向后穿透。</li>
 * </ul>
 * <p>
 * 熔毁时散落空白碎片（对应 HBM onMelt：1 + rand(2) 块 BLANK）。
 */
public class RBMKReflector extends RBMKBase {

    public RBMKReflector(String name) {
        super(name);
        consoleType = ColumnType.REFLECTOR;
        buildType = RBMKReflectorBuild::new;
    }

    public class RBMKReflectorBuild extends RBMKBaseBuild {

        @Override
        public RBMKType getRBMKType() {
            return RBMKType.REFLECTOR;
        }

        @Override
        public ColumnType getConsoleType() {
            return ColumnType.REFLECTOR;
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