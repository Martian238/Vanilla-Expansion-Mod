package VanillaExpansion.expand.input;
import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.input.*;
import mindustry.world.*;
import VanillaExpansion.expand.world.block.*;
import static mindustry.Vars.*;
public class VEInputHandler extends DesktopInput {
    @Override
    public void update() {
        if (block instanceof SixteenDirectionBlock && selectPlans.isEmpty()) {
            int axis = (int) Core.input.axisTap(Binding.rotate);
            if (axis != 0) {
                SixteenDirectionBlock.addCurrentFullDir(Mathf.sign(axis));
            }
            rotation = SixteenDirectionBlock.getCurrentFullDir() / 4;
            super.update();
        } else {
            super.update();
        }
    }
    @Override
    public void rotatePlans(Seq<BuildPlan> plans, int direction) {
        for (var plan : plans) {
            if (!plan.breaking && plan.block instanceof SixteenDirectionBlock && plan.config instanceof Integer cfg) {
                plan.config = Mathf.mod(cfg + direction * 4, 16);
            }
        }
        super.rotatePlans(plans, direction);
    }
    @Override
    public void drawArrow(Block block, int x, int y, int rotation, boolean valid) {
        if (block instanceof SixteenDirectionBlock) {
            int fullRot = SixteenDirectionBlock.fullRotation(rotation);
            float angle = SixteenDirectionBlock.angleFrom(fullRot);
            float trns = (block.size / 2) * tilesize;
            float dx = Angles.trnsx(angle, trns);
            float dy = Angles.trnsy(angle, trns);
            float ox = x * tilesize + block.offset + dx;
            float oy = y * tilesize + block.offset + dy;
            Draw.color(!valid ? Pal.removeBack : Pal.accentBack);
            TextureRegion arrow = Core.atlas.find("place-arrow");
            Draw.rect(arrow, ox, oy - 1,
                    arrow.width * arrow.scl(),
                    arrow.height * arrow.scl(),
                    angle - 90);
            Draw.color(!valid ? Pal.remove : Pal.accent);
            Draw.rect(arrow, ox, oy,
                    arrow.width * arrow.scl(),
                    arrow.height * arrow.scl(),
                    angle - 90);
            Draw.reset();
            return;
        }
        super.drawArrow(block, x, y, rotation, valid);
    }
    @Override
    protected void flushPlans(Seq<BuildPlan> plans) {
        if (block instanceof SixteenDirectionBlock) {
            for (var plan : plans) {
                Tile tile = world.tile(plan.x, plan.y);
                if (tile != null && tile.build != null && tile.block() == plan.block && tile.team() == player.team()
                        && plan.rotation == tile.build.rotation) {
                    if (tile.build instanceof SixteenDirectionBlock.SixteenDirectionBuild sdBuild
                            && plan.config instanceof Integer cfg) {
                        if (sdBuild.getFullRotation() == Mathf.mod(cfg, 16)) {
                            continue;
                        }
                    }
                    plan.rotation = (plan.rotation + 1) % 4;
                }
            }
        }
        super.flushPlans(plans);
    }
}