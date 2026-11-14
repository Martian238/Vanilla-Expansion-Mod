package VanillaExpansion.expand.abilities;

import VanillaExpansion.VESettings;
import arc.Core;
import arc.audio.Sound;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;

import static mindustry.Vars.control;

public class PropedroneLoopSoundAbility extends Ability {
    public PropedroneLoopSoundAbility(){
        display = false;
    }

    public Sound loopSound = Sounds.none;
    public float loopSoundVolume = 0.1f;

    public boolean toilet = false;

    @Override
    public void update(Unit u){
        if (Vars.headless || Vars.control == null) return;
        if(!toilet) {
            if (!VESettings.getBool("ve-propedrone-quiet")) {
                control.sound.loop(loopSound, new Vec2(u.x(), u.y()), loopSoundVolume);
            }
        }else{
            if(u.stack() != null && u.stack().amount > 0 && u.stack().item == Items.metaglass){
                control.sound.loop(loopSound, new Vec2(u.x(), u.y()), loopSoundVolume);
                u.rotation(u.rotation() - 15f * Time.delta);
            }
        }
    }
}
