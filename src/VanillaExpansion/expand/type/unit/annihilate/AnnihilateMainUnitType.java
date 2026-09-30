package VanillaExpansion.expand.type.unit.annihilate;

import arc.Core;
import arc.graphics.g2d.TextureRegion;

public class AnnihilateMainUnitType extends AnnihilatePartUnitType{
    public AnnihilateMainUnitType(String name) {
        super(name);
        constructor = AnnihilateMainUnit::create;
        drawMinimap = true;
        hidden = false;
        isEnemy = true;
        chargeRingRadius = 200f;
        chargeRingStroke = 24f;
        speed = 4f;
        drag = 0.04f;
        accel = 0.2f;
    }

    public String ringSprite = "ve-annihilate-ring-region";
    public String ringTopSprite = "ve-annihilate-ring-top-region";

    public TextureRegion ringRegion = new TextureRegion();
    public TextureRegion ringTopRegion = new TextureRegion();

    @Override
    public void load(){
        super.load();
        ringRegion = Core.atlas.find(ringSprite);
        ringTopRegion = Core.atlas.find(ringTopSprite);
    }


}
