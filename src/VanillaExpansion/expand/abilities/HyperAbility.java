package VanillaExpansion.expand.abilities;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;

public class HyperAbility extends Ability {
    public HyperAbility() {
        display = false;
    }

    //Just some special things for Hyper.

    @Override
    public void created(Unit unit){
        unit.shield = 10000f;
    }

    @Override
    public void displayBars(Unit unit, Table bars){
        if(unit.hasItem()){
            bars.add(new Bar(Core.bundle.format("stat.unititem", unit.stack().item.localizedName, unit.stack().amount, unit.itemCapacity()), unit.stack().item.color, () -> (float) unit.stack().amount / unit.itemCapacity())).row();
        }
    }
}
