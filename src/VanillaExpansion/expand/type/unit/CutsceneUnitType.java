package VanillaExpansion.expand.type.unit;

import arc.Core;
import arc.func.Func;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import mindustry.ai.ItemUnitStance;
import mindustry.ai.UnitCommand;
import mindustry.ai.UnitStance;
import mindustry.ai.types.CommandAI;
import mindustry.ai.types.LogicAI;
import mindustry.content.Blocks;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Bar;

import static mindustry.Vars.iconMed;
import static mindustry.Vars.indexer;

public class CutsceneUnitType extends UnitType {
    public CutsceneUnitType(String name) {
        super(name);
        playerControllable = false;
        controller = u -> new CommandAI();
    }

    public Func<Unit, ? extends UnitController> controller2 = u -> new CommandAI();

    @Override
    public UnitController createController(Unit unit){
        return controller2.get(unit);
    }

    @Override
    public Unit create(Team team){
        Unit unit = constructor.get();
        unit.team = team;
        unit.setType(this);
        if(unit.controller() instanceof CommandAI command && defaultCommand != null){
            command.command = defaultCommand;
        }
        for(var ability : unit.abilities){
            ability.created(unit);
        }
        unit.elevation = flying ? 1f : 0;
        unit.heal();
        if(unit instanceof TimedKillc u){
            u.lifetime(lifetime);
        }
        return unit;
    }

    @Override
    public void getUnitStances(Unit unit, Seq<UnitStance> out){
        if(!(unit.controller() instanceof CommandAI ai)) return;

        var current = ai.currentCommand();

        //return mining stances based on present items
        if(current == UnitCommand.mineCommand){
            out.add(UnitStance.mineAuto);
            for(Item item : indexer.getAllPresentOres()){
                if(unit.canMine(item) && ((mineFloor && indexer.hasOre(item)) || (mineWalls && indexer.hasWallOre(item)))){
                    var itemStance = ItemUnitStance.getByItem(item);
                    if(itemStance != null){
                        out.add(itemStance);
                    }
                }
            }
        }else{
            for(var stance : stances){
                if(stance.isCompatible(current)){
                    out.add(stance);
                }
            }
        }

        //there might be duplicates, but that shouldn't cause issues
        out.addAll(current.extraStances);
    }

    @Override
    public void display(Unit unit, Table table){
        table.table(t -> {
            t.left();
            t.add(new Image(uiIcon)).size(iconMed).scaling(Scaling.fit);
            t.labelWrap(unit.isPlayer() ? unit.getPlayer().coloredName() + "\n[lightgray]" + localizedName : localizedName).left().width(190f).padLeft(5);
        }).growX().left();
        table.row();
    }

    @Override
    public void init(){
        super.init();
        commands.clear();
    }
}
