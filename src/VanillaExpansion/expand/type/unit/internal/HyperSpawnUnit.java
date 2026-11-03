package VanillaExpansion.expand.type.unit.internal;

import VanillaExpansion.EntityRegister;
import VanillaExpansion.VESettings;
import VanillaExpansion.content.VEJSStatuses;
import VanillaExpansion.content.VEJSUnitTypes;
import VanillaExpansion.expand.abilities.BossBarAbility;
import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;

import static mindustry.Vars.*;

public class HyperSpawnUnit extends UnitEntity {


    private float timer = 0;
    private float startX, startY, stopX, stopY;
    private float length, gap;
    private final boolean controlCamera = true;
    private final boolean changeRules = true;
    private float camX, camY, camScale, camSpeed;
    private float spawnTimer = 0;
    private int metaStep = 0, hyperStep = 0;
    private final boolean debug = false;

    @Override
    public void update(){
        super.update();

        if(!(type instanceof HyperSpawnUnitType h)) return;

        if(timer == 0){
            Sounds.waveSpawn.play(1f);
            if(rotation < 45 && rotation >= -45) rotation = 0;
            else if(rotation < 135 && rotation >= 45) rotation = 90;
            else if(rotation < -135 || rotation >= 135) rotation = 180;
            else rotation = -90;
            boolean horizontal = rotation == 90 || rotation == -90;
            length = (horizontal ? state.map.width : state.map.height) * tilesize * 0.75f;
            gap = length * (96f / 300f);
            startX = x - length * 0.5f * Mathf.cosDeg(rotation - 90f);
            startY = y - length * 0.5f * Mathf.sinDeg(rotation - 90f);
            stopX = x + length * 0.5f * Mathf.cosDeg(rotation - 90f);
            stopY = y + length * 0.5f * Mathf.sinDeg(rotation - 90f);
            camX = startX;
            camY = startY;
            camScale = 1f;
            camSpeed = 0.04f;
            if(debug) Log.info("x:"+x+",y:"+y+",startX:"+startX+",startY:"+startY+",stopX:"+stopX+",stopY:"+stopY);
            if(debug) Log.info("horizontal:"+horizontal+",length:"+length+",gap:"+gap);
        }
        timer += Time.delta;
        if(controlCamera && !VESettings.getBool("ve-skip-cutscene")) {
            control.input.logicCutscene = true;
            control.input.logicCamPan = new Vec2(camX, camY);
            control.input.logicCamSpeed = camSpeed;
            //Core.camera.position.x = Mathf.lerpDelta(Core.camera.position.x, camX, camSpeed);
            //Core.camera.position.y = Mathf.lerpDelta(Core.camera.position.y, camY, camSpeed);
            control.input.logicCutsceneZoom = 0.04f / camScale;
            /*
            if (Core.camera.width >= Core.camera.height) {
                Core.camera.width = 150f * tilesize * camScale;
            } else {
                Core.camera.height = 150f * tilesize * camScale * (1778f / 3072f);
            }
            */
        }

        if(timer <= 540f){
            float prog = timer / 420f;
            float prog2 = prog < 0.5f ? prog * (0.9f * (length - gap) / length) : (1 - (1 - prog) * (0.9f * (length - gap) / length));
            float prog3 = Mathf.clamp(timer - 75f, 0, 420f) / 420f;
            //float prog4 = prog < 0.5f ? prog3 * ((length - gap) / length) : (1 - (1 - prog3) * ((length - gap) / length));
            float prog4 = prog3;
            camX = Mathf.lerp(startX, stopX, prog4);
            camY = Mathf.lerp(startY, stopY, prog4);
            float sx = Mathf.lerp(startX, stopX, prog2);
            float sy = Mathf.lerp(startY, stopY, prog2);
            if(spawnTimer <= 0 && timer <= 420){
                spawnTimer = Math.abs(Mathf.range(0.1f * 60, 0.4f * 60));
                    if(Mathf.chance(0.4f)){
                        spawn(h.corona, sx + Mathf.range(24f * tilesize) * Mathf.cosDeg(rotation) +
                                        Mathf.range(9f * tilesize) * Mathf.cosDeg(rotation - 90f),
                                sy + Mathf.range(24f * tilesize) * Mathf.sinDeg(rotation) +
                                Mathf.range(9f * tilesize) * Mathf.sinDeg(rotation - 90f));
                    }else{
                        spawn(h.magnetic, sx + Mathf.range(24f * tilesize) * Mathf.cosDeg(rotation) +
                                        Mathf.range(9f * tilesize) * Mathf.cosDeg(rotation - 90f),
                                sy + Mathf.range(24f * tilesize) * Mathf.sinDeg(rotation) +
                                        Mathf.range(9f * tilesize) * Mathf.sinDeg(rotation - 90f));
                    }
            }
            spawnTimer -= Time.delta;
            if(metaStep == 0 && prog3 >= 0.05f){
                metaStep++;
                spawn(h.meta, x + 15f * tilesize * Mathf.cosDeg(rotation) +
                                -40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + 15f * tilesize * Mathf.sinDeg(rotation) +
                                -40f * tilesize * Mathf.sinDeg(rotation - 90f));
            }
            if(metaStep == 1 && prog3 >= 0.15f){
                metaStep++;
                spawn(h.meta, x + 15f * tilesize * Mathf.cosDeg(rotation) +
                                40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + 15f * tilesize * Mathf.sinDeg(rotation) +
                                40f * tilesize * Mathf.sinDeg(rotation - 90f));
            }
        }

        if(timer >= 540f + 72f){
            camX = x;
            camY = y;
            camSpeed = 0.1f;
            if(metaStep == 2){
                metaStep++;
                spawn(h.meta, x + -15f * tilesize * Mathf.cosDeg(rotation) +
                                -40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + -15f * tilesize * Mathf.sinDeg(rotation) +
                                -40f * tilesize * Mathf.sinDeg(rotation - 90f));
                if(!VESettings.getBool("ve-skip-cutscene")) spawn(h.text, 1f, 1f);
            }
            if(metaStep == 3 && timer >= 540f + 72f + 30f){
                metaStep++;
                spawn(h.meta, x + -15f * tilesize * Mathf.cosDeg(rotation) +
                                40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + -15f * tilesize * Mathf.sinDeg(rotation) +
                                40f * tilesize * Mathf.sinDeg(rotation - 90f));
            }
            if(hyperStep == 0 && timer >= 540f + 72f + 12f){
                hyperStep++;
                spawn(h.hyper, x, y);
            }
            if(hyperStep == 1 && timer >= 540f + 72f + 12f + 360f){
                hyperStep++;
                for(Unit cu : Groups.unit){
                    if(cu.team == team && (cu.type == VEJSUnitTypes.metaUnit || cu.type == VEJSUnitTypes.magnetic || cu.type == VEJSUnitTypes.corona)) {
                        cu.shield = Math.max(cu.shield, 10000f);
                    }
                }
                Unit boss = h.hyperBoss.create(team);
                boss.set(x, y);
                Events.fire(new EventType.UnitCreateEvent(boss, null, this));
                if(!Vars.net.client()){
                    boss.add();
                    Units.notifyUnitSpawn(boss);
                }
                boss.rotation = rotation;
                Time.run(1f, ()-> {
                    boss.apply(VEJSStatuses.fakeBoss);
                    boss.shield(10000f);
                    boss.apply(StatusEffects.unmoving, 240f);
                    boss.apply(StatusEffects.invincible, 240f);
                });
            }
            if(timer <= 540f + 72f + 12f + 360f){
                float prog = (timer - 540f - 72f - 12f) / 360f;
                camScale = 1f - 0.7f * prog * prog * prog * prog + 0.05f * prog * Mathf.sin(Time.time * 0.5f);
                Effect.shake(prog * prog * 4f, 30f, x, y);
            }
            if(hyperStep == 2 && timer >= 540f + 72f + 12f + 360f){
                hyperStep++;
                Effect.shake(64f, 90f, x, y);
                Fx.unitSpawn.at(x, y, rotation, h.hyperBoss);
                camScale = 0.1f;
            }
            if(timer >= 540f + 72f + 12f + 360f){
                camScale = Mathf.lerpDelta(camScale, 1f, 0.15f);
            }
            if(timer >= 540f + 72f + 12f + 360f + 254f){
                control.input.logicCutscene = false;
                remove();
            }
        }


    }

    @Override
    public void draw(){
    }

    private void spawn(UnitType u, float x, float y){
        Unit su = u.create(team);
        su.set(x, y);
        Events.fire(new EventType.UnitCreateEvent(su, null, this));
        Events.fire(new EventType.UnitSpawnEvent(su));
        if(!Vars.net.client()){
            su.add();
            Units.notifyUnitSpawn(su);
        }
        su.rotation = rotation;
    }
    @Override
    public int classId() {
        return EntityRegister.getID(getClass());
    }
}
