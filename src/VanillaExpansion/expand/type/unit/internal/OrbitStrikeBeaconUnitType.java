package VanillaExpansion.expand.type.unit.internal;

import arc.audio.Sound;
import mindustry.gen.Sounds;
import mindustry.type.UnitType;

public class OrbitStrikeBeaconUnitType extends UnitType {
    public OrbitStrikeBeaconUnitType(String name) {
        super(name);
        constructor = OrbitStrikeBeaconUnit::new;
    }

    public Sound drownSound = Sounds.none;
    public Sound activateSound = Sounds.none;
    public float activateTime = 60f * 120f;
}
