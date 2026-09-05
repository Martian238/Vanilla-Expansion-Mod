package VanillaExpansion.expand.type.unit;

import VanillaExpansion.EntityRegister;
import VanillaExpansion.annotations.Annotations;
import VanillaExpansion.content.VEJSUnitTypes;
import VanillaExpansion.expand.world.block.sandbox.SpawnEgg;
import arc.Core;
import arc.func.Cons;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.core.World;
import mindustry.entities.Units;
import mindustry.entities.bullet.*;
import mindustry.entities.units.StatusEntry;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.logic.LAccess;
import mindustry.mod.Mods;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.meta.Env;

import static mindustry.Vars.*;


public class HyperUnit extends PayloadUnit {
    private boolean multiMod = false, modFound = false;
    private float statTimer = 0f;


    public static PayloadUnit create(){
        return new HyperUnit();
    }

    private void findOtherModEnemies(){
        multiMod = false;
        for(Unit u : Groups.unit){
            if (!u.type.name.startsWith("ve-") && !u.type.isVanilla() && u.team != team){
                multiMod = true;
            }
        }
        for(Building b : Groups.build){
            if (!b.block.name.startsWith("ve-") && !b.block.isVanilla() && b.team != team){
                multiMod = true;
            }
        }
    }

    private void findMods(boolean log){
        multiMod = false;
        if(!mobile) {
            if(log)Log.info("Finding mods...");
            Seq<Mods.LoadedMod> mods = Vars.mods.getMods();
            Seq<String> multiModList = new Seq<>();
            multiModList.clear();
            if (mods != null) {
                for (Mods.LoadedMod mod : mods) {
                    if (mod.name.equals("ve") || mod.dependencies.contains(m -> m.name.equals("ve")) || mod.softDependencies.contains(m -> m.name.equals("ve"))) {
                        continue;
                    } else if (!mod.meta.hidden && mod.enabled()) {
                        multiMod = true;
                        multiModList.add(mod.name);
                    }
                }
            }
            if (multiMod) {
                if(log)Log.info("Other mods found: " + multiModList);
            } else {
                if(log)Log.info("Other mods not found");
            }
        }
        modFound = true;
    }

    private void clearEnemy(){
        Log.info("Clearing enemies");
        int count1 = 0;
        int count2 = 0;
        for(Bullet ebl : Groups.bullet){
            if(ebl.owner != null && ebl.team != team){
                if(ebl.owner instanceof Unit eu){
                    if(eu.team != team && !eu.type.name.startsWith("ve-") && !eu.type.isVanilla()){
                        eu.x(999999999);
                        eu.y(999999999);
                        eu.kill();eu.remove();
                        ebl.remove();
                        count1++;
                    }
                }
                if(ebl.owner instanceof Building eb){
                    if(eb.team != team && !eb.block.name.startsWith("ve-") && !eb.block.isVanilla()){
                        eb.tile.remove();
                        eb.kill();eb.remove();
                        ebl.remove();
                        count2++;
                    }
                }
            }else if(ebl.team != team){
                ebl.remove();
            }
        }
        for(Unit eu : Groups.unit){
            if(eu.team != team && !eu.type.name.startsWith("ve-") && !eu.type.isVanilla()){
                eu.x(999999999);
                eu.y(999999999);
                eu.kill();eu.remove();
                count1++;
            }
        }
        for(Building eb : Groups.build){
            if(eb.team != team && !eb.block.name.startsWith("ve-") && !eb.block.isVanilla() && eb.team != Team.derelict){
                eb.tile.remove();
                eb.kill();eb.remove();
                count2++;
            }
        }
        Log.info("Removed "+count1+" units, "+count2+" buildings");
        if(!Groups.unit.contains(u -> u == this)){
            Groups.unit.add(this);
            Log.info("Re-added into Groups.unit");
        }
    }

    private UnitController protectedController;
    private boolean mobile = false;
    private float updateTimer = 0;
    private boolean controllerGot = false;

    @Override
    public int classId() {
        return EntityRegister.getID(getClass());
    }

