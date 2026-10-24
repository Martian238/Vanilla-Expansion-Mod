package VanillaExpansion.expand.type.unit.internal;

import VanillaExpansion.content.VEJSUnitTypes;
import mindustry.type.UnitType;

public class HyperSpawnUnitType extends UnitType {
    public HyperSpawnUnitType(String name) {
        super(name);
        constructor = HyperSpawnUnit::new;
    }

    public UnitType corona;
    public UnitType magnetic;
    public UnitType meta;
    public UnitType hyper, hyperBoss;
    public UnitType text = VEJSUnitTypes.textTrigger;


}
