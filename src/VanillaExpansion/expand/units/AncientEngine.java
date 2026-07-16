package VanillaExpansion.expand.units;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.type.UnitType;

import static arc.graphics.g2d.Lines.circleVertices;

public class AncientEngine extends UnitType.UnitEngine {
    public Color engineColor;
    public float forceZ = -1;
    public float phaseOffset = Mathf.random(5);
    public float alphaBase = 0.8f;
    public float scl = 0.825f;
    public float sizeSclPlus = 0.4f;
    public float sizeSclMin = 0.95f;
    public float alphaSclMin = 0.88f;
    public float boostMultiplier = 1f;

    public AncientEngine(float x, float y, float radius, float rotation) {
        super(x, y, radius, rotation);
    }

    public AncientEngine(float x, float y, float radius, float rotation, float phaseOffset) {
        this(x, y, radius, rotation);
        this.phaseOffset = phaseOffset;
    }

    public AncientEngine(float x, float y, float radius, float rotation, float alphaBase, float sizeSclPlus, float sizeSclMin) {
        this(x, y, radius, rotation);
        this.alphaBase = alphaBase;
        this.sizeSclPlus = sizeSclPlus;
        this.sizeSclMin = sizeSclMin;
    }

    public AncientEngine a(float f) {
        this.alphaBase = f;
        return this;
    }

    public void setBoost(float boost) {
        this.boostMultiplier = boost;
    }

    @Override
    public void draw(Unit unit) {
        UnitType type = unit.type;
        if (unit.vel.len2() > 0.001f) {
            float z = Draw.z();

            if (forceZ > 0) Draw.z(forceZ);

            float rot = unit.rotation - 90.0F;
            Color c = type.engineColor == null ? unit.team.color : type.engineColor;
            Tmp.v1.set(x, y).rotate(rot).add(unit.x, unit.y);
            float ex = Tmp.v1.x;
            float ey = Tmp.v1.y;
            float baseRad = Mathf.curve(unit.vel.len2(), 0.001f, type.speed * type.speed) * radius * (sizeSclMin + Mathf.absin(Time.time + phaseOffset, scl, sizeSclPlus));
            float a = alphaBase * (alphaSclMin + Mathf.absin(Time.time * 1.3f - phaseOffset, scl, 0.13f));

            float stretchLength = baseRad * (boostMultiplier - 1f);
            float stretchAngle = unit.rotation + 180f;

            Draw.blend(Blending.additive);
            Draw.alpha(a);

            if (stretchLength > 0.5f) {
                int segments = (int)(stretchLength / 3f) + 1;
                for (int i = 0; i < segments; i++) {
                    float progress = (float)i / segments;
                    float segX = ex + Angles.trnsx(stretchAngle, stretchLength * progress);
                    float segY = ey + Angles.trnsy(stretchAngle, stretchLength * progress);
                    float segRad = baseRad * (1f - progress * 0.6f);
                    float segA = a * (1f - progress * 0.7f);

                    Tmp.c2.set(c).a(segA);
                    Fill.light(segX, segY, circleVertices(segRad), segRad, Tmp.c2, Color.clear);
                    Tmp.c1.set(Tmp.c2).lerp(Color.white, Mathf.absin(Time.time * 1.1f + phaseOffset + i, scl * 1.12512f, 0.14f));
                    Draw.color(Tmp.c1);
                    Draw.alpha(segA * 0.5f);
                    Fill.light(segX, segY, circleVertices(segRad), segRad * 1.125f, Tmp.c1, Color.clear);
                }
            }

            Tmp.c2.set(c).a(a);
            Fill.light(ex, ey, circleVertices(baseRad), baseRad, Tmp.c2, Color.clear);
            Tmp.c1.set(Tmp.c2).lerp(Color.white, Mathf.absin(Time.time * 1.1f + phaseOffset, scl * 1.12512f, 0.14f));
            Draw.color(Tmp.c1);
            Draw.alpha(a * 0.5f);
            Fill.light(ex, ey, circleVertices(baseRad), baseRad * 1.125f, Tmp.c1, Color.clear);

            Drawf.light(ex, ey, baseRad * 1.9f, Tmp.c1, a);

            Draw.reset();
            Draw.blend();

            Draw.z(z);
        }
    }
}