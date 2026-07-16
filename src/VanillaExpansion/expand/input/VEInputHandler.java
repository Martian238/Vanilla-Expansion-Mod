package VanillaExpansion.expand.input;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.graphics.Pal;
import mindustry.input.*;
import mindustry.world.*;
import VanillaExpansion.expand.world.block.*;

import static mindustry.Vars.*;

public class VEInputHandler extends DesktopInput {

    @Override
    public void update() {
        // 在调用父类之前检测滚轮输入
        int scroll = (int)Core.input.axisTap(Binding.rotate);
        int savedScroll = scroll;

        // 调用父类update（会消耗axisTap）
        super.update();

        // 如果检测到滚轮输入且正在放置16方向方块，更新16方向旋转
        if (savedScroll != 0 && block instanceof SixteenDirectionBlock) {
            SixteenDirectionBlock.addSixteenRotation(Mathf.sign(savedScroll));
        }
    }

    @Override
    public void drawArrow(Block block, int x, int y, int rotation, boolean valid) {
        // 如果是16方向方块，使用自定义箭头绘制
        if (block instanceof SixteenDirectionBlock) {
            SixteenDirectionBlock sixteenBlock = (SixteenDirectionBlock) block;

            // 使用16方向旋转值，而不是参数中的原版rotation（0-3）
            float angle = sixteenBlock.getDrawRotation(SixteenDirectionBlock.getSixteenRotation());
            float trns = (block.size / 2) * tilesize;
            
            float dx = Angles.trnsx(angle, trns);
            float dy = Angles.trnsy(angle, trns);
            
            float offsetx = x * tilesize + block.offset + dx;
            float offsety = y * tilesize + block.offset + dy;
            
            Draw.color(!valid ? Pal.removeBack : Pal.accentBack);
            TextureRegion regionArrow = Core.atlas.find("place-arrow");
            
            Draw.rect(regionArrow,
                offsetx,
                offsety - 1,
                regionArrow.width * regionArrow.scl(),
                regionArrow.height * regionArrow.scl(),
                angle - 90);
            
            Draw.color(!valid ? Pal.remove : Pal.accent);
            Draw.rect(regionArrow,
                offsetx,
                offsety,
                regionArrow.width * regionArrow.scl(),
                regionArrow.height * regionArrow.scl(),
                angle - 90);
            
            Draw.reset();
            return;
        }
        
        // 其他方块使用原版绘制
        super.drawArrow(block, x, y, rotation, valid);
    }
}
