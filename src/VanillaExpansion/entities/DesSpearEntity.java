package VanillaExpansion.entities;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import VanillaExpansion.*;
import VanillaExpansion.effects.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class DesSpearEntity extends DrawEntity{
    public boolean collided;
    public float cx, cy, cr;
    Unit unit;

    float time;
    public float rotation;
    public float size;
    float targetSize;

    float vx, vy;
    public float dx, dy;
    public float tx, ty;

    public boolean main = true;
    public float forceScl = 1f, damageScl = 1f;
    public DesSpearEntity last;
    public boolean crySound = true;
    boolean draw;

    static TextureRegion region;
    static float lifetime = 15f * 60f;
    static float offset = -40f;

    public static DesSpearEntity create(Unit unit, float x, float y, float rotation, boolean draw){
        if(region == null) region = Core.atlas.find("flameout-despondency-spear");
        float ts = Math.max(unit.hitSize / 2f, ((Math.min(unit.type.region.width, unit.type.region.height) * Draw.scl) / 2f) * 0.8f);
        float ts2 = Math.max(unit.hitSize / 2f, ((Math.min(unit.type.region.width, unit.type.region.height) * Draw.scl) / 2f)) * 1.6f;
        //float scl = Mathf.clamp(ts / (region.height * Draw.scl)) * (1f + ts / 100f);
        float scl = Math.max(Mathf.clamp(ts2 / (region.height * Draw.scl)), (ts / (region.height * Draw.scl)) / 3.5f) * (1f + ts / 70f);

        DesSpearEntity e = new DesSpearEntity();
        e.x = x;
        e.y = y;
        e.rotation = rotation;
        e.size = scl;
        e.targetSize = ts;
        e.unit = unit;
        e.draw = draw;

        e.add();

        return e;
    }

    @Override
    public void update(){
        time += Time.delta;

        if(!collided) rotation = Angles.moveToward(rotation, Angles.angle(x, y, unit.x + tx, unit.y + ty), 180f * Mathf.clamp(time / 120f));

        if(time > 20f && unit.isAdded() && !collided){
            float speed = (time - 20f) / 3f + Mathf.pow(Mathf.clamp((time - 20f) / 30f), 2f) * 12f + 20f;
            float lx = x, ly = y;

            Vec2 v = Utils.v.trns(rotation, speed * Time.delta);
            x += v.x;
            y += v.y;

            Vec2 col = Utils.intersectCircle(lx, ly, x, y, unit.x, unit.y, targetSize);
            if(col != null){
                x = col.x;
                y = col.y;

                collided = true;
                cx = unit.x;
                cy = unit.y;
                cr = unit.rotation;

                vx = (v.x / (1f + targetSize / 15f)) * forceScl;
                vy = (v.y / (1f + targetSize / 15f)) * forceScl;
                if(last != null){
                    vx += last.vx / 3f;
                    vy += last.vy / 3f;
                    last.main = false;
                }

                //x -= unit.x;
                //y -= unit.y;

                dx = x - unit.x;
                dy = y - unit.y;

            }
        }
        if(collided){
            cx += vx;
            cy += vy;
            vx *= 1f - 0.075f * Time.delta;
            vy *= 1f - 0.075f * Time.delta;

            if(main){
                unit.x = cx;
                unit.y = cy;
                unit.rotation = cr;
            }

            x = unit.x + dx;
            y = unit.y + dy;
        }
        if(!unit.isAdded() && time < (lifetime - 15f)){
            time = lifetime - 15f;
        }

        if(time >= lifetime){
            remove();
        }
    }

    public boolean fading(){
        return time > (lifetime - 15f);
    }

    @Override
    public float clipSize(){
        return 1000f * size;
    }

    @Override
    protected void addGroup(){
        Groups.all.add(this);
        if(draw) Groups.draw.add(this);
    }

    @Override
    protected void removeGroup(){
        Groups.all.remove(this);
        if(draw) Groups.draw.remove(this);
    }

    @Override
    public void draw(){
        if(region == null) region = Core.atlas.find("flameout-despondency-spear");

        float z = getZ();
        Draw.z(z);
        Draw.color();

        drawRaw();
    }

    public void drawRaw(){
        if(region == null) region = Core.atlas.find("flameout-despondency-spear");
        float fin = Mathf.clamp(time / 15f) * Mathf.clamp((lifetime - time) / 15f);
        Vec2 v = Tmp.v2.trns(rotation, offset * size).add(x, y);

        //unit.elevation > 0.5f ? (lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);
        //UnitType type = unit.type;
        //float z = (unit.elevation > 0.5f ? (type.lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : type.groundLayer + Mathf.clamp(type.hitSize / 4000f, 0f, 0.01f)) - 0.01f;

        TextureRegion r = Tmp.tr1;
        r.set(region);
        r.setU2(Mathf.lerp(region.u, region.u2, fin));
        Draw.rect(r, v.x, v.y, r.width * Draw.scl * size, r.height * Draw.scl * size, rotation);
    }

    public float getZ(){
        UnitType type = unit.type;
        return (unit.elevation > 0.5f ? (type.lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : type.groundLayer + Mathf.clamp(type.hitSize / 4000f, 0f, 0.01f)) - 0.01f;
    }
}
