package VanillaExpansion.expand.type.unit.internal;

import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.gen.TimedKillUnit;
import mindustry.gen.TimedKillc;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.unit.MissileUnitType;

public class ChiniunChargeUnitType extends MissileUnitType {
    public ChiniunChargeUnitType(String name) {
        super(name);
        constructor = ChiniunChargeUnit::new;
    }

    public Sound startChargeSound;

    public float chargeRingRadius = 40f;
    public float chargeRingStroke = 16f;

    public Color chargeRingColor1 = Color.valueOf("f25555");
    public Color chargeRingColor2 = chargeRingColor1.cpy().mul(1.25f);
    public Color chargeRingColor3 = chargeRingColor1.cpy().mul(1.5f);
    public Color chargeRingDelayColor = Color.valueOf("ffa665");

    public int charges = 1;
    public int interrupts = 0;
    public float chargeDelay = 0;
    public float chargeDelayMax = 0;
    public float chargeTimeMax = lifetime;

    @Override
    public void draw(Unit unit){
        float chargeTime = lifetime - (unit instanceof TimedKillUnit u ? u.time() : 0f);
        float x = unit.x, y = unit.y;
        float chargeRot = Time.time;
        float chargeBright = unit instanceof TimedKillUnit u ? Mathf.pow(Mathf.clamp((20f - u.time()) / 20f), 1.5f) : 0f;
        if(charges > 0 && chargeTime > 0 && chargeTimeMax > 0){
            Draw.z(Layer.effect - 1f);
            if(chargeDelay > 0 && chargeDelayMax > 0){
                Draw.color(chargeRingDelayColor);
                Lines.stroke(0.2f * chargeRingStroke);
                if(charges > 1) {
                    for(int i = 0; i < charges; i++){
                        Lines.arc(x, y, chargeRingRadius, (1 - (chargeDelay / chargeDelayMax)) / charges, 360f * ((float) i / charges));
                    }
                }else Lines.arc(x, y, chargeRingRadius, 1 - chargeDelay / chargeDelayMax, 0f);
            }else{
                Draw.color(chargeRingColor1);
                Lines.stroke(0.15f * chargeRingStroke);
                if(charges > 1){
                    for(int i = 0; i < charges - interrupts; i++){
                        Lines.arc(x, y, (chargeRingRadius + chargeRingStroke * 0.5f) * (chargeTime / chargeTimeMax) + chargeRingStroke * 0.15f,
                                0.75f / charges, 360f * ((float) i / charges) + chargeRot * 2.5f);
                    }
                }
                addChargeArc(unit, chargeTime, chargeRot, 6, 1f, 0.4f, -10f);
                addChargeArc(unit, chargeTime, chargeRot, 4, 0.7f, 0.6f, 5f);
                Draw.color(chargeRingColor2);
                addChargeArc(unit, chargeTime, chargeRot, 3, 0.4f, 0.8f, -2.5f);
                Draw.color(chargeRingColor3);
                Lines.stroke(0.2f * chargeRingStroke);
                Lines.circle(x, y, chargeRingRadius * (chargeTime / chargeTimeMax));
                if(chargeBright > 0){
                    Draw.color(chargeRingColor1.cpy().lerp(Color.white, chargeBright));
                    Lines.stroke(chargeBright * chargeRingStroke * 0.65f);
                    Lines.circle(x, y, chargeRingRadius * (chargeTime / chargeTimeMax));
                }
            }
            Draw.reset();
        }
    }

    public void addChargeArc(Unit unit, float chargeTime, float chargeRot, int amount, float stroke, float alpha, float rotSpeed){
        Draw.alpha(alpha);
        Lines.stroke(stroke * chargeRingStroke);
        for(int i = 0; i < amount; i++){
            Lines.arc(unit.x, unit.y, chargeRingRadius * (chargeTime / chargeTimeMax), 0.5f / amount, 360f * ((float) i / amount) + chargeRot * rotSpeed);
        }
    }
}
