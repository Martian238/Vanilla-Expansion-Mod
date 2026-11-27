package VanillaExpansion.expand.world.block;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class SixteenDirectionBlock extends Block implements Autotiler {

    public static final int DIRECTIONS = 16;
    public static final float DEG_PER_DIR = 360f / DIRECTIONS;

    private static final float[] ANGLE_MAP = new float[16];
    static {
        for (int i = 0; i < 16; i++) ANGLE_MAP[i] = i * DEG_PER_DIR;
    }
    private static int currentFullDir = 0;
    public static int getCurrentFullDir() { return currentFullDir; }
    public static void setCurrentFullDir(int dir) {
        currentFullDir = Mathf.mod(dir, 16);
    }
    public static void addCurrentFullDir(int delta) {
        currentFullDir = Mathf.mod(currentFullDir + delta, 16);
    }
    public static int fullRotation(int baseRotation) {
        return currentFullDir;
    }
    public static float angleFrom(int fullRot) {
        return ANGLE_MAP[Mathf.mod(fullRot, 16)];
    }
    public static int angleToFullRotation(float angleDeg) {
        return Mathf.round(angleDeg / DEG_PER_DIR) % DIRECTIONS;
    }
    public static boolean isCardinal(int fullRot) {
        return fullRot % 4 == 0;
    }
    public static int toCardinal(int fullRot) {
        return isCardinal(fullRot) ? fullRot / 4 : -1;
    }
    public SixteenDirectionBlock(String name) {
        super(name);
        rotate = true;
        saveConfig = true;
        quickRotate = false;
        config(Integer.class, (SixteenDirectionBuild build, Integer rot) -> {
            build.fullRotation = Mathf.mod(rot, 16);
        });
        buildType = () -> new SixteenDirectionBuild();
    }
    @Override
    public boolean canReplace(Block other) {
        if (other == this) return true;
        return super.canReplace(other);
    }
    @Override
    public Object nextConfig() {
        return currentFullDir;
    }
    @Override
    public void onNewPlan(BuildPlan plan) {
        if (plan.config == null) {
            plan.config = currentFullDir;
        }
    }
    @Override
    public int planRotation(int rot) {
        if (Vars.headless || Core.input == null) return rot;
        int cx = Mathf.round(Core.input.mouseWorld().x / tilesize);
        int cy = Mathf.round(Core.input.mouseWorld().y / tilesize);
        Tile tile = world.tile(cx, cy);
        if (tile != null && tile.build != null && tile.build.block == this && tile.team() == player.team() && rot == tile.build.rotation) {
            if (tile.build instanceof SixteenDirectionBuild sdBuild) {
                if (sdBuild.getFullRotation() != getCurrentFullDir()) {
                    return (rot + 1) % 4;
                }
            } else {
                return (rot + 1) % 4;
            }
        }
        return rot;
    }
    @Override
    public void flipRotation(BuildPlan req, boolean x) {
        if (req.config instanceof Integer full) {
            int flipped = x ? (16 - full) % 16 : (24 - full) % 16;
            req.config = flipped;
            req.rotation = flipped / 4;
        }
    }
    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        int fullRot = currentFullDir;
        if (plan.config instanceof Integer cfg) {
            fullRot = Mathf.mod(cfg, 16);
        }
        float angle = angleFrom(fullRot);
        TextureRegion reg = getPlanRegion(plan, list);
        float a = Draw.getColorAlpha();
        Draw.rect(reg, plan.drawx(), plan.drawy(), angle);
        if (plan.worldContext && player != null && teamRegion != null && teamRegion.found()) {
            if (teamRegions[player.team().id] == teamRegion) Draw.color(player.team().color, a);
            Draw.rect(teamRegions[player.team().id], plan.drawx(), plan.drawy());
            Draw.color(1f, 1f, 1f, a);
        }
        drawPlanConfig(plan, list);
    }
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        if (Core.input.shift()) {
            float cx = x * tilesize + offset;
            float cy = y * tilesize + offset;
            float angle = Angles.angle(cx, cy, Core.input.mouseWorldX(), Core.input.mouseWorldY());
            currentFullDir = angleToFullRotation(angle);
        }
    }
    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return (otherblock.outputsItems() ||
                (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems))
            && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
    }
    public boolean lookingAt(Tile tile, int rotation, int otherx, int othery, Block otherblock) {
        return Point2.equals(
            tile.x + Geometry.d4x(rotation),
            tile.y + Geometry.d4y(rotation),
            otherx, othery
        );
    }
    public boolean lookingAtEither(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        return Point2.equals(tile.x + Geometry.d4x(rotation), tile.y + Geometry.d4y(rotation), otherx, othery)
            || !otherblock.rotatedOutput(otherx, othery, tile)
            || Point2.equals(otherx + Geometry.d4x(otherrot), othery + Geometry.d4y(otherrot), tile.x, tile.y);
    }
    public class SixteenDirectionBuild extends Building implements LReadable, LWritable {
        protected int fullRotation = -1;
        public int getFullRotation() {
            return fullRotation < 0 ? rotation * 4 : fullRotation;
        }
        @Override
        public float drawrot() {
            return angleFrom(getFullRotation());
        }
        @Override
        public void configured(@Nullable Unit builder, Object config) {
            if (config instanceof Integer rot) {
                fullRotation = Mathf.mod(rot, 16);
                rotation = fullRotation / 4;
            }
        }
        @Override
        public Object config() {
            return getFullRotation();
        }
        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(getFullRotation());
        }
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            fullRotation = read.s();
        }
        @Override
        public boolean readable(LExecutor exec) {
            return isValid() && (exec.privileged || (team == exec.team && !block.privileged));
        }
        @Override
        public void read(LVar position, LVar output) {
            int addr = position.numi();
            switch (addr) {
                case 0 -> output.setnum(getFullRotation());
                case 1 -> output.setnum(drawrot());
                case 2 -> output.setnum(isCardinal(getFullRotation()) ? 1 : 0);
                case 3 -> output.setnum(toCardinal(getFullRotation()));
                default -> output.setnum(Double.NaN);
            }
        }
        @Override
        public boolean writable(LExecutor exec) {
            return readable(exec);
        }

        @Override
        public void write(LVar position, LVar value) {
            int addr = position.numi();
            double val = value.num();
            switch (addr) {
                case 0 -> fullRotation = Mathf.mod((int) val, 16);
                case 1 -> fullRotation = Mathf.round((float) (val / DEG_PER_DIR)) % 16;
                case 2 -> fullRotation = Mathf.mod((int) val * 4, 16);
            }
        }
    }
}
