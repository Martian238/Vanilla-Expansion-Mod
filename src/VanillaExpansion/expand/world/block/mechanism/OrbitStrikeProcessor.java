package VanillaExpansion.expand.world.block.mechanism;

import VanillaExpansion.content.CustomFx;
import VanillaExpansion.content.VEJSUnitTypes;
import VanillaExpansion.effects.OneBitRenderer;
import VanillaExpansion.expand.type.unit.internal.OrbitStrikeBeaconUnit;
import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.func.Cons;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Fires;
import mindustry.entities.Units;
import mindustry.entities.effect.ParticleEffect;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BuildVisibility;

import static mindustry.Vars.*;
import static mindustry.Vars.state;

public class OrbitStrikeProcessor extends Block {
    public OrbitStrikeProcessor(String name) {
        super(name);
        clipSize = 80000f;
        lightClipSize = 80000f;
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

    public float checkInterval = 60f;
    public Seq<String> checkUnits = Seq.with("ve-orbit-strike-beacon");
    public boolean checkOtherTeams = true;
    public boolean autoBan = true;

    public float strikeRadius = 120f;
    public float strikeChargeTime = 120f;
    public float strikeBeamTime = 60f;
    public float strikeCooldownTime = 120f;
    public float strikeOneBitTime = 3f;
    public Sound strikeSound = Sounds.none;
    public Sound strikeSoundGlobal = Sounds.none;

    public float cameraScale = 1f;

    public Color color1 = Color.valueOf("00ffce");

    public float delta(){
        return Time.delta;
    }

    public TextureRegion circleSoft = new TextureRegion();

    @Override
    public void load() {
        super.load();
        circleSoft = Core.atlas.find("ve-circle-soft");
    }

    private final boolean debug = false;

    @Override
    public boolean checkForceDark(Tile tile){
        return !accessible();
    }
    public boolean accessible(){
        return !privileged || state.rules.editor || state.playtestingMap != null || state.rules.allowEditWorldProcessors;
    }

    public class OrbitStrikeBuild extends Building {

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

        public float checkTimer = 0;

        @Override
        public void updateTile(){
            super.updateTile();
            orbitStrikeUpdate();
        }

        public static class BeaconPos{
            public float x, y;
            public Unit u;
            public BeaconPos(float x, float y, Unit u){
                this.x = x;
                this.y = y;
                this.u = u;
            }
        }

        public Seq<Unit> beacons = new Seq<>();
        public Seq<Unit> safeRemovedBeacons = new Seq<>();
        public Seq<Unit> activatedBeacons = new Seq<>();
        public Seq<BeaconPos> beaconsPos = new Seq<>();

        public boolean striking = false;
        public float strikeX = 0;
        public float strikeY = 0;
        public float strikeTimer = 0;
        public boolean strikeDamaged = false;

        public boolean working(){
            if (!enabled) return false;
            if (autoBan) {
                var sector = Vars.state.getSector();
                if (sector != null && sector.isCaptured()) {
                    var playerTeam = Vars.player != null ? Vars.player.team() : Vars.state.rules.defaultTeam;
                    if (playerTeam != team()) return false;
                }
            }
            return true;
        }

        public void orbitStrikeUpdate(){
            //Add beacons if enabled
            if(working()){
                checkTimer -= delta();
                if (checkTimer <= 0) {
                    checkTimer = checkInterval;
                    for (Unit u : Groups.unit) {
                        if (!beacons.contains(u) && checkUnits.contains(u.type.name) && (checkOtherTeams || u.team() == team())) {
                            if (u instanceof OrbitStrikeBeaconUnit bu) {
                                if (!bu.activated) {
                                    beacons.add(u);
                                    bu.processor = this;
                                }
                            } else {
                                beacons.add(u);
                            }
                            if (debug) Log.info("new beacon: " + u.type + " at (" + u.x + "," + u.y + ")");
                        }
                    }
                }
            }
            if (debug && Core.input.keyTap(KeyCode.z)) {
                Log.info(checkTimer + ", " + beacons.count(u -> true));
            }

            //Check whether any beacon is activated
            if(!striking && working()) {
                for (Unit u2 : beacons) {
                    BeaconPos pos = beaconsPos.find(p -> p.u == u2);
                    if (u2.dead() || activatedBeacons.contains(u2)) {
                        if(activatedBeacons.contains(u2)) activatedBeacons.remove(u2);
                        if (safeRemovedBeacons.contains(u2)) {
                            safeRemovedBeacons.remove(u2);
                            if (debug) Log.info("a beacon safely removed");
                            beacons.remove(u2);
                            if (pos != null) {
                                beaconsPos.remove(pos);
                            }
                        } else {
                            if (debug) Log.info("strike");
                            if (pos != null) {
                                striking = true;
                                strikeX = pos.x;
                                strikeY = pos.y;
                                strikeTimer = 0;
                                if (debug) Log.info("strike position set to (" + pos.x + "," + pos.y + ")");
                                beacons.remove(u2);
                                if (pos != null) {
                                    beaconsPos.remove(pos);
                                }
                                break;
                            }
                        }
                    } else {
                        if (pos != null) {
                            pos.x = u2.x();
                            pos.y = u2.y();
                        } else {
                            beaconsPos.add(new BeaconPos(u2.x(), u2.y(), u2));
                            if (debug) Log.info("beacon position added");
                        }
                    }
                }
            }

            //Calculate a strike
            if(striking){
                if(strikeTimer >= strikeChargeTime && !strikeDamaged){
                    strikeDamage();
                    strikeDamaged = true;
                    OneBitRenderer.enable();
                    if(debug)Log.info("strike hit");
                }
                if(strikeTimer >= strikeChargeTime + strikeOneBitTime && OneBitRenderer.isEnabled()) {
                    OneBitRenderer.disable();
                }
                if(strikeTimer >= strikeChargeTime + strikeBeamTime + strikeCooldownTime){
                    striking = false;
                    strikeDamaged = false;
                    if(debug) Log.info("strike end");
                }
                strikeTimer += delta();
            }
        }

        public void strikeDamage(){
            Units.nearbyEnemies(team(), strikeX, strikeY, strikeRadius, unitConsumer);
            indexer.eachBlock(null, strikeX, strikeY, strikeRadius, other -> other.team() != team(), Building::kill);
            if (!Vars.headless && Core.camera != null) {
                float d = Mathf.dst(strikeX, strikeY, Core.camera.position.x, Core.camera.position.y);
                Effect.shake(120, 6f, Core.camera.position.x, Core.camera.position.y);
                Effect.shake(30f * (float) Math.sqrt(Mathf.clamp(200f / d)), strikeBeamTime * 1.5f, Core.camera.position.x, Core.camera.position.y);
                strikeSound.at(strikeX, strikeY, 1f, Mathf.clamp(200f / d));
            }

            strikeSoundGlobal.play();
            CustomFx.loadCircleSoft();
            CustomFx.orbitStrikeSparks.at(strikeX, strikeY, 0f, color1, strikeRadius * 0.5f);
            CustomFx.orbitStrikeSparks.at(strikeX, strikeY, 0f, color1, strikeRadius);
            CustomFx.orbitStrikeSparks.at(strikeX, strikeY, 0f, color1, strikeRadius * 1.5f);
            CustomFx.orbitStrikeSparks.at(strikeX, strikeY, 0f, color1, strikeRadius * 2f);
            CustomFx.orbitStrikeScorch.at(strikeX, strikeY, 0f, color1, strikeRadius);
            for(int i = 0; i < 20; i++) {
                float fd = Mathf.range(strikeRadius);
                float fr = Mathf.range(180f);
                Tile tile = world.tileWorld(strikeX + Mathf.cosDeg(fr) * fd, strikeY + Mathf.sinDeg(fr) * fd);
                if(tile == null || tile.floor() == null || tile.floor().isLiquid) continue;
                Fire fire = Fires.get(tile);
                float ft = 420f + Mathf.range(120f);
                if(fire == null){
                    fire = Fire.create();
                    fire.tile = tile;
                    fire.lifetime = ft;
                    fire.set(tile.worldx(), tile.worldy());
                    fire.add();
                    world.tiles.setFire(tile.array(), fire);
                    Events.fire(EventType.Trigger.fireCreate);
                }else{
                    fire.lifetime = ft;
                    fire.time = 0f;
                }
                ParticleEffect splash = new ParticleEffect(){{
                    region = "ve-circle-soft";
                    length = strikeRadius * 3f;
                    sizeFrom = 16f;
                    sizeTo = 64f;
                    colorFrom = tile.getFloorColor().cpy().a(0.1f);
                    colorTo = tile.getFloorColor().cpy().a(0f);
                    interp = Interp.pow4Out;
                    sizeInterp = Interp.pow4Out;
                    colorInterp = Interp.pow2In;
                    particles = 20;
                    lifetime = 600f;
                    layer = Layer.floor + 0.2f;
                }};
                splash.at(tile.worldx(), tile.worldy());
            }
        }

        public static Cons<Unit> unitConsumer = u -> {
            if(u instanceof OrbitStrikeBeaconUnit b){
                b.striked();
            }else{
                u.kill();
            }
        };

        @Override
        public void draw(){
            super.draw();
            if(striking){
                float x = strikeX;
                float y = strikeY;
                float radius = strikeRadius;
                if(strikeTimer < strikeChargeTime){
                    float p = strikeTimer / strikeChargeTime;
                    Draw.z(Layer.overlayUI - 0.1f);
                    Draw.blend(Blending.additive);
                    float r = radius + 20f * radius * (1 - p) * (1 - p);
                    float size = tilesize * 1.5f;
                    float rot = 90f * p;
                    Draw.alpha((float) Math.pow(p, 0.3f));
                    for(int i = 0; i < 8; i++) {
                        Draw.color(Color.red);
                        float dx = x + r * Mathf.cosDeg(rot + i * 45f);
                        float dy = y + r * Mathf.sinDeg(rot + i * 45f);
                        Draw.rect(circleSoft, dx, dy, size, size);
                        Draw.color(Color.red.cpy().lerp(Color.white, 0.5f));
                        Draw.rect(circleSoft, dx, dy, size / 2, size / 2);
                        Drawf.light(dx, dy, p * size * 4f, Color.red, 1f);
                    }
                    Draw.rect(circleSoft, x, y, size, size);
                    Draw.color(Color.red.cpy().lerp(Color.white, 0.5f));
                    Draw.rect(circleSoft, x, y, size / 2, size / 2);
                    Draw.color(color1.cpy().lerp(Color.white, (float)Math.pow(p, 8f)));
                    Draw.alpha((float)Math.pow(p, 8f) * 0.6f);
                    Draw.rect(circleSoft, x, y, radius * 6f, radius * 6f);
                    Drawf.light(x, y, p * size * 4f, Color.red, 1f);
                    Drawf.light(x, y, radius * 10f, color1, p * p * p);
                }else if(strikeTimer < strikeChargeTime + strikeBeamTime){
                    float p = (strikeTimer - strikeChargeTime) / strikeBeamTime;
                    Draw.z(Layer.light);
                    Draw.blend(Blending.additive);
                    float r1 = radius * (1 - (float) Math.pow(p, 3));
                    float r2 = 0.75f * radius * (1 - p);
                    float cs = Core.camera.height;
                    float cx = Core.camera.position.x;
                    float cy = Core.camera.position.y;
                    float cz = cs * 0.5f * cameraScale;
                    float cMaxLen = 0.5f * Mathf.dst(Core.camera.width, Core.camera.height);
                    Drawf.light(x, y, r1 * 80f, color1, 1f);
                    Drawf.light(x, y, r1 * 40f, Color.white, 1 - p);
                    Draw.alpha(0.0075f);
                    boolean shouldBreak = false;
                    for(int i = 0; i < 4000; i++) {
                        float h = i * 4f;
                        float px = (cx / cz - x / h) / (1 / cz - 1 / h);
                        float py = (cy / cz - y / h) / (1 / cz - 1 / h);
                        float ar1 = cz * (r1 / (cz - h));
                        float ar1d = ar1 * 1.414f;
                        float d = Mathf.dst(px, py, cx, cy);
                        if(d > ar1d + cMaxLen || d < ar1d - cMaxLen) {
                            if(shouldBreak) break;
                            shouldBreak = true;
                            Draw.color(Color.white);
                            Draw.alpha(1f);
                        }else {
                            if (i > 3000) {
                                Draw.alpha(0.01f * (4000 - i) / 1000);
                            }
                            Draw.color(color1.cpy().lerp(Color.white, 1 - (float) Math.pow(Mathf.clamp(i / 125f), 0.01f)));
                        }
                        Draw.rect(circleSoft, px, py, ar1d, ar1d);
                        float ar2 = cz * (r2 / (cz - h));
                        float ar2d = ar2 * 1.414f;
                        if(i > 3000){
                            Draw.color(color1.cpy().lerp(Color.white, (4000 - i) / 1000f));
                        }else{
                            Draw.color(Color.white);
                        }
                        Draw.rect(circleSoft, px, py, ar2d, ar2d);
                    }
                    Draw.color(Color.white);
                    Draw.alpha(0.75f);
                    double v = radius * 20f * Math.pow(1 - p, 5);
                    Draw.rect(circleSoft, strikeX, strikeY, (float) v, (float) v);
                    Draw.color(color1.cpy().lerp(Color.white, 0.5f - p * 0.5f));
                    Draw.alpha(1 - p);
                    for(int i = 0; i < 20; i++){
                        float r = (float)Math.pow(p, 0.3) * radius * (20f / ((float)Math.pow(i, 0.8) + 0.5f));
                        float h = i * i * 50f;
                        if(cz <= h) break;
                        float px = (cx / cz - x / h) / (1 / cz - 1 / h);
                        float py = (cy / cz - y / h) / (1 / cz - 1 / h);
                        float ar = cz * (r / (cz - h));
                        Lines.stroke((float) (cz * (radius / (cz - h)) * 0.3 * (1 - p * p)));
                        Lines.circle(px, py, ar);
                    }
                }
                Draw.blend();
                Draw.color();
                Draw.alpha(1f);
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(striking);
            write.f(strikeTimer);
            write.bool(strikeDamaged);
            write.f(strikeX);
            write.f(strikeY);
        }
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                striking = read.bool();
                strikeTimer = read.f();
                strikeDamaged = read.bool();
            }else{
                striking = false;
                strikeTimer = 0;
                strikeDamaged = false;
            }
            if(revision >= 3){
                strikeX = read.f();
                strikeY = read.f();
            }
        }
        @Override
        public byte version(){
            return 3;
        }
    }
}
