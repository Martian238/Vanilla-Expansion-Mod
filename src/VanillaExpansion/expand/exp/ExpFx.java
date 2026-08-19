package VanillaExpansion.expand.exp;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import VanillaExpansion.VEPal;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;

/** Exp 相关特效占位实现，后续随 ExpTurret 完整移植补充 */
public class ExpFx{
    public static final Effect upgradeBlockFx = new Effect(90f, e -> {
        color(Color.white, Color.green, e.fin());
        stroke(e.fout() * 6f * e.rotation);
        square(e.x, e.y, (e.fin() * 4f + 2f) * e.rotation, 0f);
    }),
    placeShine = new Effect(30f, e -> {
        color(e.color);
        stroke(e.fout());
        square(e.x, e.y, e.rotation / 2f + e.fin() * 3f);
        spark(e.x, e.y, 25f, 15f * e.fout(), e.finpow() * 90f);
    }),
    expPoof = new Effect(60f, e -> {
        color(Pal.accent, VEPal.exp, e.fin());
        randLenVectors(e.id, 9, 1f + 30f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.7f * e.fout());
        });
    }),
    expShineRegion = new Effect(25f, e -> {
        color();
        Tmp.c1.set(Pal.accent).lerp(VEPal.exp, e.fin());
        mixcol(Tmp.c1, 1f);
        alpha(1f - e.fin() * e.fin());

        if(e.data instanceof TextureRegion region){
            Draw.rect(region, e.x, e.y, e.rotation);
        }
    }),
    expGain = new Effect(75f, 400f, e -> {
        if(!(e.data instanceof Position pos)) return;

        float fin = Mathf.curve(e.fin(), 0, Mathf.randomSeed(e.id, 0.25f, 1f));
        if(fin >= 1) return;

        float a = angle(e.x, e.y, pos.getX(), pos.getY()) - 90;
        float d = Mathf.dst(e.x, e.y, pos.getX(), pos.getY());
        float fslope = fin * (1f - fin) * 4f;
        float sfin = Interp.pow2In.apply(fin);
        float spread = d / 4f;
        Tmp.v1.trns(a, Mathf.randomSeed(e.id * 2L, -spread, spread) * fslope, d * sfin);
        Tmp.v1.add(e.x, e.y);

        color(VEPal.exp, Color.white, 0.1f + 0.1f * Mathf.sin(Time.time * 0.03f + e.id * 3f));
        Fill.circle(Tmp.v1.x, Tmp.v1.y, 1.5f);
        stroke(0.5f);
        for(int i = 0; i < 4; i++) Drawf.tri(Tmp.v1.x, Tmp.v1.y, 4f, 4 + 1.5f * Mathf.sin(Time.time * 0.12f + e.id * 4f), i * 90f + Mathf.sin(Time.time * 0.04f + e.id * 5f) * 28f);
    });

    static void spark(float x, float y, float w, float h, float r){
        for(int i = 0; i < 4; i++){
            Drawf.tri(x, y, w, h, r + 90 * i);
        }
    }
}