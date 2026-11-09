package VanillaExpansion.expand.type.unit;

import mindustry.type.UnitType;

public class OmicronUnitType extends UnitType {
    public OmicronUnitType(String name) {
        super(name);
        targetable = false;
        vulnerableWithPayloads = true;
        hittable = false;
        constructor = OmicronUnit::create;
        targetPriority = -2;
    }

    public float shootVulnerableTime = 240f;
    public boolean buildVulnerable = true;
    public boolean alwaysUntargetable = false;
}
