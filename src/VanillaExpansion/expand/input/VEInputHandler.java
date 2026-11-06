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

/**
 * 扩展输入处理，支持16方向方块以22.5°步进旋转。
 *
 * 交互设计：
 *   R键/滚动 → currentFullDir ± 1（步进 22.5°）
 *   Shift+鼠标 → 根据鼠标角度计算（drawPlace 中处理）
 *   每帧同步 rotation = currentFullDir / 4，保证原版兼容
 */
public class VEInputHandler extends DesktopInput {

    @Override
    public void update() {
        if (block instanceof SixteenDirectionBlock && selectPlans.isEmpty()) {
            // 单方块模式：消费 R/滚轮，步进 16 向
            int axis = (int) Core.input.axisTap(Binding.rotate);
            if (axis != 0) {
                SixteenDirectionBlock.addCurrentFullDir(Mathf.sign(axis));
            }
            rotation = SixteenDirectionBlock.getCurrentFullDir() / 4;
            super.update();
        } else {
            // 蓝图模式：不消费，让原版 rotatePlans 处理
            super.update();
        }
    }

    @Override
    public void rotatePlans(Seq<BuildPlan> plans, int direction) {
        // 同步旋转 16 向 config（原版只改了 plan.rotation 4 向）
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
            // 绕过 Build.validPlaceIgnoreUnits 的旋转检查（Build.java:252）
            // 同类型+同 rotation 会被原版禁建；临时改 rotation 使检查通过，
            // 之后在 SixteenDirectionBuild.configured() 中纠正回来
            for (var plan : plans) {
                Tile tile = world.tile(plan.x, plan.y);
                if (tile != null && tile.build != null && tile.block() == plan.block && tile.team() == player.team()
                        && plan.rotation == tile.build.rotation) {
                    // 16方向专属检查：同 config 才禁建，不同 config 才绕开
                    if (tile.build instanceof SixteenDirectionBlock.SixteenDirectionBuild sdBuild
                            && plan.config instanceof Integer cfg) {
                        if (sdBuild.getFullRotation() == Mathf.mod(cfg, 16)) {
                            continue; // 同 config → 让原版旋转检查拦住
                        }
                    }
                    plan.rotation = (plan.rotation + 1) % 4;
                }
            }
        }
        super.flushPlans(plans);
    }
}