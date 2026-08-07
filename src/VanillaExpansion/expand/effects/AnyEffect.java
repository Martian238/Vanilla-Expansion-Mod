package VanillaExpansion.expand.effects;

import mindustry.entities.Effect;
import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.entities.bullet.BulletType;

public class AnyEffect extends Effect {
    public Sound sound = Sounds.none;
    public float minPitch = 1.0f;
    public float maxPitch = 1.0f;
    public float minVolume = 1f;
    public float maxVolume = 1f;
    public @Nullable Bullet bullet;
    public float shake = 0f;
    public float shakeDuration = 0f;
    public Effect effect;

    public AnyEffect(){
        startDelay = -1;
    }



    @Override
    public void init(){
        if(startDelay < 0){
            startDelay = effect.startDelay;
        }
        if(shakeDuration <= 0){
            shakeDuration = shake;
        }
    }




    @Override
    public void create(float x, float y, float rotation, Color color, Object data){
        if(!shouldCreate()) return;

        if(startDelay > 0){
            Time.run(startDelay, () ->
                    sound.at(x, y, Mathf.random(minPitch, maxPitch), Mathf.random(minVolume, maxVolume))
            );
        }else{
            sound.at(x, y, Mathf.random(minPitch, maxPitch), Mathf.random(minVolume, maxVolume));
            if(bullet != null){
                //TODO add bullet spawning
            }
        }

        effect.create(x, y, rotation, color, data);
    }

}
