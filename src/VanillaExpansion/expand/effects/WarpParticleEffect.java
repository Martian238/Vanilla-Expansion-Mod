package VanillaExpansion.expand.effects;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Vec2;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;

import static arc.math.Mathf.rand;

public class WarpParticleEffect extends Effect {

    public WarpParticleEffect() {
        clip = generalLength * 2f;
    }

    private static final Rand rand = new Rand();
    private static final Vec2 rv = new Vec2();

    public float width = 24f;
    public float generalLength = 720f;
    public float stroke = 1f;
    public float lengthFrom = 20f;
    public float lengthTo = 5f;
    public int particles = 50;
    public Color colorFrom = Color.valueOf("00ffce").mul(1.2f);
    public Color colorTo = Color.valueOf("00ffce");
    public Interp interp = Interp.circleOut;
    public Interp sizeInterp = Interp.pow2In;
    public Interp colorInterp = Interp.linear;
    public float cone = 0;

    @Override
    public void render(EffectContainer e){
        float rawfin = e.fin();
        float fin = e.fin(interp);
        float colFin = e.fin(colorInterp);
        Draw.color(colorFrom, colorTo, colFin);
        Lines.stroke(sizeInterp.apply(stroke, 0, rawfin));
        float len = sizeInterp.apply(lengthFrom, lengthTo, rawfin);

        rand.setSeed(e.id);
        for(int i = 0; i < particles; i++){
            float l = generalLength * fin;
            float c = rand.random(-1f, 1f);
            float lm = rand.random(0.5f, 2f);
            rv.trns(e.rotation + rand.range(cone), rand.random(l) * Mathf.pow(Mathf.clamp(1f - Math.abs(c)), 0.5f));
            float x = rv.x + width * 0.5f * c * Mathf.cosDeg(e.rotation - 90f), y = rv.y + width * 0.5f * c * Mathf.sinDeg(e.rotation - 90f);

            Lines.lineAngle(e.x + x, e.y + y, e.rotation, Mathf.clamp(e.fout() - (1 - lm)) * len * Mathf.pow(Mathf.clamp(1.25f - Math.abs(c)), 0.5f), 0);
            Drawf.light(e.x + x, e.y + y, len, Draw.getColor(), 1f);
        }
    }
}
