package VanillaExpansion.expand.world.block.power;
import arc.math.*;
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