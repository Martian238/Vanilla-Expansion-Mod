package VanillaExpansion.control;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.util.*;

/** A simple class for playing a looping sound at a position.*/
public class VESoundLoop{
    private static final float fadeSpeed = 10000f; //GO DIE!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private final Sound sound;
    private int id = -1;
    private float volume, baseVolume;

    public VESoundLoop(Sound sound, float baseVolume){
        this.sound = sound;
        this.baseVolume = baseVolume;
    }

    public void update(float x, float y, boolean play){
        update(x, y, play, 1f);
    }

    public void update(float x, float y, boolean play, float volumeScl){
        if(baseVolume <= 0) return;

        if(id < 0){
            if(play){
                id = sound.loop(sound.calcVolume(x, y) * volume * baseVolume * volumeScl, 1f, sound.calcPan(x, y));
            }
        }else{
            //fade the sound in or out
            if(play){
                volume = Mathf.clamp(volume + fadeSpeed * Time.delta);
            }else{
                volume = Mathf.clamp(volume - fadeSpeed * Time.delta);
                if(volume <= 0.001f){
                    Core.audio.stop(id);
                    id = -1;
                    return;
                }
            }

            Core.audio.set(id, sound.calcPan(x, y), sound.calcVolume(x, y) * volume * baseVolume * volumeScl);
        }
    }

    public void stop(){
        if(id != -1){
            Core.audio.stop(id);
            id = -1;
            volume = 0f;
        }
    }
}

