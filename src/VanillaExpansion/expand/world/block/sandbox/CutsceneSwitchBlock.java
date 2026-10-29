package VanillaExpansion.expand.world.block.sandbox;

import VanillaExpansion.VESettings;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.BuildVisibility;

import static mindustry.Vars.state;

public class CutsceneSwitchBlock extends Block {
    public CutsceneSwitchBlock(String name) {
        super(name);
        noUpdateDisabled = false;
        forceDark = true;
        placeableLiquid = true;
        buildVisibility = BuildVisibility.sandboxOnly;
        privileged = true;
        category = Category.logic;
        update = true;
        solid = true;
        targetable = false;
        canOverdrive = false;
        size = 1;
        underBullets = true;
        rebuildable = false;
        createRubble = false;
        drawCracks = false;
        destructible = false;
    }

    @Override
    public boolean checkForceDark(Tile tile){
        return !accessible();
    }
    public boolean accessible(){
        return !privileged || state.rules.editor || state.playtestingMap != null || state.rules.allowEditWorldProcessors;
    }

    public class CutsceneSwitchBuild extends Building {

        @Override
        public boolean collide(Bullet other){
            return !privileged || destructible;
        }
        @Override
        public void damage(float damage){
            if(!privileged || destructible){
                super.damage(damage);
            }
        }
        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public void drawStatus(){
            if(Vars.state.isEditor() || accessible()){
                float m = block.size > 1 ? 1f : 0.64f;
                float bx = x + block.size*8f/2f - 8f*m/2f, by = y - block.size*8f/2f + 8f*m/2f;
                Draw.z(71f); Draw.color(Pal.gray); Fill.square(bx, by, 2.5f*m, 45f);
                Draw.color(status().color); Fill.square(bx, by, 1.5f*m, 45f); Draw.color();
            }
        }

        @Override
        public BlockStatus status(){
            return VESettings.getBool("ve-skip-cutscene") ? BlockStatus.noInput : BlockStatus.active;
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y, drawrot());
            drawStatus();
        }

        @Override
        public void updateTile(){
            enabled = !VESettings.getBool("ve-skip-cutscene");
        }
    }
}
