package VanillaExpansion.expand.effects;

import arc.math.geom.Geometry;
import mindustry.entities.Effect;
import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.ParticleEffect;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.entities.bullet.BulletType;

public class SkyLightningEffect extends Effect{

    public float pointClip = 2000f;
    public Color color1 = Color.valueOf("8aa3f4aa");
    public Color color2 = Color.valueOf("8aa3f400");
    public Color color3 = Color.valueOf("ffffffff");
    public float pointWidth = 7f;
    public float pointLife = 62f;

    public int segments = 10;
    public float segmentLengthMax = 512f;
    public float segmentLengthMin = 512f * 0.2f;
    public float generalRotation = 90f;
    public float segmentRotationRange = 30f;
    public float segmentDelay = 0f;
    public float segmentLengthEnd = 1024f;

    public float twistChance = 0f;
    public boolean shorterTwist = true;
    public float shortestTwistRange = 75f;

    public float shake = 0f;
    public float shakeDuration = 40f;

    public boolean surge = false;

    public SkyLightningEffect(){
        clip = pointClip;
    }

    @Override
    public void create(float x, float y, float rotation, Color color, Object data){
        float nx = x;
        float ny = y;
        float px = x;
        float py = y;
        float delay = segmentDelay * (segments - 1);
        float nr = generalRotation;
        float rrand = segmentRotationRange;
        float d = segmentLengthMax;
        float dr = 0f;
        float br = 0f;
        float b = 0f;
        if(surge){
            color1 = Color.valueOf("f3e979aa");
            color2 = Color.valueOf("f3e97900");
        }
        Effect.shake(shake, shakeDuration, x, y);
        for(int i = 0; i < segments; i++) {
            Effect segPointEffect = new SkyLightningPointEffect(pointClip, color1, color2, color3, pointWidth, pointLife, delay);
            d = Math.abs(Mathf.range(segmentLengthMin, segmentLengthMax));
            if(i == segments - 1) {
                d = segmentLengthEnd;
            }
            if(shorterTwist && i != segments - 1) {
                rrand = (shortestTwistRange - segmentRotationRange) * ((d - segmentLengthMin) / (segmentLengthMax - segmentLengthMin)) + segmentRotationRange;
            }else{
                rrand = segmentRotationRange;
            }
            dr = Mathf.range(rrand) + generalRotation;
            if(twistChance > 0f && Mathf.chance(twistChance)){
                br = nr - generalRotation;
                b = Mathf.sinDeg(br);
                if(b > 0){
                    dr = Mathf.range(generalRotation - rrand, generalRotation);
                }else if(b < 0){
                    dr = Mathf.range(generalRotation, generalRotation + rrand);
                }
            }
            nx = px;
            ny = py;
            px = px + (d * Mathf.cosDeg(dr));
            py = py + (d * Mathf.sinDeg(dr));
            Geometry.iterateLine(0f, nx, ny, px, py, 1, (ex, ey) -> {
                segPointEffect.at(ex, ey, 0f);
            });
            delay = delay - segmentDelay;
            nr = dr;
        }
    }
}
