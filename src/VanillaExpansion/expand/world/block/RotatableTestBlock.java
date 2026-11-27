package VanillaExpansion.expand.world.block;

public class RotatableTestBlock extends RotatableBlock {

    public RotatableTestBlock(String name) {
        super(name);
        rotate = true;
        autoRotate = true;
        showConfigUI = true;
        solid = true;
        destructible = true;
        health = 200;
    }

    public class RotatableTestBuild extends RotatableBuild {
    }
}