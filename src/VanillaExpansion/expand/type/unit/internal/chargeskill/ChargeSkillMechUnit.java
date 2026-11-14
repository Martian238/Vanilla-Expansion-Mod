package VanillaExpansion.expand.type.unit.internal.chargeskill;

import VanillaExpansion.EntityRegister;
import VanillaExpansion.expand.methods.Drawve;
import arc.graphics.Color;
import arc.util.Time;
import mindustry.gen.MechUnit;
import mindustry.gen.UnitEntity;

public class ChargeSkillMechUnit extends MechUnit {

    public float chargeRingRadius = 40f;
    public float chargeRingStroke = 16f;

    public Color chargeRingColor1 = Color.valueOf("f25555");
    public Color chargeRingColor2 = chargeRingColor1.cpy().mul(1.25f);
    public Color chargeRingColor3 = chargeRingColor1.cpy().mul(1.5f);
    public Color chargeRingDelayColor = Color.valueOf("ffa665");

    public int charges = 0;
    public int interrupts = 0;
    public float chargeDelay = 0;
    public float chargeDelayMax = 0;
    public float chargeTimeMax = 120;

    public float chargeBright = 0;
    public float chargeTime = 0;

    @Override
    public void draw(){
        super.draw();
        drawCharge();
    }

    public void drawCharge(){
        float chargeRot = Time.time;
        Drawve.chargeRing(charges, chargeTime, chargeTimeMax, chargeDelay, chargeDelayMax,
                chargeRingColor1, chargeRingColor2, chargeRingColor3, chargeRingDelayColor,
                x, y, chargeRingRadius, chargeRingStroke, chargeBright, chargeRot, interrupts);
    }

    @Override
    public int classId() {
        return EntityRegister.getID(getClass());
    }
}
