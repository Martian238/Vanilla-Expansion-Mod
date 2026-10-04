package VanillaExpansion.content;

import VanillaExpansion.VEPal;
import VanillaExpansion.expand.world.block.defense.TestPullRequestForceProjector;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.units.UnitAssembler.*;

import static arc.graphics.g2d.Draw.rect;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

public class CustomFx{
    public static final Rand rand = new Rand();
    public static final Vec2 v = new Vec2();

    public static final
        Effect instHitColor = new Effect(20f, 200f, e -> {
            Color c2 = e.color;
            color(c2);
            for (int i = 0; i < 2; i++) {
                color(c2);
                float m = i == 0 ? 1f : 0.5f;

                for (int j = 0; j < 5; j++) {
                    float rot = e.rotation + Mathf.randomSeedRange(e.id + j, 50f);
                    float w = 23f * e.fout() * m;
                    Drawf.tri(e.x, e.y, w, (80f + Mathf.randomSeedRange(e.id + j, 40f)) * m, rot);
                    Drawf.tri(e.x, e.y, w, 20f * m, rot + 180f);
                }
            }

            e.scaled(10f, c -> {
                color(c2);
                stroke(c.fout() * 2f + 0.2f);
                circle(e.x, e.y, c.fin() * 30f);
            });
            e.scaled(12f, c -> {
                color(c2);
                randLenVectors(e.id, 25, 5f + e.fin() * 80f, e.rotation, 60f, (x, y) -> {
                    Fill.square(e.x + x, e.y + y, c.fout() * 3f, 45f);
                });
            });
        }),

    instBombColor = new Effect(15f, 100f, e -> {
        Color c2 = e.color;
        if(e.data instanceof Color ce){
            c2 = ce;
        }
        color(c2);
        stroke(e.fout() * 4f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 20f);

        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 6f, 80f * e.fout(), i*90 + 45);
        }

