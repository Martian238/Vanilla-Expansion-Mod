package VanillaExpansion.expand.abilities;

import arc.Events;
import arc.func.Cons;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.EmptyFloor;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.*;

public class EndermanAbility extends Ability {
    public EndermanAbility(){

    }

    public float teleportRadius = 32f * tilesize;
    public float teleportCooldown = -1f;
    public UnitType teleportEffectUnit;

    public float bulletDetectRadius = 6f * tilesize;
    public float enemyDetectRadius = 14f * tilesize;

    private float teleportCooldownTimer = 0f;
    private float healthSaved = 0f;
    private float damageAccumulated = 0f;
    private float damageThreshold = 100f;

    @Override
    public void update(Unit unit){
        super.update(unit);

        if(teleportCooldown > 0) {
            if (teleportCooldownTimer > 0) teleportCooldownTimer--;
            if (teleportCooldownTimer < 0) teleportCooldownTimer = 0f;
        }
        if(healthSaved <= 0) healthSaved = unit.health + unit.shield;
        paramUnit = unit;
        paramField = this;
        if(teleportCooldownTimer <= 0 || teleportCooldown <= 0) {
            tooClose = false;
            Groups.bullet.intersect(unit.x - bulletDetectRadius * 2, unit.y - bulletDetectRadius * 2, 4 * bulletDetectRadius, 4 * bulletDetectRadius, bulletConsumer);
            if (tooClose || damageAccumulated >= damageThreshold) {
                if(damageAccumulated >= damageThreshold) damageAccumulated = 0; healthSaved = unit.health + unit.shield;
                tryTeleport(unit);
            }
            if(healthSaved > unit.health + unit.shield) {
                damageAccumulated += healthSaved - unit.health - unit.shield;
                healthSaved = unit.health + unit.shield;
            }
        }
    }

    public void tryTeleport(Unit unit){
        float originX = unit.x;
        float originY = unit.y;
        for(int i = 0; i < 1000; i++){
            float randomR = Mathf.range(180f);
            float randomD = Mathf.range(teleportRadius);
            float randomX = originX + Mathf.cosDeg(randomR) * randomD;
            float randomY = originY + Mathf.sinDeg(randomR) * randomD;
            if(randomX < 0) randomX = 0;
            if(randomX > state.map.width * tilesize) randomX = state.map.width * tilesize;
            if(randomY < 0) randomY = 0;
            if(randomY > state.map.height * tilesize) randomY = state.map.height * tilesize;
            if(canTeleport(unit, randomX, randomY) || i >= 999){
                doTeleport(unit, randomX, randomY);
                break;
            }
        }
    }

    public void doTeleport(Unit unit, float x, float y){
        float ox = unit.x, oy = unit.y;
        spawnEffectUnit(unit, ox, oy);
        unit.set(x, y);
        spawnEffectUnit(unit, x, y);
        teleportCooldownTimer = teleportCooldown;
    }

    public void spawnEffectUnit(Unit unit, float x, float y){
        if(teleportEffectUnit != null){
            Unit eu = teleportEffectUnit.create(unit.team);
            eu.set(x, y);
            Events.fire(new EventType.UnitCreateEvent(eu, null, unit));
            if(!Vars.net.client()){
                eu.add();
                Units.notifyUnitSpawn(eu);
            }
            eu.rotation = 0f;
        }
    }

    private boolean tooClose;
    private static EndermanAbility paramField;
    private static Unit paramUnit;
    private float tx, ty;
    private static Cons<Unit> unitConsumer = u -> {
        if(paramField.getDistance(paramUnit.x, paramUnit.y, u.x, u.y) <= paramField.enemyDetectRadius
                && u.team != paramUnit.team && u.type.canAttack){
            paramField.tooClose = true;
        }
    };
    private static Cons<Unit> unitConsumer2 = u -> {
        if(paramField.getDistance(paramField.tx, paramField.ty, u.x, u.y) <= paramField.enemyDetectRadius
                && u.team != paramUnit.team && u.type.canAttack){
            paramField.tooClose = true;
        }
    };
    private static Cons<Bullet> bulletConsumer = b -> {
        if(paramField.getDistance(paramUnit.x, paramUnit.y, b.x, b.y) <= paramField.bulletDetectRadius
                && b.team != paramUnit.team){
            paramField.tooClose = true;
        }
    };
    private static Cons<Bullet> bulletConsumer2 = b -> {
        if(paramField.getDistance(paramField.tx, paramField.ty, b.x, b.y) <= paramField.bulletDetectRadius
                && b.team != paramUnit.team){
            paramField.tooClose = true;
        }
    };
    private static Cons<Building> buildingConsumer = b -> {
        if(paramField.getDistance(paramUnit.x, paramUnit.y, b.x, b.y) <= paramField.enemyDetectRadius
                && b.team != paramUnit.team){
            paramField.tooClose = true;
        }
    };
    private static Cons<Building> buildingConsumer2 = b -> {
        if(paramField.getDistance(paramField.tx, paramField.ty, b.x, b.y) <= paramField.enemyDetectRadius
                && b.team != paramUnit.team){
            paramField.tooClose = true;
        }
    };

    public boolean canTeleport(Unit unit, float x, float y){
        if(!canSpawnUnit(unit.type(), x, y)){
            return false;
        }
        tooClose = false;
        tx = x;
        ty = y;
        Units.nearby(unit.team, x, y, enemyDetectRadius * 1.5f, unitConsumer2);
        Groups.bullet.intersect(x - bulletDetectRadius * 2, y - bulletDetectRadius * 2, 4 * bulletDetectRadius, 4 * bulletDetectRadius, bulletConsumer2);
        Units.nearbyBuildings(x, y, enemyDetectRadius * 1.5f, buildingConsumer2);
        return !tooClose;
    }

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }

    public boolean canSpawnUnit(UnitType u, float x, float y) {
        Floor floor = world.floorWorld(x, y);
        Tile tile = world.tileWorld(x, y);
        Building build = world.buildWorld(x, y);
        if(u.flying || u.canBoost) {
            return true;
        }else if(u.naval){
            if(floor != null){
                if(floor.isLiquid){
                    if(tile != null){
                        if(!tile.block().solid){
                            if(build != null){
                                return !build.block.solid;
                            }else return true;
                        }else return false;
                    }else{
                        if(build != null){
                            return !build.block.solid;
                        }else return true;
                    }
                }else return false;
            }else return false;
        }else{
            if((u.constructor instanceof LegsUnit && u.allowLegStep) || (u.constructor instanceof CrawlUnit)){
                if(floor != null){
                    if(floor.placeableOn && !(floor instanceof EmptyFloor)){
                        if(tile != null){
                            return !tile.block().solid;
                        }else return true;
                    }else return false;
                }else return false;
            }else{
                if(floor != null){
                    if(floor.placeableOn && !(floor instanceof EmptyFloor)){
                        if(tile != null){
                            if(!tile.block().solid){
                                if(build != null){
                                    return !build.block.solid;
                                }else return true;
                            }else return false;
                        }else{
                            if(build != null){
                                return !build.block.solid;
                            }else return true;
                        }
                    }else return false;
                }else return false;
            }
        }
    }
}
