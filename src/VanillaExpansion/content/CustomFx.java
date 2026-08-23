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
        Effect instHit2 = new Effect(20f, 200f, e -> {
            color(VEPal.cyclant);

            for (int i = 0; i < 2; i++) {
                color(i == 0 ? VEPal.cyclant : VEPal.cyclant);

                float m = i == 0 ? 1f : 0.5f;

                for (int j = 0; j < 5; j++) {
                    float rot = e.rotation + Mathf.randomSeedRange(e.id + j, 50f);
                    float w = 23f * e.fout() * m;
                    Drawf.tri(e.x, e.y, w, (80f + Mathf.randomSeedRange(e.id + j, 40f)) * m, rot);
                    Drawf.tri(e.x, e.y, w, 20f * m, rot + 180f);
                }
            }

            e.scaled(10f, c -> {
                color(VEPal.cyclant);
                stroke(c.fout() * 2f + 0.2f);
                circle(e.x, e.y, c.fin() * 30f);
            });

            e.scaled(12f, c -> {
                color(VEPal.cyclant);
                randLenVectors(e.id, 25, 5f + e.fin() * 80f, e.rotation, 60f, (x, y) -> {
                    Fill.square(e.x + x, e.y + y, c.fout() * 3f, 45f);
                });
            });
        }),

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
    });

                ;
    }


