package VanillaExpansion.expand.world.block.liquid;
import VanillaExpansion.expand.world.block.distribution.SideOutputConveyor;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
public class SideOutputConduit extends Conduit {
    public SideOutputConduit(String name) {
        super(name);
    }
    @Override
    public void init() {
        super.init();
        if (junctionReplacement == null) junctionReplacement = Blocks.liquidJunction;
        if (bridgeReplacement == null || !(bridgeReplacement instanceof ItemBridge)) bridgeReplacement = Blocks.bridgeConduit;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        if (super.blends(tile, rotation, otherx, othery, otherrot, otherblock)) return true;
        if (tile == null) return false;
        if (!otherblock.hasLiquids) return false;
        // whitelist: self-type allowed (BD check: both sides side-face → disallow)
        if (otherblock instanceof SideOutputConduit) {
            int worldDir = tile.relativeTo(otherx, othery);
            int mySide = Mathf.mod(worldDir - rotation, 4);
            int theirSide = Mathf.mod((worldDir + 2) - otherrot, 4);
            return !((mySide == 1 || mySide == 3) && (theirSide == 1 || theirSide == 3));
        }
        return !otherblock.outputsLiquid;
    }

    @Override
    public Block getReplacement(BuildPlan req, Seq<BuildPlan> plans) {
        if (junctionReplacement == null) return this;

        Boolf<Point2> cont = p -> plans.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && (req.block instanceof SideOutputConduit || req.block instanceof LiquidJunction));
        if (cont.get(Geometry.d4(req.rotation)) &&
            cont.get(Geometry.d4(req.rotation - 2)) &&
            req.tile() != null &&
            req.tile().block() instanceof SideOutputConduit &&
            Mathf.mod(req.tile().build.rotation - req.rotation, 2) == 1) return junctionReplacement;

        if (req.tile() != null && req.tile().block() instanceof SideOutputConveyor &&
            Mathf.mod(req.tile().build.rotation - req.rotation, 2) == 1) {
            Boolf<Point2> lineCont = p -> plans.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y);
            if (lineCont.get(Geometry.d4(req.rotation)) && lineCont.get(Geometry.d4(req.rotation - 2))) {
                return junctionReplacement;
            }
        }
        return this;
    }

    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans) {
        if (bridgeReplacement == null) return;

        boolean hasJunctionReplacement = junctionReplacement != null;
        if (bridgeReplacement instanceof ItemBridge bridge) {
            Placement.calculateBridges(plans, bridge, hasJunctionReplacement, b -> b instanceof SideOutputConduit || b instanceof SideOutputConveyor);
        }
    }

    public class SideOutputConduitBuild extends ConduitBuild {
        public @Nullable Building leftSide;
        public @Nullable Building rightSide;

        public boolean isSideTarget(int x, int y) {
            return (rightSide != null && rightSide.tileX() == x && rightSide.tileY() == y) ||
                   (leftSide != null && leftSide.tileX() == x && leftSide.tileY() == y);
        }

        @Override
        public void onProximityUpdate() {
            int leftDir = Mathf.mod(rotation - 1, 4);
            int rightDir = Mathf.mod(rotation + 1, 4);

            leftSide = tile.nearbyBuild(leftDir);
            rightSide = tile.nearbyBuild(rightDir);

            super.onProximityUpdate();
        }

        @Override
        public void updateTile() {
            smoothLiquid = Mathf.lerpDelta(smoothLiquid, liquids.currentAmount() / liquidCapacity, 0.05f);

            if (liquids.currentAmount() > 0.0001f && timer(timerFlow, 1)) {
                moveLiquidForward(leaks, liquids.current());
                trySideOutput();
                noSleep();
            } else {
                sleep();
            }
        }

        public void trySideOutput() {
            Liquid liquid = liquids.current();
            if (liquid == null || liquids.get(liquid) <= 0.001f) return;

            for (Building target : new Building[]{rightSide, leftSide}) {
                if (target == null || target.team != team || !target.block.hasLiquids) continue;
                if (!(target.block instanceof SideOutputConduit) && target.block.outputsLiquid) continue;
                if (target.block instanceof SideOutputConduit) {
                    int worldDir = tile.relativeTo(target.tileX(), target.tileY());
                    int mySide = Mathf.mod(worldDir - rotation, 4);
                    int theirSide = Mathf.mod((worldDir + 2) - target.rotation, 4);
                    if ((mySide == 1 || mySide == 3) && (theirSide == 1 || theirSide == 3)) continue;
                }
                moveLiquid(target, liquid);
            }
        }
    }
}
