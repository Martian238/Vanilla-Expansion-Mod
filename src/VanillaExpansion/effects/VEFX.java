package VanillaExpansion.effects;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import VanillaExpansion.*;
import VanillaExpansion.expand.graphics.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import static arc.math.Interp.*;
import static arc.graphics.g2d.Draw.*;
public class VEFX{
    public static Effect shield = new Effect(30f, e -> {
        blend(Blending.additive);
        color(Tmp.c1.set(VEPal.primary).a(Mathf.absin(e.fin(pow2Out), 1f / 50f, 1f) * 0.5f * e.fout()));

        Fill.polyBegin();
        for(int i = 0; i < 6; i++){
            float ang = i * (360f / 6f);
            Tmp.v1.trns(ang, 30f);
            Tmp.v1.y *= 0.333f;

            Vec2 v = Tmp.v2.trns(e.rotation + 90f, Tmp.v1.x, Tmp.v1.y).add(e.x, e.y);
            Fill.polyPoint(v.x, v.y);
        }
        Fill.polyEnd();

        blend();
    });
    public static Effect fragmentGroundImpact = new Effect(40f, 300f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        color(e.color);

        float size = e.rotation;
        int iter = ((int)(size / 8f)) + 6;
        for(int i = 0; i < iter; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size) + (r.random(0.5f, 1f) * size * 0.5f + 20f) * e.finpow()).add(e.x, e.y);
            Fill.circle(v.x, v.y, r.random(5f, 16f) * e.fout());
        }
    }).layer(Layer.debris);
    public static Effect fragmentExplosion = new Effect(40f, 300f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);
        float size = e.rotation;
        e.lifetime = size / 1.5f + 10f;

        int iter = ((int)(size / 7f)) + 12;
        int iter3 = ((int)(size / 14.5f)) + 12;
        color(Color.gray);
        //alpha(0.9f);
        for(int i = 0; i < iter3; i++){
            //
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size / 2f) * e.finpow());
            float s = r.random(size / 2.75f, size / 2f) * e.fout();
            Fill.circle(v.x + e.x, v.y + e.y, s);
        }
        for(int i = 0; i < iter; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size) + (r.random(0.25f, 2f) * size) * e.finpow());
            float s = r.random(size / 3.5f, size / 2.5f) * e.fout();
            Fill.circle(v.x + e.x, v.y + e.y, s);
            Fill.circle(v.x / 2 + e.x, v.y / 2 + e.y, s * 0.5f);
        }

        float sfin = Mathf.curve(e.fin(), 0f, 0.65f);
        if(sfin < 1f){
            int iter2 = ((int)(size / 10f)) + 4;
            float sfout = 1f - sfin;

            color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
            Lines.stroke((1.7f * sfout) * (1f + size / 60f));

            Draw.z(Layer.effect + 0.001f);

            for(int i = 0; i < iter2; i++){
                Vec2 v = Tmp.v1.trns(r.random(360f), r.random(0.001f, size / 2f) + (r.random(0.4f, 2.2f) * size) * pow2Out.apply(sfin));
                Lines.lineAngle(e.x + v.x, e.y + v.y, Mathf.angle(v.x, v.y), 1f + sfout * 3 * (1f + size / 50f));
            }
        }
    });
    public static Effect fragmentExplosionSpark = new Effect(26f, 300f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);
        float size = e.rotation;
        e.lifetime = size / 1.5f + 10f;

        float sfin = e.fin();

        int iter2 = ((int)(size / 12f)) + 3;
        float sfout = 1f - sfin;

        color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        Lines.stroke((1.7f * sfout) * (1f + size / 60f));

        Draw.z(Layer.effect + 0.001f);

        for(int i = 0; i < iter2; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(0.001f, size / 2f) + (r.random(0.4f, 2.2f) * size) * pow2Out.apply(sfin));
            Lines.lineAngle(e.x + v.x, e.y + v.y, Mathf.angle(v.x, v.y), 1f + sfout * 3 * (1f + size / 50f));
        }
    });
    public static Effect debrisSmoke = new Effect(40f, e -> {
        color(Color.gray);
        float fin = Utils.biasSlope(e.fin(), 0.075f);
        Fill.circle(e.x, e.y, e.rotation * fin);
    });
    public static Effect empathyParryExplosion = new Effect(40f, e -> {
        color(VEPal.empathyDark, e.fout());
        blend(Blending.additive);
        float r = pow3Out.apply(Mathf.clamp(e.time / 6f)) * e.rotation + e.finpow() * 10f;
        Fill.circle(e.x, e.y, r);
        blend();
    }).layer(Layer.flyingUnitLow + 1f);
}
