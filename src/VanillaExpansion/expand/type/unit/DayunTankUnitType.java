package VanillaExpansion.expand.type.unit;

import arc.audio.Sound;
import mindustry.gen.Sounds;
import mindustry.type.unit.TankUnitType;

public class DayunTankUnitType extends TankUnitType {
    public DayunTankUnitType(String name){
        super(name);
        constructor = DayunTankUnit::create;
    }

    public Sound truckMusic = Sounds.none;
    public float truckMusicVolume = 2f;
}
