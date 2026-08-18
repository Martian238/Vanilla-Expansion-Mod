package VanillaExpansion.expand.exp;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

/** Ported from Project Unity's ExpLaserBulletType: single-target beam that grants exp on hit. */
public class ExpLaserBulletType extends ExpBulletType{
    /** Dimensions of laser */
    public float width = 1f, length;
    /** Length increase per owner level, if the owner can level up. */
    public float lengthInc;
    /** Widths of each color */
    public float[] strokes = {2.9f, 1.8f, 1};
    /** Exp gained on hit */
    public int buildingExpGain;
    public boolean hitMissed = false;
    public boolean blip = false;

    public ExpLaserBulletType(float length, float damage){
        super(0.01f, damage);
        this.length = length;
        ammoMultiplier = 1;
        drawSize = length * 2f;
        hitSize = 0f;
        hitEffect = Fx.hitLiquid;
        shootEffect = Fx.hitLiquid;
        lifetime = 18f;
        despawnEffect = Fx.none;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
        expOnHit = false;
    }

    public ExpLaserBulletType(){
        this(120f, 1f);
    }

    public float getLength(Bullet b){
        return length + lengthInc * getLevel(b);
    }

    @Override
    protected float calculateRange(){
        return Math.max(length, maxRange);
    }

    @Override
    public void init(Bullet b){
        super.init(b);
        despawnHit = false;

        setDamage(b);

        Healthc target = linecast(b, b.x, b.y, b.rotation(), getLength(b));
        b.data = target;

        if(target instanceof Hitboxc hit){
            hit.collision(b, hit.x(), hit.y());
            b.collision(hit, hit.x(), hit.y());
            handleExp(b, hit.x(), hit.y(), expGain);
        }else if(target instanceof Building tile && tile.collide(b)){
            tile.collision(b);
            hit(b, tile.x, tile.y);
            handleExp(b, tile.x, tile.y, expGain);
        }else{
            Vec2 v = new Vec2().trns(b.rotation(), getLength(b)).add(b.x, b.y);
            b.data = v;
            if(hitMissed) hit(b, v.x, v.y);
        }
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof Position point){
            Tmp.v1.set(point);

            Draw.color(getColor(b));

            Draw.alpha(0.4f);
            Lines.stroke(b.fout() * width * strokes[0]);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            Draw.alpha(1);
            Lines.stroke(b.fout() * width * strokes[1]);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            Draw.color(Color.white);
            Lines.stroke(b.fout() * width * strokes[2]);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            if(blip){
                Draw.color(Color.white, Tmp.c2, b.fin());
                Lines.circle(Tmp.v1.x, Tmp.v1.y, b.finpow() * width * 5f);
            }
            Draw.reset();

            Drawf.light(b.x, b.y, Tmp.v1.x, Tmp.v1.y, width * 10 * b.fout(), Color.white, 0.6f);
        }
    }

    @Override
    public void drawLight(Bullet b){
        //no light drawn here
    }

    // Ported from Unity's Utils.linecast: returns the first target along the beam, or null.
    private static final Vec2 tV = new Vec2();
    private static final Rect rect = new Rect(), hitRect = new Rect();
    private static Building tmpBuilding;
    private static Unit tmpUnit;

    private static Healthc linecast(Bullet hitter, float x, float y, float angle, float length){
        tV.trns(angle, length);

        tmpBuilding = null;

        if(hitter.type.collidesGround){
            World.raycastEachWorld(x, y, x + tV.x, y + tV.y, (cx, cy) -> {
                Building tile = world.build(cx, cy);
                if(tile != null && tile.team != hitter.team){
                    tmpBuilding = tile;
                    return true;
                }
                return false;
            });
        }

        rect.setPosition(x, y).setSize(tV.x, tV.y);
        float x2 = tV.x + x, y2 = tV.y + y;

        if(rect.width < 0){
            rect.x += rect.width;
            rect.width *= -1;
        }

        if(rect.height < 0){
            rect.y += rect.height;
            rect.height *= -1;
        }

        float expand = 3f;

        rect.y -= expand;
        rect.x -= expand;
        rect.width += expand * 2;
        rect.height += expand * 2;

        tmpUnit = null;

        Units.nearbyEnemies(hitter.team, rect, e -> {
            if((tmpUnit != null && e.dst2(x, y) > tmpUnit.dst2(x, y)) || !e.checkTarget(hitter.type.collidesAir, hitter.type.collidesGround)) return;

            e.hitbox(hitRect);
            Rect other = hitRect;
            other.y -= expand;
            other.x -= expand;
            other.width += expand * 2;
            other.height += expand * 2;

            Vec2 vec = Geometry.raycastRect(x, y, x2, y2, other);

            if(vec != null){
                tmpUnit = e;
            }
        });

        if(tmpBuilding != null && tmpUnit != null){
            if(Mathf.dst2(x, y, tmpBuilding.getX(), tmpBuilding.getY()) <= Mathf.dst2(x, y, tmpUnit.getX(), tmpUnit.getY())){
                return tmpBuilding;
            }
        }else if(tmpBuilding != null){
            return tmpBuilding;
        }

        return tmpUnit;
    }
}
