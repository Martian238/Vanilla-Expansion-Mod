package VanillaExpansion.expand.bullets;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.RailBulletType;
import mindustry.entities.effect.WrapEffect;
import mindustry.gen.Bullet;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;

public class DarkSlashBulletType extends BasicBulletType {
    public DarkSlashBulletType() {
        absorbable = false;
        reflectable = false;
        hittable = false;
        collides = false;
        hitEffect = despawnEffect = Fx.none;
        speed = 0;
        lifetime = 30f;
    }

    public float slashLength = 64f;
    public float slashWidth = 8f;
    public float slashStroke = 2f;
    public boolean slashHorizontallyShrink = true;

    public float slashAngleOffset = 90f;
    public float slashAngleRand = 90f;

    public float slashDamageInterval = 5;
    public RailBulletType damageDealer = new RailBulletType(){{
        hitEffect = despawnEffect = Fx.none;
        pierceDamageFactor = 0;
        pierceCap = -1;
    }};


    @Override
    public void init(Bullet b){
        super.init(b);
        b.rotation(b.rotation() + slashAngleOffset + Mathf.range(slashAngleRand / 2f));
        intervalRandomSpread = 0;
        intervalSpread = 180;
        bulletInterval = slashDamageInterval;
        intervalBullets = 2;
        intervalBullet = damageDealer;
        damageDealer.damage = damage;
        damageDealer.hitColor = frontColor;
        damageDealer.length = slashLength;
        damageDealer.status = status;
        damageDealer.statusDuration = statusDuration;
        damageDealer.statusChance = statusChance;
        damageDealer.knockback = knockback;
        damageDealer.buildingDamageMultiplier = buildingDamageMultiplier;
        damageDealer.armorMultiplier = armorMultiplier;
        damageDealer.pierceArmor = pierceArmor;
        damageDealer.blockArmorMultiplier = blockArmorMultiplier;
        damageDealer.pierceEffect = new WrapEffect(){{
            effect = Fx.hitLaserBlast;
            color = backColor;
        }};
    }
    @Override
    public void draw(Bullet b){
        Draw.z(Layer.bullet - 0.01f);
        Draw.color(frontColor.cpy().lerp(backColor, b.fin()));
        float stroke = slashStroke * (1f - b.fin());
        Lines.stroke(stroke);
        float pLen = slashHorizontallyShrink ? 1f : (1f - b.fin());
        float pWid = slashHorizontallyShrink ? (1f - b.fin()) : 1f;
        float len = slashLength * 0.5f;
        float wid = slashWidth * 0.5f;
        float r = b.rotation();
        float x1 = b.x + pLen * len * Mathf.cosDeg(r);
        float y1 = b.y + pLen * len * Mathf.sinDeg(r);
        float x2 = b.x + pWid * wid * Mathf.cosDeg(r - 90f);
        float y2 = b.y + pWid * wid * Mathf.sinDeg(r - 90f);
        float x3 = b.x - pLen * len * Mathf.cosDeg(r);
        float y3 = b.y - pLen * len * Mathf.sinDeg(r);
        float x4 = b.x - pWid * wid * Mathf.cosDeg(r - 90f);
        float y4 = b.y - pWid * wid * Mathf.sinDeg(r - 90f);
        Lines.quad(x1, y1, x2, y2, x3, y3, x4, y4);
        Drawf.light(x1, y1, x2, y2,
                stroke, frontColor, 1f);
        Drawf.light(x2, y2, x3, y3,
                stroke, frontColor, 1f);
        Drawf.light(x3, y3, x4, y4,
                stroke, frontColor, 1f);
        Drawf.light(x4, y4, x1, y1,
                stroke, frontColor, 1f);
        Draw.z(Layer.bullet - 0.1f);
        Draw.color(Color.black);
        Fill.quad(x1, y1, x2, y2, x3, y3, x4, y4);
        Draw.color();
    }
}
