package VanillaExpansion.expand.type.unit.internal;

import VanillaExpansion.content.VEJSStatuses;
import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;

import static mindustry.Vars.*;

public class HyperSpawnUnit extends UnitEntity {


    private float timer = 0;
    private float startX, startY, stopX, stopY;
    private float length, gap;
    private final boolean controlCamera = true;
    private float camX, camY, camScale, camSpeed;
    private float spawnTimer = 0;
    private int metaStep = 0, hyperStep = 0;

    @Override
    public void update(){
        super.update();

        if(!(type instanceof HyperSpawnUnitType h)) return;

        if(timer == 0){
            if(rotation < 45 && rotation >= -45) rotation = 0;
            else if(rotation < 135 && rotation >= 45) rotation = 90;
            else if(rotation < -135 || rotation >= 135) rotation = 180;
            else rotation = -90;
            boolean horizontal = rotation == 90 || rotation == -90;
            length = (horizontal ? state.map.width : state.map.height) * tilesize * 0.75f;
            gap = length * (96f / 300f);
            startX = x + length * 0.5f * Mathf.cosDeg(rotation + 90f);
            startY = y + length * 0.5f * Mathf.sinDeg(rotation + 90f);
            stopX = x + length * 0.5f * Mathf.cosDeg(rotation - 90f);
            stopY = y + length * 0.5f * Mathf.sinDeg(rotation - 90f);
            camX = startX;
            camY = startY;
            camScale = 1f;
            camSpeed = 0.04f;
        }
        timer += Time.delta;
        if(controlCamera) {
            Core.camera.position.x = Mathf.lerpDelta(Core.camera.position.x, camX, camSpeed);
            Core.camera.position.y = Mathf.lerpDelta(Core.camera.position.y, camY, camSpeed);
            if (Core.camera.width >= Core.camera.height) {
                Core.camera.width = 150f * tilesize * camScale;
            } else {
                Core.camera.height = 150f * tilesize * camScale * (1778f / 3072f);
            }
        }

        if(timer <= 540f){
            float prog = timer / 540f;
            float prog2 = prog < 0.5f ? prog * (length - gap) : (1 - (1 - prog) * (length - gap));
            camX = startX + prog2 * length * Mathf.cosDeg(rotation - 90f);
            camY = startY + prog2 * length * Mathf.sinDeg(rotation - 90f);
            if(spawnTimer <= 0 && timer <= 420){
                spawnTimer = Math.abs(Mathf.range(0.15f * 60, 0.65f * 60));
                    if(Mathf.chance(0.4f)){
                        spawn(h.corona, camX + Mathf.range(24f * tilesize) * Mathf.cosDeg(rotation) +
                                        Mathf.range(9f * tilesize) * Mathf.cosDeg(rotation - 90f),
                                camY + Mathf.range(24f * tilesize) * Mathf.sinDeg(rotation) +
                                Mathf.range(9f * tilesize) * Mathf.sinDeg(rotation - 90f));
                    }else{
                        spawn(h.magnetic, camX + Mathf.range(24f * tilesize) * Mathf.cosDeg(rotation) +
                                        Mathf.range(9f * tilesize) * Mathf.cosDeg(rotation - 90f),
                                camY + Mathf.range(24f * tilesize) * Mathf.sinDeg(rotation) +
                                        Mathf.range(9f * tilesize) * Mathf.sinDeg(rotation - 90f));
                    }
            }
            spawnTimer -= Time.delta;
            if(metaStep == 0 && prog >= 0.25f){
                metaStep++;
                spawn(h.meta, x + 15f * tilesize * Mathf.cosDeg(rotation) +
                                -40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + 15f * tilesize * Mathf.sinDeg(rotation) +
                                -40f * tilesize * Mathf.sinDeg(rotation - 90f));
            }
            if(metaStep == 1 && prog >= 0.35f){
                metaStep++;
                spawn(h.meta, x + 15f * tilesize * Mathf.cosDeg(rotation) +
                                40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + 15f * tilesize * Mathf.sinDeg(rotation) +
                                40f * tilesize * Mathf.sinDeg(rotation - 90f));
            }
        }

        if(timer >= 540f + 42f){
            camX = x;
            camY = y;
            camSpeed = 0.1f;
            if(metaStep == 2){
                metaStep++;
                spawn(h.meta, x + -15f * tilesize * Mathf.cosDeg(rotation) +
                                -40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + -15f * tilesize * Mathf.sinDeg(rotation) +
                                -40f * tilesize * Mathf.sinDeg(rotation - 90f));
                spawn(h.text, 1f, 1f);
            }
            if(metaStep == 3 && timer >= 540f + 42f + 30f){
                metaStep++;
                spawn(h.meta, x + -15f * tilesize * Mathf.cosDeg(rotation) +
                                40f * tilesize * Mathf.cosDeg(rotation - 90f),
                        y + -15f * tilesize * Mathf.sinDeg(rotation) +
                                40f * tilesize * Mathf.sinDeg(rotation - 90f));
            }
            if(hyperStep == 0 && timer >= 540f + 42f + 12f){
                hyperStep++;
                spawn(h.hyper, x, y);
            }
            if(hyperStep == 1 && timer >= 540f + 42f + 12f + 360f){
                hyperStep++;
                Unit boss = h.hyperBoss.create(team);
                boss.set(x, y);
                Events.fire(new EventType.UnitCreateEvent(boss, null, this));
                if(!Vars.net.client()){
                    boss.add();
                    Units.notifyUnitSpawn(boss);
                }
                boss.rotation = rotation;
                boss.apply(VEJSStatuses.fakeBoss);
                boss.shield(10000f);
                boss.apply(StatusEffects.unmoving, 90f);
            }
            if(timer <= 540f + 42f + 12f + 360f){
                float prog = (timer - 540f - 42f - 12f) / 360f;
                camScale = 1f - 0.5f * prog + 0.025f * prog * Mathf.sin(Time.time * 0.5f);
                Effect.shake(prog * 2f, 30f, x, y);
            }
            if(hyperStep == 2 && timer >= 540f + 42f + 12f + 360f){
                hyperStep++;
                Effect.shake(32f, 48f, x, y);
            }
            if(timer >= 540f + 42f + 12f + 360f){
                camScale = Mathf.lerpDelta(camScale, 1f, 0.1f);
            }
            if(timer >= 540f + 42f + 12f + 360f + 60f){
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
        if(!Vars.net.client()){
            su.add();
            Units.notifyUnitSpawn(su);
        }
        su.rotation = rotation;
    }
}
