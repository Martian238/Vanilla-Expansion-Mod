package VanillaExpansion.expand.type.unit;

import arc.audio.Sound;
import mindustry.gen.Sounds;
import mindustry.type.UnitType;
import mindustry.world.meta.Env;

public class IronGolemType extends UnitType {
    public IronGolemType(String name) {
        super(name);
        envDisabled = Env.none;
    }

    public float invincibleTime;
    public Sound hurtSound = Sounds.none;
    public float hurtSoundRange = 0.1f;
    public float hurtSoundVolume = 0.5f;
    public float selfHealAmount = 200f;
    public Sound healSound = Sounds.none;
    public float healSoundRange = 0.1f;
    public float healSoundVolume = 0.5f;
}
