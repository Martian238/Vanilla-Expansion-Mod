package VanillaExpansion.expand.type.unit;

import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.entities.units.StatusEntry;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Mods;
import mindustry.type.StatusEffect;
import mindustry.world.meta.Env;

import static mindustry.Vars.state;

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

    private void findMods(){
        multiMod = false;
        if(!Vars.mobile) {
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
                Log.info("Other mods found: " + multiModList);
            } else {
                Log.info("Other mods not found");
            }

        }
        modFound = true;
    }

    private void clearEnemy(){
        for(Bullet ebl : Groups.bullet){
            if(ebl.owner != null && ebl.team != team){
                if(ebl.owner instanceof Unit eu){
                    if(eu.team != team && !eu.type.name.startsWith("ve-") && !eu.type.isVanilla()){
                        eu.kill();eu.remove();
                        ebl.remove();
                    }
                }
                if(ebl.owner instanceof Building eb){
                    if(eb.team != team && !eb.block.name.startsWith("ve-") && !eb.block.isVanilla()){
                        eb.kill();eb.remove();
                        ebl.remove();
                    }
                }
            }else if(ebl.team != team){
                ebl.remove();
            }
        }
        for(Unit eu : Groups.unit){
            if(eu.team != team && !eu.type.name.startsWith("ve-") && !eu.type.isVanilla()){
                eu.kill();eu.remove();
            }
        }
        for(Building eb : Groups.build){
            if(eb.team != team && !eb.block.name.startsWith("ve-") && !eb.block.isVanilla() && eb.team != Team.derelict){
                eb.kill();eb.remove();
            }
        }
    }

    @Override
    public void update(){
        if(!modFound){
            findMods();
        }
        if(multiMod){
            if(dead){
                dead = false;
                healthSet(maxHealth);
            }
            statTimer++;
            if(statuses.contains(s -> statusB(s))){
                statuses.clear();
                apply(StatusEffects.boss);
            }
            if(statTimer >= 30) {
                keepStats();
                if(Vars.mobile) {
                    findOtherModEnemies();
                }
                statTimer = 0f;
                if(!statuses.contains(s -> s.effect == StatusEffects.boss)){
                    apply(StatusEffects.boss);
                }
            }
        }
        super.update();
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
            return;
        }
        super.dead(d);
    }

    private void healthSet(float h){
        health = h;
    }

    @Override
    public void kill(){
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            return;
        }
        super.kill();
    }
    @Override
    public void remove(){
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            return;
        }
        super.remove();
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
