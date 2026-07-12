package MultiCraft;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.meta.BlockStatus;

public class IncineratingSorter extends Sorter {
    public float powerUse = 1f;

    public IncineratingSorter(String name) {
        this(name, false);
    }

    public IncineratingSorter(String name, boolean invert) {
        super(name);
        this.invert = invert;
        hasPower = true;
        consumesPower = true;
        consumePower(powerUse);
        buildType = IncineratingSorterBuild::new;
    }

    public class IncineratingSorterBuild extends SorterBuild {

        // 判断物品是否应该被焚烧
        private boolean shouldIncinerate(Item item) {
            if (!enabled || power.status <= 0) return false;
            // 空选时：反向分类器焚烧所有物品，正向分类器不焚烧（向前输出）
            if (sortItem == null) return invert;
            // 非空选：按照 invert 规则判断
            return invert == (item != sortItem);
        }


        @Override
        public BlockStatus status() {
            if (!enabled) return BlockStatus.logicDisable;
            if (power.status <= 0) return BlockStatus.noInput;
            return BlockStatus.active;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            if (shouldIncinerate(item)) {
                // 焚烧物品：直接接受（用于销毁）
                return source.relativeTo(tile.x, tile.y) != -1;
            }
            return super.acceptItem(source, item);
        }

        @Override
        public void handleItem(Building source, Item item) {
            if (shouldIncinerate(item)) {
                // 焚烧：物品消失，不传递
                return;
            }
            super.handleItem(source, item);
        }

        @Override
        public Building getTileTarget(Item item, Building source, boolean flip) {
            // 通电时，不应焚烧的物品强制向前
            if (power.status > 0 && !shouldIncinerate(item)) {
                int dir = source.relativeTo(tile.x, tile.y);
                if (dir == -1) return null;
                return nearby(dir);
            }
            // 断电，原分类器逻辑
            return super.getTileTarget(item, source, flip);
        }

        @Override
        public void draw() {
            if (sortItem == null) {
                Draw.rect(cross, x, y);
            } else {
                Draw.color(sortItem.color);
                Fill.circle(x, y, 3f);
                Draw.color();
            }
            super.draw();
        }
    }
}