        color();
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 3f, 30f * e.fout(), i*90 + 45);
        }

        Drawf.light(e.x, e.y, 150f, c2, 0.9f * e.fout());
    }),

    instTrailColor = new Effect(30, e -> {
        Color c2 = e.color;
        for(int i = 0; i < 2; i++){
            color(c2);

            float m = i == 0 ? 1f : 0.5f;

            float rot = e.rotation + 180f;
            float w = 15f * e.fout() * m;
            Drawf.tri(e.x, e.y, w, (30f + Mathf.randomSeedRange(e.id, 15f)) * m, rot);
            Drawf.tri(e.x, e.y, w, 10f * m, rot + 180f);
        }

        Drawf.light(e.x, e.y, 60f, c2, 0.6f * e.fout());
    }),

    instShootColor = new Effect(24f, e -> {
        Color c2 = e.color;
        e.scaled(10f, b -> {
            color(Color.white, c2, b.fin());
            stroke(b.fout() * 3f + 0.2f);
            Lines.circle(b.x, b.y, b.fin() * 50f);
        });

        color(c2);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 13f * e.fout(), 85f, e.rotation + 90f * i);
            Drawf.tri(e.x, e.y, 13f * e.fout(), 50f, e.rotation + 20f * i);
        }

        Drawf.light(e.x, e.y, 180f, c2, 0.9f * e.fout());
    }),

    chainLightningBig = new Effect(30f, 808f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = p.getX(), ty = p.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
        Tmp.v1.set(p).sub(e.x, e.y).nor();

        float normx = Tmp.v1.x, normy = Tmp.v1.y;
        float range = 18f;
        int links = Mathf.ceil(dst / range);
        float spacing = dst / links;

        Lines.stroke(3.5f * e.fout());
        Draw.color(Color.white, e.color, e.fin());

        Lines.beginLine();

        Lines.linePoint(e.x, e.y);

        rand.setSeed(e.id);

        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(rand).scl(range/2f);
                nx = e.x + normx * len + Tmp.v1.x;
                ny = e.y + normy * len + Tmp.v1.y;
            }

            Lines.linePoint(nx, ny);
        }

        Lines.endLine();
    }).followParent(false).rotWithParent(false),

        shieldBreakProjector = new Effect(40, e -> {
            color(e.color);
            stroke(3f * e.fout());
            if(e.data instanceof ForceFieldAbility ab){
                Lines.poly(e.x, e.y, ab.sides, e.rotation + e.fin(), ab.rotation);
                return;
            }else if(e.data instanceof TestPullRequestForceProjector ab){
                Lines.poly(e.x, e.y, ab.sides, e.rotation + e.fin(), ab.shieldRotation);
                return;
            }
            Lines.poly(e.x, e.y, 6, e.rotation + e.fin());
        }).followParent(true),



    shockwaveSparks = new Effect(75f, e -> {
        color(e.color);
        Lines.stroke(1.25f + 1.25f*e.fout());
        float spread = 30f;

        rand.setSeed(e.id);
        for(int i = 0; i < 20; i++){
            float ang = e.rotation + rand.range(17f);
            v.trns(ang, rand.random(e.fin() * 55f));
            Lines.lineAngle(e.x + v.x + rand.range(spread), e.y + v.y + rand.range(spread), ang, e.fout() * 10f * rand.random(1f));
        }
    }),

    shockwaveSparksSmall = new Effect(60f, e -> {
        color(e.color);
        Lines.stroke(0.75f + 0.75f*e.fout());
        float spread = 20f;

        rand.setSeed(e.id);
        for(int i = 0; i < 5; i++){
            float ang = e.rotation + rand.range(17f);
            v.trns(ang, rand.random(e.fin() * 55f));
            Lines.lineAngle(e.x + v.x + rand.range(spread), e.y + v.y + rand.range(spread), ang, e.fout() * 10f * rand.random(1f));
        }
    }),

    shockwaveHitGround = new Effect(50f, 100f, e -> {

        float rad = 5 * tilesize;

        e.scaled(7f, b -> {
            color(Team.crux.color, b.fout());
            Fill.circle(e.x, e.y, rad);
        });

        color(Team.crux.color);
        stroke(e.fout() * 6f);
        Lines.circle(e.x, e.y, rad);

        int points = 8;
        float offset = Mathf.randomSeed(e.id, 360f);
        for(int i = 0; i < points; i++){
            float angle = i* 360f / points + offset;
            //for(int s : Mathf.zeroOne){
            Drawf.tri(e.x + Angles.trnsx(angle, rad), e.y + Angles.trnsy(angle, rad), 6f, 80f * e.fout(), angle/* + s*180f*/);
            //}
        }

        Draw.z(Layer.blockUnder + 0.25f);
        Fill.circle(e.x, e.y, 12f * e.fout());
        color();
        Fill.circle(e.x, e.y, 6f * e.fout());
        Drawf.light(e.x, e.y, rad * 1.6f, Team.crux.color, e.fout());
    }),

    shootTriple = new Effect(15, e -> {
        color(Color.valueOf("2eeaea"), e.color, e.fin());
        float w = 1.3f + 10 * e.fout();
        Drawf.tri(e.x, e.y, w, 35f * e.fout(), e.rotation);
        Drawf.tri(e.x, e.y, w, 6f * e.fout(), e.rotation + 180f);
        Drawf.tri(e.x, e.y, w * 0.8f, 25f * e.fout(), e.rotation + 30f);
        Drawf.tri(e.x, e.y, w * 0.8f, 6f * e.fout(), e.rotation + 210f);
        Drawf.tri(e.x, e.y, w * 0.8f, 25f * e.fout(), e.rotation - 30f);
        Drawf.tri(e.x, e.y, w * 0.8f, 6f * e.fout(), e.rotation + 150f);
    }),

    annihilateInterrupt = new Effect(40f, e -> {
        color(Team.sharded.color.cpy().mul(1.5f), Team.sharded.color.cpy(), e.fin());
        float w = 8f * e.fout();
        float l = 80f * (0.25f * e.fout() + 0.75f);
        float a = 20f;
        if(e.data instanceof Float f){
            w *= f;
            l *= f;
        }
        Drawf.tri(e.x, e.y, w, l, e.rotation + a);
        Drawf.tri(e.x, e.y, w, l, e.rotation - a);
        Drawf.tri(e.x, e.y, w, l, e.rotation + a + 180f);
        Drawf.tri(e.x, e.y, w, l, e.rotation - a + 180f);
    }),

    annihilateChargeStop = new Effect(60, e -> {
        float radius = e.data instanceof Float f ? f : 160f;

        e.scaled(20f, c -> {
            color(e.color, 0.9f);
            stroke(c.fout() * 12f + 0.1f);

            randLenVectors(e.id, (int)(radius * 1.2f), radius/2f + c.finpow() * radius*1.25f, (x, y) -> {
                lineAngle(c.x + x, c.y + y, Mathf.angle(x, y), c.fout() * 5 + 1f);
            });
        });

        color(e.color, e.fout() * 0.9f);
        stroke(e.fout() * 6f);
        Lines.circle(e.x, e.y, radius);
    });
}


