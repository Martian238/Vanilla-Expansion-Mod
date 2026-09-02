package VanillaExpansion.expand.effects;

import arc.graphics.Color;
import arc.math.Interp;
import mindustry.entities.Effect;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.ParticleEffect;

public class SkyLightningPointEffect extends Effect {

    public float pointClip = 2000f;
    public Color color1 = Color.valueOf("8aa3f4aa");
    public Color color2 = Color.valueOf("8aa3f400");
    public Color color3 = Color.valueOf("ffffffff");
    public float pointWidth = 7f;
    public float pointLife = 62f;
    public float pointStartDelay = -1;

    public Effect pointEffect = new MultiEffect(
            new ParticleEffect(){{
                particles = 1; clip = pointClip; length = 0f; interp = Interp.linear;
                sizeInterp = Interp.circleOut; colorFrom = color1; colorTo = color2;
                sizeFrom = pointWidth; sizeTo = 0f; lifetime = pointLife; layer = 109f;
                startDelay = pointStartDelay;
            }},
            new ParticleEffect(){{
                particles = 1; clip = pointClip; length = 0f; interp = Interp.linear;
                sizeInterp = Interp.circleOut; colorFrom = color3; colorTo = color3;
                sizeFrom = pointWidth * 0.3f; sizeTo = 0f; lifetime = pointLife * 1.07f; layer = 109.1f;
                startDelay = pointStartDelay;
            }}
    );
    public SkyLightningPointEffect() {
        clip = pointClip;
    }
    public SkyLightningPointEffect(float pointClip, Color color1, Color color2, Color color3, float pointWidth, float pointLife, float pointStartDelay) {
        this.pointClip = pointClip;
        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
        this.pointWidth = pointWidth;
        this.pointLife = pointLife;
        this.pointStartDelay = pointStartDelay;
        pointEffect = new MultiEffect(
                new ParticleEffect(){{
                    particles = 1; clip = pointClip; length = 0f; interp = Interp.linear;
                    sizeInterp = Interp.circleOut; colorFrom = color1; colorTo = color2;
                    sizeFrom = pointWidth; sizeTo = 0f; lifetime = pointLife; layer = 109f;
                    startDelay = pointStartDelay;
                }},
                new ParticleEffect(){{
                    particles = 1; clip = pointClip; length = 0f; interp = Interp.linear;
                    sizeInterp = Interp.circleOut; colorFrom = color3; colorTo = color3;
                    sizeFrom = pointWidth * 0.3f; sizeTo = 0f; lifetime = pointLife * 1.07f; layer = 109.1f;
                    startDelay = pointStartDelay;
                }}
        );
        clip = pointClip;
    }

    @Override
    public void create(float x, float y, float rotation, Color color, Object data){
        if(!shouldCreate()) return;
        pointEffect.create(x, y, rotation, color, data);
    }
}
