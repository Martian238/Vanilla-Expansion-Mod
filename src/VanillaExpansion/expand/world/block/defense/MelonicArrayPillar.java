package VanillaExpansion.expand.world.block.defense;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.PowerTurret;

import java.util.Objects;

import static mindustry.Vars.tilesize;

public class MelonicArrayPillar extends PowerTurret {
    public MelonicArrayPillar(String name) {
        super(name);
        solid = false;
    }

    public String shadowName = "ve-melonic-array-pillar-shadowpillar";
    public TextureRegion shadowRegionPillar;

    public float minDistance = 12f * tilesize;

    public boolean blockAir = true;

    protected static MelonicArrayPillarBuild paramBuild;
    protected static MelonicArrayPillar paramBlock;

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(!Groups.build.contains(b -> Objects.equals(b.block.name, this.name))){
            return true;
        }
        float offset = 0;
        if(size % 2 == 0){
            offset = 0.5f * tilesize;
        }
        boolean bo = false;
        for(Building b : Groups.build){
            if(Objects.equals(b.block.name, this.name)){
                Draw.z(Layer.buildBeam);
                Draw.color(Color.valueOf("e44646"));
                Draw.alpha(0.5f);
                Lines.stroke(2f);
                Lines.circle(b.tile.worldx() + offset, b.tile.worldy() + offset, minDistance - tilesize / 2f);
                if(getDistance(b.tile.worldx(), b.tile.worldy(), tile.worldx(), tile.worldy()) < minDistance){
                    Lines.stroke(2f);
                    Lines.line(tile.worldx() + offset, tile.worldy() + offset, b.tile.worldx() + offset, b.tile.worldy() + offset);
                    bo = true;
                }
            }
        }
        return !bo;
    }

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }

    protected static final Cons<Bullet> bulletConsumer = bullet -> {
        if(bullet.team != paramBuild.team && bullet.within(paramBuild, paramBlock.size * tilesize/2f)){
            paramBuild.damage(bullet.team, bullet.damage() * bullet.buildingDamageMultiplier());
            bullet.absorb();
        }
    };

    protected static final Cons<Unit> unitConsumer = unit -> {
        if(unit.isFlying() && paramBlock.blockAir) {
            float overlapDst = (unit.hitSize / 2f + paramBlock.size * tilesize / 2f) - unit.dst(paramBuild);

            if (overlapDst > 0) {
                if (overlapDst > unit.hitSize * 1.5f) {
                    if(unit.team != paramBuild.team) {
                        unit.kill();
                    }else{
                        unit.x(unit.x + paramBlock.size * tilesize / 2f * Mathf.cosDeg(unit.rotation));
                        unit.y(unit.y + paramBlock.size * tilesize / 2f * Mathf.sinDeg(unit.rotation));
                    }
                } else {
                    //stop
                    unit.vel.setZero();
                    //get out
                    unit.move(Tmp.v1.set(unit).sub(paramBuild).setLength(overlapDst + 0.01f));

                    if (Mathf.chanceDelta(0.12f * Time.delta)) {
                        Fx.circleColorSpark.at(unit.x, unit.y, Color.valueOf("e44646"));
                    }
                }
            }
        }
    };

    @Override
    public void load(){
        super.load();
        shadowRegionPillar = Core.atlas.find(shadowName);
    }


    public class MelonicArrayPillarBuild extends PowerTurretBuild {
        @Override
        public void updateTile(){
            super.updateTile();

                paramBuild = this;
                paramBlock = (MelonicArrayPillar) this.block;
                float rad = paramBlock.size * tilesize;
                //paramEffect = absorbEffect;
                Groups.bullet.intersect(x - rad, y - rad, rad * 2f, rad * 2f, bulletConsumer);
                Units.nearby(team, x, y, rad + 10f, unitConsumer);

            Draw.z(Layer.flyingUnitLow - 1f);
            float elevation = 40f;
            if(shadowRegionPillar != null) Drawf.shadow(shadowRegionPillar, x - elevation, y - elevation, 0f);
        }
    }
}
