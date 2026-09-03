package VanillaExpansion.expand.abilities;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
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

    private ItemStack items = new ItemStack();
    private boolean has = false;

    @Override
    public void update(Unit unit){
        super.update(unit);
        items = unit.stack();
        has = unit.hasItem();
    }
    @Override
    public void displayBars(Unit unit, Table bars){
        if(has){
            bars.add(new Bar(Core.bundle.format("stat.unititem", items.item.localizedName, items.amount, unit.itemCapacity()), items.item.color, () -> (float) items.amount / unit.itemCapacity())).row();
        }
    }
}
