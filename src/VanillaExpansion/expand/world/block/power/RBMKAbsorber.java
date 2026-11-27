package VanillaExpansion.expand.world.block.power;
import arc.math.*;
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