    @Override
    public void update(){
        updateTimer = 0;
        if(!modFound){
            mobile = Vars.mobile;
            try {
                findMods(true);
            } catch (Exception e) {
                Log.info("Failed to find mods. Switching to mobile testing mode.");
                mobile = true;
            }
        }
        if(multiMod){
            if(dead){
                dead = false;
                healthSet(maxHealth);
            }
            if(statuses.contains(s -> statusB(s))){
                statuses.clear();
                apply(StatusEffects.boss);
            }
        }
        statTimer++;
        if(statTimer >= 5) {
            keepStats();
            if(mobile) {
                findOtherModEnemies();
            }
            statTimer = 0f;
            if(!statuses.contains(s -> s.effect == StatusEffects.boss) && multiMod){
                apply(StatusEffects.boss);
            }
            if(protectedController == null){
                protectedController = this.controller;
                controllerGot = true;
                Log.info("Controller saved: "+protectedController);
            }
            if(!isShooting() && !isPlayer()){
                if(x < 0) x = 0;
                if(x > state.map.width * tilesize) x = state.map.width * tilesize;
                if(y < 0) y = 0;
                if(y > state.map.height * tilesize) y = state.map.height * tilesize;
            }
            for(Bullet b : Groups.bullet){
                if(b.team != team && !isBulletVanilla(b)){
                    try{
                        b.x(99999999f);
                        b.y(99999999f);
                    }catch(Exception ignored){}
                    b.remove();
                }
            }
        }
        if(multiMod){
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            if(controller() == null || !controller().isValidController()){
                try {
                    resetController();
                }catch (Exception e){
                    try {
                        Core.app.exit();
                    }catch (Exception e2){
                        while(true){}
                    }
                }
            }
        }
        super.update();

        if(controllerGot && multiMod && controller != protectedController){
            controller = protectedController;
            Log.info("Controller protection test: in update()");
        }
    }

    @Override
    public void draw(){
        super.draw();
        if(!state.isEditor() && !state.isPaused() && multiMod){
            updateTimer++;
            if(updateTimer > 60f && !net.client()){
                try {
                    Core.app.exit();
                }catch (Exception e3){
                    while(true){}
                }
            }
        }
        if(controllerGot && multiMod && controller != protectedController){
            controller = protectedController;
            Log.info("Controller protection test: in draw()");
        }
        if(multiMod){
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
        }
    }

    @Override
    public boolean collides(Hitboxc other) {
        if(multiMod && other instanceof Bullet b){
            return isBulletVanilla(b);
        }
        return super.collides(other);
    }

