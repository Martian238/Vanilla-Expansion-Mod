package VanillaExpansion.expand.abilities;

import VanillaExpansion.content.VEJSUnitTypes;
import VanillaExpansion.expand.type.unit.HyperUnit;
import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.*;
import mindustry.game.EventType;
import mindustry.gen.Bullet;
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
        recoveryTimer = cantRecoveryTimer = 0f;
    }

    private ItemStack items = new ItemStack();
    private boolean has = false;

    private float recoveryTimer = 0f;
    private float cantRecoveryTimer = 0f;

    @Override
    public void update(Unit unit){
        super.update(unit);
        items = unit.stack();
        has = unit.hasItem();
        recoveryTimer ++;
        if(unit instanceof HyperUnit h){
            if(recoveryTimer > 10f){
                recoveryTimer = 0f;
                h.tryRecovery = true;
            }
            if(h.tryRecovery){
                cantRecoveryTimer ++;
            }else{
                cantRecoveryTimer = 0f;
            }
            if(cantRecoveryTimer >= 10f){
                cantRecoveryTimer = 0f;
                try{
                    h.controller(new CommandAI());
                    Unit u = VEJSUnitTypes.hyper.create(unit.team);
                    u.set(unit.x, unit.y);
                    Events.fire(new EventType.UnitCreateEvent(u, null, unit));
                    if(!Vars.net.client()){
                        u.add();
                        Units.notifyUnitSpawn(u);
                    }
                    u.rotation = unit.rotation;
                }catch(Exception ignored){}
            }
            if(h.publicMultiMod){
                paramUnit = unit;
                Groups.bullet.intersect(unit.x - unit.type.hitSize * 4f, unit.y - unit.type.hitSize * 4f,
                        unit.type.hitSize * 8f, unit.type.hitSize * 8f, bulletConsumer);
                Units.nearby(unit.x - unit.type.hitSize * 10f, unit.y - unit.type.hitSize * 10f,
                        unit.type.hitSize * 20f, unit.type.hitSize * 20f, unitConsumer);
            }
        }
    }

    private static Unit paramUnit;

    private static Cons<Bullet> bulletConsumer = b -> {
        if(!isBulletVanilla(b) && b.team != paramUnit.team){
            b.x(99999999f);
            b.y(99999999f);
            b.remove();
        }
        if(b.owner instanceof Unit u && !u.type.isVanilla() && !u.type.name.startsWith("ve-") && b.team != paramUnit.team){
            b.x(99999999f);
            b.y(99999999f);
            b.remove();
        }
    };

    private static Cons<Unit> unitConsumer = u -> {
        if(u.team != paramUnit.team){
            if(!u.type.isVanilla() && !u.type.name.startsWith("ve-")
                    && !u.type.name.equals("new-horizon-nucleoid") && !u.type.name.equals("new-horizon-pester")){
                u.set(999999999f, 999999999f);
                u.x(99999999f);
                u.y(99999999f);
                u.kill();
                u.remove();
            }
        }
    };

    @Override
    public void displayBars(Unit unit, Table bars){
        if(has){
            bars.add(new Bar(Core.bundle.format("stat.unititem", items.item.localizedName, items.amount, unit.itemCapacity()), items.item.color, () -> (float) items.amount / unit.itemCapacity())).row();
        }
    }

    private static boolean isBulletVanilla(Bullet b){
        boolean c = false;
        if(b.type.getClass() == BulletType.class) c = true;
        else if(b.type.getClass() == ArtilleryBulletType.class) c = true;
        else if(b.type.getClass() == BasicBulletType.class) c = true;
        else if(b.type.getClass() == BombBulletType.class) c = true;
        else if(b.type.getClass() == ContinuousLaserBulletType.class) c = true;
        else if(b.type.getClass() == ContinuousFlameBulletType.class) c = true;
        else if(b.type.getClass() == EmpBulletType.class) c = true;
        else if(b.type.getClass() == ExplosionBulletType.class) c = true;
        else if(b.type.getClass() == FlakBulletType.class) c = true;
        else if(b.type.getClass() == LaserBoltBulletType.class) c = true;
        else if(b.type.getClass() == LaserBulletType.class) c = true;
        else if(b.type.getClass() == LightningBulletType.class) c = true;
        else if(b.type.getClass() == LiquidBulletType.class) c = true;
        else if(b.type.getClass() == MassDriverBolt.class) c = true;
        else if(b.type.getClass() == MissileBulletType.class) c = true;
        else if(b.type.getClass() == PointBulletType.class) c = true;
        else if(b.type.getClass() == PointLaserBulletType.class) c = true;
        else if(b.type.getClass() == SapBulletType.class) c = true;
        else if(b.type.getClass() == ShrapnelBulletType.class) c = true;
        else if(b.type.getClass() == SpaceLiquidBulletType.class) c = true;
        return c;
    }
}
