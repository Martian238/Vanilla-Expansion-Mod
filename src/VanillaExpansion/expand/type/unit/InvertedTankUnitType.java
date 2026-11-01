package VanillaExpansion.expand.type.unit;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.gen.Tankc;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

public class InvertedTankUnitType extends UnitType {
    public InvertedTankUnitType(String name) {
        super(name);
    }

    @Override
    public <T extends Unit & Tankc> void drawTank(T unit){
        applyColor(unit);
        Draw.rect(treadRegion, unit.x, unit.y, unit.rotation - 90);

        if(treadRegion.found()){
            int frame = treadFrames - (int)(unit.treadTime()) % treadFrames - 1;
            for(int i = 0; i < treadRects.length; i ++){
                var region = treadRegions[i][frame];
                var treadRect = treadRects[i];
                float xOffset = -(treadRect.x + treadRect.width/2f);
                float yOffset = -(treadRect.y + treadRect.height/2f);

                for(int side : Mathf.signs){
                    Tmp.v1.set(xOffset * side, yOffset).rotate(unit.rotation - 90);
                    Draw.rect(region, unit.x + Tmp.v1.x / 4f, unit.y + Tmp.v1.y / 4f, treadRect.width / 4f, region.height * region.scale / 4f, unit.rotation - 90);
                }
            }
        }
    }
}
