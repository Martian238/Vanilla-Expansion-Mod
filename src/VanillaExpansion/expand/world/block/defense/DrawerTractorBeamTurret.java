package VanillaExpansion.expand.world.block.defense;

import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.world.blocks.defense.turrets.TractorBeamTurret;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawRegion;
import mindustry.world.draw.DrawTurret;

public class DrawerTractorBeamTurret extends TractorBeamTurret {
    public DrawBlock drawer = new DrawRegion();

    public DrawerTractorBeamTurret(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        drawer.load(this);
    }
    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }
}
