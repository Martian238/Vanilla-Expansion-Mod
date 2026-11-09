package VanillaExpansion.expand.world.block.mechanism;

import VanillaExpansion.content.VEJSUnitTypes;
import VanillaExpansion.expand.world.block.crux.LargeTeamProjector;
import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.type.Category;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BuildVisibility;

import static mindustry.Vars.*;
import static mindustry.Vars.state;

public class OrbitStrikeBeaconSpawner extends Block {
    public OrbitStrikeBeaconSpawner(String name) {
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

    public float interval = 90f * 60f;
    public float intervalRand = 30f * 60f;
    public int checkBuildingRange = 15;
    public float checkProjectorInterval = 300f;
    public int projectorRange = 19;

    @Override
    public boolean checkForceDark(Tile tile){
        return !accessible();
    }
    public boolean accessible(){
        return !privileged || state.rules.editor || state.playtestingMap != null || state.rules.allowEditWorldProcessors;
    }

    public class OrbitStrikeBeaconSpawnerBuild extends Building {

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

        private float timer = 0;
        private float time = 0;
        private float checkTimer = 0;
        private boolean hasProjector = false;
        private Seq<Vec2> projectors = new Seq<>();
        private float speed = 1;

        @Override
        public void updateTile(){
            if(!enabled || (state.isCampaign() && ((!state.rules.attackMode && state.enemies > 0 && state.wave < 30) || (state.getSector() != null && state.getSector().isCaptured())))) return;
            if(time <= 0){
                time = interval + Mathf.range(intervalRand);
            }
            checkTimer += Time.delta;
            if(checkTimer >= checkProjectorInterval){
                checkTimer = 0;
                hasProjector = false;
                speed = 1;
                projectors.clear();
                for(Building b : Groups.build){
                    if(b.team() != team() && b instanceof LargeTeamProjector.LargeTeamProjectorBuild && b.efficiency > 0.9f){
                        hasProjector = true;
                        projectors.add(new Vec2(b.x(), b.y()));
                        if(speed < 4){
                            speed += Math.min(0.25f, (speed * 4f + 4f) / 5 - speed);
                        }
                    }
                }
            }
            timer += Time.delta * speed;
            if(timer >= time){
                time = interval + Mathf.range(intervalRand);
                timer = 0;
                spawnBeacon();
            }
        }

        public void spawnBeacon(){
            if(!hasProjector || Mathf.chance((double) 1 / Math.max(2, projectors.size))){
                for(int i = 0; i < 200; i++){
                    float rx = Math.abs(Mathf.range(state.map.width * tilesize));
                    float ry = Math.abs(Mathf.range(state.map.height * tilesize));
                    Floor floor = world.floorWorld(rx, ry);
                    if(floor == null || floor.isDeep() || floor.damageTaken > 0)continue;
                    Tile tile = world.tileWorld(rx, ry);
                    if(tile != null && ((tile.block() != null && tile.block() == Blocks.air)
                            || (tile.build != null && !tile.build.block.solid))) {
                        if(tile.block() == Blocks.air) {
                            boolean hasNearbyBuilding = false;
                            for (int ix = 0; ix < checkBuildingRange; ix++) {
                                for (int iy = 0; iy < checkBuildingRange; iy++) {
                                    float tx = rx + (ix - Mathf.floor(checkBuildingRange / 2f)) * tilesize;
                                    float ty = ry + (iy - Mathf.floor(checkBuildingRange / 2f)) * tilesize;
                                    Tile tileNearby = world.tileWorld(tx, ty);
                                    if(tileNearby != null && tileNearby.build != null && tileNearby.build.team() != team() && (tileNearby.build.efficiency >= 0.1f || tileNearby.build instanceof CoreBlock.CoreBuild || tileNearby.build instanceof BaseTurret.BaseTurretBuild)){
                                        hasNearbyBuilding = true;
                                        break;
                                    }
                                }
                                if(hasNearbyBuilding) break;
                            }
                            if(hasNearbyBuilding){
                                spawnBeacon(rx, ry);
                                break;
                            }
                        }else{
                            spawnBeacon(rx, ry);
                            break;
                        }
                    }
                }
            }else{
                int rand = Math.abs(Mathf.random(projectors.size - 1));
                Vec2 pos = projectors.get(rand);
                if(pos == null) return;
                float rx = pos.x;
                float ry = pos.y;
                for(int i = 0; i < 20; i++){
                    float tx = rx + Mathf.range(projectorRange * tilesize);
                    float ty = ry + Mathf.range(projectorRange * tilesize);
                    Floor floor = world.floorWorld(tx, ty);
                    if(floor.isDeep() || floor.damageTaken > 0) continue;
                    Tile tile = world.tileWorld(tx, ty);
                    if(tile != null && ((tile.block() != null && tile.block() == Blocks.air)
                            || (tile.build != null && !tile.build.block.solid))) {
                        spawnBeacon(tx, ty);
                        break;
                    }
                }
            }
        }
        public void spawnBeacon(float x, float y){
            Unit u = VEJSUnitTypes.orbitStrikeBeacon.create(team());
            u.set(x, y);
            Events.fire(new EventType.UnitCreateEvent(u, this, null));
            if(!net.client()){
                u.add();
                Units.notifyUnitSpawn(u);
            }
            Fx.unitSpawn.at(x, y);
            Sounds.payloadDrop1.at(x, y);
            Time.run(30f, () -> {
                if(!u.dead() && !headless
                        && !(Math.abs(u.x - Core.camera.position.x) < Core.camera.width / 4f
                        && Math.abs(u.y - Core.camera.position.y) < Core.camera.height / 4f)) {
                    String key = "orbitstrikebeacon.spawn";
                    if(Vars.player != null) {
                        float angle = Angles.angle(u.x - Core.camera.position.x, u.y - Core.camera.position.y);
                        if(angle > 180) angle -= 360;
                        if(angle <= 22.5 && angle > -22.5) key = "orbitstrikebeacon.spawn.e";
                        else if(angle <= 67.5 && angle > 22.5) key = "orbitstrikebeacon.spawn.ne";
                        else if(angle <= 112.5 && angle > 67.5) key = "orbitstrikebeacon.spawn.n";
                        else if(angle <= 157.5 && angle > 112.5) key = "orbitstrikebeacon.spawn.nw";
                        else if(angle <= -157.5 || angle > 157.5) key = "orbitstrikebeacon.spawn.w";
                        else if(angle <= -112.5 && angle > -157.5) key = "orbitstrikebeacon.spawn.sw";
                        else if(angle <= -67.5 && angle > -112.5) key = "orbitstrikebeacon.spawn.s";
                        else key = "orbitstrikebeacon.spawn.se";
                        if(!Vars.player.team().cores().isEmpty()){
                            for(CoreBlock.CoreBuild c : Vars.player.team().cores()){
                                if(Mathf.dst2(c.x, c.y, u.x, u.y) <= 20 * 20 * tilesize * tilesize){
                                    key = "orbitstrikebeacon.spawn.core";
                                    break;
                                }
                            }
                        }
                    }
                    ui.announce(Core.bundle.format(key,
                            Mathf.floor(u.x / tilesize), Mathf.floor(u.y / tilesize)), 6f);
                    Log.info(Core.bundle.format(key,
                            Mathf.floor(u.x / tilesize), Mathf.floor(u.y / tilesize)));
                }
            });
        }

        @Override
        public void write(Writes w){
            w.f(timer);
            w.i(Mathf.floor(time));
        }

        @Override
        public void read(Reads r, byte revision){
            timer = r.f();
            time = r.i();
        }
    }
}
