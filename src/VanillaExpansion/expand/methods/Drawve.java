package VanillaExpansion.expand.methods;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import mindustry.graphics.Layer;

public class Drawve {

    /** 通用蓄力光圈 */
    public static void chargeRing(
            int charges,
            float chargeTime,
            float chargeTimeMax,
            float chargeDelay,
            float chargeDelayMax,
            Color chargeRingColor1,
            Color chargeRingColor2,
            Color chargeRingColor3,
            Color chargeRingDelayColor,
            float x,
            float y,
            float chargeRingRadius,
            float chargeRingStroke,
            float chargeBright,
            float chargeRot,
            int interrupts
    ){
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
                addChargeArc(6, 1f, 0.4f, -10f,
                        chargeTime, chargeTimeMax, x, y, chargeRingRadius, chargeRingStroke, chargeRot);
                addChargeArc(4, 0.7f, 0.6f, 5f,
                        chargeTime, chargeTimeMax, x, y, chargeRingRadius, chargeRingStroke, chargeRot);
                Draw.color(chargeRingColor2);
                addChargeArc(3, 0.4f, 0.8f, -2.5f,
                        chargeTime, chargeTimeMax, x, y, chargeRingRadius, chargeRingStroke, chargeRot);
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
    private static void addChargeArc(int amount, float stroke, float alpha, float rotSpeed,
                                    float chargeTime, float chargeTimeMax,
                                    float x, float y, float chargeRingRadius, float chargeRingStroke, float chargeRot){
        Draw.alpha(alpha);
        Lines.stroke(stroke * chargeRingStroke);
        for(int i = 0; i < amount; i++){
            Lines.arc(x, y, chargeRingRadius * (chargeTime / chargeTimeMax), 0.5f / amount, 360f * ((float) i / amount) + chargeRot * rotSpeed);
        }
    }
}
