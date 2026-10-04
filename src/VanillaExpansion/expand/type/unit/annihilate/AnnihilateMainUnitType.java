package VanillaExpansion.expand.type.unit.annihilate;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.g2d.TextureRegion;

public class AnnihilateMainUnitType extends AnnihilatePartUnitType{
    public AnnihilateMainUnitType(String name) {
        super(name);
        constructor = AnnihilateMainUnit::create;
        drawMinimap = true;
        hidden = false;
        isEnemy = true;
        chargeRingRadius = 200f;
        chargeRingStroke = 48f;
        speed = 4f;
        drag = 0.04f;
        accel = 0.2f;
        playerControllable = true;
        physics = false;
    }

    @Override
    public float estimateDps(){
        return Float.MAX_VALUE;
    }

    public String ringSprite = "ve-annihilate-ring-region";
    public String ringTopSprite = "ve-annihilate-ring-top-region";

    public String emojiBaseSprite = "ve-annihilate-emoji-base";
    public String emojiNormalSprite = "ve-annihilate-emoji-normal";
    public String emojiIdleSprite = "ve-annihilate-emoji-idle";
    public String emojiPauseSprite = "ve-annihilate-emoji-pause";
    public String emojiPuzzledSprite = "ve-annihilate-emoji-puzzled";
    public String emojiHurtSprite = "ve-annihilate-emoji-hurt";

    public TextureRegion ringRegion = new TextureRegion();
    public TextureRegion ringTopRegion = new TextureRegion();

    public TextureRegion emojiBaseRegion = new TextureRegion();
    public TextureRegion emojiNormalRegion = new TextureRegion();
    public TextureRegion emojiIdleRegion = new TextureRegion();
    public TextureRegion emojiPauseRegion = new TextureRegion();
    public TextureRegion emojiPuzzledRegion = new TextureRegion();
    public TextureRegion emojiHurtRegion = new TextureRegion();

    public Sound chargeSound1;

    @Override
    public void load(){
        super.load();
        ringRegion = Core.atlas.find(ringSprite);
        ringTopRegion = Core.atlas.find(ringTopSprite);
        emojiBaseRegion = Core.atlas.find(emojiBaseSprite);
        emojiNormalRegion = Core.atlas.find(emojiNormalSprite);
        emojiIdleRegion = Core.atlas.find(emojiIdleSprite);
        emojiPauseRegion = Core.atlas.find(emojiPauseSprite);
        emojiPuzzledRegion = Core.atlas.find(emojiPuzzledSprite);
        emojiHurtRegion = Core.atlas.find(emojiHurtSprite);
    }


}
