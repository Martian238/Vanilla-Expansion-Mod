package VanillaExpansion.entities;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import VanillaExpansion.*;
import VanillaExpansion.effects.*;
import VanillaExpansion.effects.Fragmentation.*;
import VanillaExpansion.expand.world.block.power.*;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Fires;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.Tile;
import static mindustry.Vars.*;
public class RBMKDebrisEntity extends FragmentEntity{
    public RBMKBase.DebrisType type = RBMKBase.DebrisType.BLANK;
    public Team team = Team.derelict;
    public float damage;
    public boolean radiate, annihilate;
    public boolean burning;
    protected boolean destroyed = false;
    protected boolean landed = false;
    public static RBMKDebrisEntity create(float x, float y, RBMKBase.DebrisType type, TextureRegion region){
        RBMKDebrisEntity d = new RBMKDebrisEntity();
        d.type = type;
        d.team = Team.derelict;
        d.x = x;
        d.y = y;
        d.rotation = Mathf.random(360f);
        d.burning = Mathf.chance(0.6f);
        d.smokeScale = 1f / 3f;
        Fragmentation frag = new Fragmentation(region, 3, 3, 1);
        frag.drawWidth = d.sizeOf(type);
        frag.drawHeight = frag.drawWidth;
        frag.layer = Layer.flyingUnitLow;
        frag.shadowElevation = 0.5f;
        d.main = frag;
        d.island = 0;
        Vec3 pos = frag.getOffset(frag.islands.get(0));
        d.offsetX = pos.x;
        d.offsetY = pos.y;
        d.boundSize = pos.z * Math.min(Math.abs(frag.drawWidth), Math.abs(frag.drawHeight));
        d.calculateArea();
        d.vx = 8f / 16f * (Mathf.range(3.2f) + Mathf.random(1f) * Mathf.sign(Mathf.random(1f)));
        d.vy = 8f / 16f * (Mathf.range(3.2f) + Mathf.random(1f) * Mathf.sign(Mathf.random(1f)));
        d.vz = 8f * (2f + Mathf.random(4f));
        d.vr = 8f * Mathf.range(5f, 12f);
        switch(type){
            case FUEL -> {
                d.damage = 1.5f;
                d.radiate = true;
                d.lifetime = 600f;
            }
            case GRAPHITE -> {
                d.damage = 0.6f;
                d.radiate = true;
                d.lifetime = 900f;
            }
            case ROD -> {
                d.lifetime = 360f;
            }
            case ELEMENT -> {
                d.lifetime = 180f;
            }
            case LID -> {
                d.annihilate = true;
                d.vx *= 0.5f;
                d.vy *= 0.5f;
                d.vz += 2f;
                d.lifetime = 120f;
            }
            default -> { // BLANK
                d.lifetime = 180f;
            }
        }
        if(RBMKDials.enablePermaScrap) d.lifetime = 1_000_000f;
        d.customUpdate = e -> {
            if(e.impact && !d.landed){
                d.landed = true;
                e.lifetime = e.time + 20f * 60f;
                if(d.burning){
                    Tile ground = world.tileWorld(e.x, e.y);
                    if(ground != null && ground.block().isAir()){
                        Fires.create(ground);
                    }
                }
            }
            if(!net.client() && d.radiate){
                Utils.scanEnemies(d.team, e.x, e.y, 24f, true, true, t -> {
                    if(t instanceof Healthc h){
                        h.damage(d.damage * Time.delta);
                    }
                });
            }
            if(d.annihilate && !d.destroyed && (e.impact || (e.vz > 0f && e.onSolid()))){
                d.destroyed = true;
                d.annihilateArea();
            }
        };
        d.add();
        return d;
    }
    @Override
    public void update(){
        vz -= 15f * 0.01f;
        if(burning && Mathf.chance(0.8f)){
            Fx.fire.at(x + Mathf.range(6f), y + Mathf.range(6f), Mathf.random(4f, 8f));
        }
        super.update();
    }
    float sizeOf(RBMKBase.DebrisType type){
        return switch(type){
            case FUEL, GRAPHITE -> 4f;
            case ROD -> 5f;
            case ELEMENT -> 7f;
            case LID -> 8f;
            default -> 5f;
        };
    }
    protected void annihilateArea(){
        if(net.client()) return;
        Effect.shake(6f, 6f, x, y);
        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
                Tile tile = world.tile(tileX() + dx, tileY() + dy);
                if(tile == null) continue;
                if(tile.build != null){
                    tile.build.kill();
                }else if(!tile.block().isAir() && tile.block().destructible){
                    tile.setBlock(Blocks.air);
                }
            }
        }
    }
}
