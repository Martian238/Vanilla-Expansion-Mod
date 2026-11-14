package VanillaExpansion.expand.type.unit.internal.chargeskill;

import VanillaExpansion.content.CustomFx;
import arc.Core;
import arc.audio.Sound;
import arc.graphics.g2d.TextureRegion;
import mindustry.entities.Effect;
import mindustry.gen.Sounds;
import mindustry.type.UnitType;

public class ChargeSkillUnitType extends UnitType {
    public ChargeSkillUnitType(String name) {
        super(name);
    }

    public Sound startChargeSound = Sounds.none;
    public float interruptShake = 8f;
    public Effect interruptEffect = CustomFx.annihilateInterrupt;
    public Effect chargeStopEffect = CustomFx.annihilateChargeStop;
    public Sound interruptSound = Sounds.none;

    public Sound newspaperRipSound = Sounds.none;
    public Sound newspaperAngrySound = Sounds.none;

    public String newspaper = "ve-reign-news";
    public TextureRegion newspaperRegion = new TextureRegion();

    @Override
    public void load(){
        super.load();
        newspaperRegion = Core.atlas.find(newspaper);
    }
}
