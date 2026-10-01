package VanillaExpansion.expand.type.unit.annihilate;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import mindustry.ai.types.CommandAI;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

public class AnnihilatePartUnitType extends UnitType {
    public AnnihilatePartUnitType(String name) {
        super(name);
        constructor = AnnihilatePartUnit::create;
        drawBody = drawCell = drawSoftShadow = drawMinimap = false;
        engineSize = 0;
        hidden = true;
        wobble = isEnemy = false;
        playerControllable = false;
        controller = u -> new CommandAI();
        flying = true;
        clipSize = 8000f;
    }

    public String sprite = "error";
    public String armSprite1 = "error";
    public String armSprite2 = "error";
    public float spriteX = 0;
    public float spriteY = 0;
    public float defaultElevation = 2f;
    public float chargeRingRadius = 80f;
    public float chargeRingStroke = 16f;

    public TextureRegion partRegion = new TextureRegion();
    public TextureRegion armRegion1 = new TextureRegion();
    public TextureRegion armRegion2 = new TextureRegion();

    @Override
    public void load(){
        super.load();
        partRegion = Core.atlas.find(sprite);
        armRegion1 = Core.atlas.find(armSprite1);
        armRegion2 = Core.atlas.find(armSprite2);
    }



}
