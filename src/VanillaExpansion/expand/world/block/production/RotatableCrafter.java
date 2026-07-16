package VanillaExpansion.expand.world.block.production;

import VanillaExpansion.expand.world.block.RotatableBlock;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.liquid.Conduit.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class RotatableCrafter extends RotatableBlock {

    public @Nullable ItemStack outputItem;
    public @Nullable ItemStack[] outputItems;

    public @Nullable LiquidStack outputLiquid;
    public @Nullable LiquidStack[] outputLiquids;
    public int[] liquidOutputDirections = {-1};

    public boolean dumpExtraLiquid = true;
    public boolean ignoreLiquidFullness = false;

    public float craftTime = 80;
    public Effect craftEffect = Fx.none;
    public Effect updateEffect = Fx.none;
    public float updateEffectChance = 0.04f;
    public float updateEffectSpread = 4f;
    public float warmupSpeed = 0.019f;

    public DrawBlock drawer = new DrawDefault();

    public RotatableCrafter(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        rotate = true;
        autoRotate = true;
        showConfigUI = true;
        ambientSound = Sounds.loopMachine;
        ambientSoundVolume = 0.03f;
        flags = EnumSet.of(BlockFlag.factory);
        drawArrow = true;
    }

    @Override
    public void setStats() {
        stats.timePeriod = craftTime;
        super.setStats();
        if ((hasItems && itemCapacity > 0) || outputItems != null) {
            stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
        }

        if (outputItems != null) {
            stats.add(Stat.output, StatValues.items(craftTime, outputItems));
        }

        if (outputLiquids != null) {
            stats.add(Stat.output, StatValues.liquids(1f, outputLiquids));
        }
    }

    @Override
    public void setBars() {
        super.setBars();

        if (outputLiquids != null && outputLiquids.length > 0) {
            removeBar("liquid");

            for (var stack : outputLiquids) {
                addLiquidBar(stack.liquid);
            }
        }
    }

    @Override
    public boolean rotatedOutput(int fromX, int fromY, Tile destination) {
        if (!(destination.build instanceof ConduitBuild)) return false;

        Building crafter = world.build(fromX, fromY);
        if (crafter == null) return false;
        int relative = Mathf.mod(crafter.relativeTo(destination) - crafter.rotation, 4);
        for (int dir : liquidOutputDirections) {
            if (dir == -1 || dir == relative) return false;
        }

        return true;
    }

    @Override
    public void load() {
        super.load();
        drawer.load(this);
    }

    @Override
    public void init() {
        if (outputItems == null && outputItem != null) {
            outputItems = new ItemStack[]{outputItem};
        }
        if (outputLiquids == null && outputLiquid != null) {
            outputLiquids = new LiquidStack[]{outputLiquid};
        }
        if (outputLiquid == null && outputLiquids != null && outputLiquids.length > 0) {
            outputLiquid = outputLiquids[0];
        }
        outputsLiquid = outputLiquids != null;

        if (outputItems != null) hasItems = true;
        if (outputLiquids != null) hasLiquids = true;

        super.init();
    }

    @Override
    public void afterPatch() {
        super.afterPatch();

        outputsLiquid = outputLiquids != null;

        if (outputItems != null) hasItems = true;
        if (outputLiquids != null) hasLiquids = true;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public TextureRegion[] icons() {
        return drawer.finalIcons(this);
    }

    @Override
    public boolean outputsItems() {
        return outputItems != null;
    }

    @Override
    public void getRegionsToOutline(Seq<TextureRegion> out) {
        drawer.getRegionsToOutline(this, out);
    }

    @Override
    public void drawOverlay(float x, float y, int rotation) {
        if (outputLiquids != null) {
            for (int i = 0; i < outputLiquids.length; i++) {
                int dir = liquidOutputDirections.length > i ? liquidOutputDirections[i] : -1;

                if (dir != -1) {
                    Draw.rect(
                        outputLiquids[i].liquid.fullIcon,
                        x + Geometry.d4x(dir + rotation) * (size * tilesize / 2f + 4),
                        y + Geometry.d4y(dir + rotation) * (size * tilesize / 2f + 4),
                        8f, 8f
                    );
                }
            }
        }
    }

    public class RotatableCrafterBuild extends RotatableBlock.RotatableBuild {
        public float progress;
        public float totalProgress;
        public float warmup;

        @Override
        public void draw() {
            drawer.draw(this);
        }

        @Override
        public void drawLight() {
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public boolean shouldConsume() {
            if (outputItems != null) {
                for (var output : outputItems) {
                    if (items.get(output.item) + output.amount > itemCapacity) {
                        return false;
                    }
                }
            }

            if (outputLiquids != null && !ignoreLiquidFullness) {
                boolean allFull = true;
                for (var output : outputLiquids) {
                    if (liquids.get(output.liquid) >= liquidCapacity - 0.001f) {
                        if (!dumpExtraLiquid) {
                            return false;
                        }
                    } else {
                        allFull = false;
                    }
                }

                if (allFull) {
                    return false;
                }
            }

            return enabled;
        }

        @Override
        public void updateTile() {
            super.updateTile();

            if (efficiency > 0) {
                progress += getProgressIncrease(craftTime);
                warmup = Mathf.approachDelta(warmup, warmupTarget(), warmupSpeed);

                if (outputLiquids != null) {
                    float inc = getProgressIncrease(1f);
                    for (var output : outputLiquids) {
                        handleLiquid(this, output.liquid, Math.min(output.amount * inc, liquidCapacity - liquids.get(output.liquid)));
                    }
                }

                if (wasVisible && Mathf.chanceDelta(updateEffectChance)) {
                    updateEffect.at(x + Mathf.range(size * updateEffectSpread), y + Mathf.range(size * updateEffectSpread));
                }
            } else {
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
            }

            totalProgress += warmup * Time.delta;

            if (progress >= 1f) {
                craft();
            }

            dumpOutputs();
        }

        @Override
        public float getProgressIncrease(float baseTime) {
            if (ignoreLiquidFullness) {
                return super.getProgressIncrease(baseTime);
            }

            float scaling = 1f, max = 1f;
            if (outputLiquids != null) {
                max = 0f;
                for (var s : outputLiquids) {
                    float value = (liquidCapacity - liquids.get(s.liquid)) / (s.amount * edelta());
                    scaling = Math.min(scaling, value);
                    max = Math.max(max, value);
                }
            }

            return super.getProgressIncrease(baseTime) * (dumpExtraLiquid ? Math.min(max, 1f) : scaling);
        }

        public float warmupTarget() {
            return 1f;
        }

        @Override
        public float warmup() {
            return warmup;
        }

        @Override
        public float totalProgress() {
            return totalProgress;
        }

        public void craft() {
            consume();

            if (outputItems != null) {
                for (var output : outputItems) {
                    for (int i = 0; i < output.amount; i++) {
                        offload(output.item);
                    }
                }
            }

            if (wasVisible) {
                craftEffect.at(x, y);
            }
            progress %= 1f;
        }

        public void dumpOutputs() {
            if (outputItems != null && timer(timerDump, dumpTime / timeScale)) {
                for (ItemStack output : outputItems) {
                    dump(output.item);
                }
            }

            if (outputLiquids != null) {
                for (int i = 0; i < outputLiquids.length; i++) {
                    int dir = liquidOutputDirections.length > i ? liquidOutputDirections[i] : -1;

                    dumpLiquid(outputLiquids[i].liquid, 2f, dir);
                }
            }
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.progress) return progress();
            if (sensor == LAccess.totalLiquids && outputLiquid != null) return liquids.get(outputLiquid.liquid);
            return super.sense(sensor);
        }

        @Override
        public float progress() {
            return Mathf.clamp(progress);
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return itemCapacity;
        }

        @Override
        public boolean shouldAmbientSound() {
            return efficiency > 0;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
            warmup = read.f();
        }
    }
}