    private boolean isBulletVanilla(Bullet b){
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

    @Override
    public boolean killable() {
        return type.killable(this) && !multiMod;
    }

    @Override
    public boolean shouldUpdateController() {
        return true;
    }

    @Override
    public int cap() {
        return multiMod ? Integer.MAX_VALUE : Units.getCap(team);
    }



    @Override
    public UnitController controller(){
        return controllerGot? protectedController : controller;
    }

    @Override
    public void controller(UnitController next){
        Log.info("Method: controller(next) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.controller(next);
    }

    @Override
    public void resetController(){
        Log.info("Method: resetController() with multiMod: "+multiMod);
        if(protectedController != null){
            controller = protectedController;
            return;
        }
        super.resetController();
    }

    @Override
    public void set(Position pos) {
        Log.info("Method: set(pos) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.set(pos);
    }

    @Override
    public void set(float x, float y) {
        Log.info("Method: set(x, y) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.set(x, y);
    }

    @Override
    public void set(UnitType def, UnitController controller) {
        Log.info("Method: set(def, controller) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.set(def, controller);
    }

    @Override
    public void setType(UnitType type) {
        Log.info("Method: setType(type) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.setType(type);
    }



    private void keepStats(){
        maxHealth = type.health = 190000f;
        drag = type.drag = 0.025f;
        type.speed = 1.5f;
        type.rotateSpeed = 0.5f;
        type.canAttack = true;
        armor = type.armor = 32f;
        type.envEnabled = Env.any;
        type.envDisabled = Env.none;
        type.flying = true;
        type.bounded = false;
    }
    //a

    private boolean statusB(StatusEntry s){
        if (s.effect == StatusEffects.boss) {
            if(StatusEffects.boss.damageMultiplier < 1.3f) StatusEffects.boss.damageMultiplier = 1.3f;
            if(StatusEffects.boss.healthMultiplier < 1.5f) StatusEffects.boss.healthMultiplier = 1.5f;
            if(StatusEffects.boss.speedMultiplier < 1f) StatusEffects.boss.speedMultiplier = 1f;
            if(StatusEffects.boss.damage > 0f) StatusEffects.boss.damage = 0f;
            if(StatusEffects.boss.dragMultiplier != 1f) StatusEffects.boss.dragMultiplier = 1f;
            if(StatusEffects.boss.reloadMultiplier < 1f) StatusEffects.boss.reloadMultiplier = 1f;
            if(StatusEffects.boss.disarm) StatusEffects.boss.disarm = false;
        }
        if (s.effect == StatusEffects.invincible) {
            if(StatusEffects.invincible.healthMultiplier < 1f) StatusEffects.invincible.healthMultiplier = Float.POSITIVE_INFINITY;
            if(StatusEffects.invincible.speedMultiplier < 1f) StatusEffects.invincible.speedMultiplier = 1f;
            if(StatusEffects.invincible.damageMultiplier < 1f) StatusEffects.invincible.damageMultiplier = 1f;
            if(StatusEffects.invincible.reloadMultiplier < 1f) StatusEffects.invincible.reloadMultiplier = 1f;
            if(StatusEffects.invincible.dragMultiplier != 1f) StatusEffects.invincible.dragMultiplier = 1f;
            if(StatusEffects.invincible.damage > 0f) StatusEffects.invincible.damage = 0f;
            if(StatusEffects.invincible.disarm) StatusEffects.invincible.disarm = false;
        }
        if (s.effect.disarm) return true;
        if (!s.effect.name.startsWith("ve-") && !s.effect.isVanilla()) {
            if (s.effect.healthMultiplier < 1f) return true;
            if (s.effect.reloadMultiplier < 1f) return true;
            if (s.effect.speedMultiplier < 1f) return true;
            if (s.effect.damageMultiplier < 1f) return true;
            if (s.effect.dragMultiplier != 1f) return true;
            if (s.effect.damage > 0f) return true;
        }
        return false;
    }

    @Override
    public boolean dead(){
        if(multiMod){
            if(health <= 0){
                healthSet(maxHealth);
            }
            return false;
        }
        return super.dead();
    }

    @Override
    public void dead(boolean d){
        if(multiMod){
            dead = false;
            healthSet(maxHealth);
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                }
            });
            return;
        }
        super.dead(d);
    }

    private void healthSet(float h){
        health = h;
    }

    @Override
    public void kill(){
        Log.info("Method: kill() with multiMod: "+multiMod);
        if(multiMod) {
            if(x < 0) x = 0;
            if(x > state.map.width * tilesize) x = state.map.width * tilesize;
            if(y < 0) y = 0;
            if(y > state.map.height * tilesize) y = state.map.height * tilesize;
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.kill();
    }
    @Override
    public void killed(){
        Log.info("Method: killed() with multiMod: "+multiMod);
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.killed();
    }
    @Override
    public void destroy(){
        Log.info("Method: destroy() with multiMod: "+multiMod);
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.destroy();
    }
    @Override
    public void remove(){
        Log.info("Method: remove() with multiMod: "+multiMod);
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            Log.info("Contains: " + Groups.unit.contains(u -> u == this));
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.remove();
    }
    @Override
    public boolean isAdded(){
        if(multiMod) return true;
        return super.isAdded();
    }
    @Override
    public void team(Team t){
        if(t != this.team && multiMod && !isPlayer()){
            clearEnemy();
            return;
        }
        super.team(t);
    }
    @Override
    public void health(float h){
        if(multiMod){
            healthSet(maxHealth);
            return;
        }
        super.health(h);
    }

    @Override
    public void rawDamage(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.rawDamage(d);
    }
    @Override
    public void damage(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damage(d);
    }
    @Override
    public void damage(float d, boolean withEffect){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damage(d, withEffect);
    }

    @Override
    public void damagePierce(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damagePierce(d);
    }
    @Override
    public void damagePierce(float d, boolean withEffect){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damagePierce(d, withEffect);
    }

    @Override
    public void damageArmorMult(float d, float m){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageArmorMult(d, m);
    }
    @Override
    public void damageArmorMult(float d, float m, boolean withEffect){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageArmorMult(d, m, withEffect);
    }
    @Override
    public void damageContinuous(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageContinuous(d);
    }
    @Override
    public void damageContinuousPierce(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageContinuousPierce(d);
    }
    @Override
    public void damageContinuousArmorMult(float d, float m){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageContinuousArmorMult(d, m);
    }

    @Override
    public void apply(StatusEffect s, float dur){
        if(multiMod && !s.isVanilla() && !s.name.startsWith("ve-") && s != StatusEffects.invincible && s != StatusEffects.boss){
            apply(StatusEffects.invincible, dur);
            return;
        }
        super.apply(s, dur);
    }
    @Override
    public void apply(StatusEffect s){
        if(multiMod && !s.isVanilla() && !s.name.startsWith("ve-") && s != StatusEffects.invincible && s != StatusEffects.boss){
            apply(StatusEffects.invincible, 60f);
            return;
        }
        super.apply(s);
    }
}
