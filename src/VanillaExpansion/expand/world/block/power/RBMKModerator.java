package VanillaExpansion.expand.world.block.power;
import arc.math.*;
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
            return true;
        }
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