package VanillaExpansion.expand.world.block.production;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.logic.LAccess;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.Pump;

import static mindustry.Vars.*;

public class GasPump extends Pump {
    public GasPump(String name) {
        super(name);
        liquidCapacity = 300;
        squareSprite = false;
    }

    public boolean banLiquid = true;
    public boolean ignoreMultiplier = true;
    public int leastTiles = 9;

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(isMultiblock()){
            Liquid last = null;
            int num = 0;
            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(other.floor().liquidDrop == null) continue;
                if(other.floor().liquidDrop != last && last != null) return false;
                last = other.floor().liquidDrop;
                if(last != null) {
                    if (!banLiquid || last.gas) {
                        num++;
                    }else{
                        last = null;
                    }
                }
            }
            if(leastTiles > 0 && num < leastTiles){
                return false;
            }
            return last != null;
        }else{
            return canPump(tile);
        }
    }

    @Override
    protected boolean canPump(Tile tile){
        if(tile != null && tile.floor().liquidDrop != null) {
            if (banLiquid && !tile.floor().liquidDrop.gas) {
                return false;
            }
        }
        return tile != null && tile.floor().liquidDrop != null;
    }

    @Override
    public void setBars(){
        super.setBars();

        //replace dynamic output bar with own custom bar
        addLiquidBar((GasPumpBuild build) -> build.liquidDrop);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPotentialLinks(x, y);
        drawOverlay(x * tilesize + offset, y * tilesize + offset, rotation);

        Tile tile = world.tile(x, y);

        if(valid && tile != null){
            float amount = 0f;
            Liquid liquidDrop = null;

            for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
                if(canPump(other)){
                    if(liquidDrop != null && other.floor().liquidDrop != liquidDrop){
                        liquidDrop = null;
                        break;
                    }
                    liquidDrop = other.floor().liquidDrop;
                    if(!ignoreMultiplier) {
                        amount += other.floor().liquidMultiplier;
                    }else{
                        amount += 1f;
                    }
                }
            }

            if(liquidDrop != null){
                float width = drawPlaceText(Core.bundle.formatFloat("bar.pumpspeed", amount * pumpAmount * 60f, 0), x, y, valid);
                float dx = x * tilesize + offset - width/2f - 4f, dy = y * tilesize + offset + size * tilesize / 2f + 5, s = iconSmall / 4f;
                float ratio = (float)liquidDrop.fullIcon.width / liquidDrop.fullIcon.height;
                Draw.mixcol(Color.darkGray, 1f);
                Draw.rect(liquidDrop.fullIcon, dx, dy - 1, s * ratio, s);
                Draw.reset();
                Draw.rect(liquidDrop.fullIcon, dx, dy, s * ratio, s);
            }
        }
    }

    public class GasPumpBuild extends LiquidBuild{
        public float warmup, totalProgress;
        public float consTimer;
        public float amount = 0f;
        public @Nullable Liquid liquidDrop = null;

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public void pickedUp(){
            amount = 0f;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.efficiency) return shouldConsume() ? efficiency : 0f;
            if(sensor == LAccess.totalLiquids) return liquidDrop == null ? 0f : liquids.get(liquidDrop);
            return super.sense(sensor);
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            amount = 0f;
            liquidDrop = null;
            int num = 0;

            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(canPump(other)){
                    liquidDrop = other.floor().liquidDrop;
                    if(!ignoreMultiplier) {
                        amount += other.floor().liquidMultiplier;
                        if(other.floor().liquidMultiplier > 0){
                            num++;
                        }
                    }else{
                        amount += 1f;
                        num++;
                    }
                }
            }
            if(leastTiles > 0 && num < leastTiles){
                amount = 0f;
            }
        }

        @Override
        public boolean shouldConsume(){
            return liquidDrop != null && liquids.get(liquidDrop) < liquidCapacity - 0.01f && enabled;
        }

        @Override
        public void updateTile(){
            if(efficiency > 0 && liquidDrop != null){
                float maxPump = Math.min(liquidCapacity - liquids.get(liquidDrop), amount * pumpAmount * edelta());
                liquids.add(liquidDrop, maxPump);

                //does nothing for most pumps, as those do not require items.
                if((consTimer += delta()) >= consumeTime){
                    consume();
                    consTimer %= 1f;
                }

                warmup = Mathf.approachDelta(warmup, maxPump > 0.001f ? 1f : 0f, warmupSpeed);
            }else{
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
            }

            totalProgress += warmup * Time.delta;

            if(liquidDrop != null){
                dumpLiquid(liquidDrop);
            }
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float progress(){
            return Mathf.clamp(consTimer / consumeTime);
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }
    }
}
