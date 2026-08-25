package VanillaExpansion.expand.abilities;

import arc.audio.Sound;
import arc.struct.Seq;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Sounds;

public class ZenithAIAbility extends Ability {
    public ZenithAIAbility(){}

    public Seq<String> zenithUnits = new Seq<>();
    public Sound talkSoundNormal = Sounds.none;
    public Sound talkSoundQuestion = Sounds.none;
    public Sound talkSoundUrgent = Sounds.none;

    //TODO
}
