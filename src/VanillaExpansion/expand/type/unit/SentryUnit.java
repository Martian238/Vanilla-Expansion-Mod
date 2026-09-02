package VanillaExpansion.expand.type.unit;

import VanillaExpansion.expand.world.block.crux.SentryAlertStorer;
import arc.Events;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.Door;
import mindustry.world.blocks.production.GenericCrafter;

import java.util.Objects;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class SentryUnit extends UnitEntity {
    public static UnitEntity create(){
        return new SentryUnit();
    }

    public float visionRadius = 80f;
    /** An additional arc area in where a player can make the unit suspecting */
    public float suspectRadius = 60f;
    public float visionAngle = 80f;
    /** Time before the unit back to patrol from default status */
    public float safeTime = 90f;
    /** Time before the unit starts moving toward a suspected player */
    public float movingWarmupTime = 60f;
    /** Time before the unit arrives at the last position or misses the target and back to default status */
    public float suspectTime = 120f;

    /** Default duration of an arc wave effect */
    public float areaWaveTime = 20f;
    public float areaWaveInterval = 40f;
    /** Default speed multiplier of the arc wave effect in suspect status */
    public float areaWaveMultiplierSuspect = 2f;
    /** Default speed multiplier of the arc wave effect in alert status */
    public float areaWaveMultiplierAlert = 4f;
    public Color areaColorDefault = Color.valueOf("98ffa9");
    public Color areaColorSuspect = Color.valueOf("ffd37f");
    public Color areaColorAlert = Color.valueOf("f25555");

    /** Whether the unit will continuously attack at the last position before it back to default status */
    public boolean continuousAttack;

    /** Target team. Players of other teams won't be scanned */
    public Team playerTeam = Team.sharded;

    /** Sound played when notices a suspected player in 0 - 2 global status */
    public Sound suspectSound = Sounds.none;
    /** Sound played when spots a player in 0 - 1 global status */
    public Sound alertSound = Sounds.none;
    /** Metal Gear Solid exclamation mark effect unit */
    public UnitType exclamationMarkUnit;
    public float exclamationMarkOffset = 3.75f * tilesize;
    /** Time before directly starting attack when a player is suspected but the unit cannot move towards it */
    public float confirmTime = 300f;

    public Vec2 defaultPos = new Vec2();
    public float defaultRotation = 0f;
    public class PatrolPoint {
        public float x = 0f;
        public float y = 0f;
        public boolean hasRotation = false;
        public float rotation = 0f;
        public float pause = 0f;
    }
    public Seq<PatrolPoint> patrolPoints = new Seq<>();

    public void loadKeys(){
        if(type instanceof SentryUnitType s){
            visionRadius = s.visionRadius;
            suspectRadius = s.suspectRadius;
            safeTime = s.suspectDuration;
            visionAngle = s.visionAngle;
            movingWarmupTime = s.movingWarmupTime;
            areaWaveTime = s.areaWaveTime;
            areaWaveInterval = s.areaWaveInterval;
            areaWaveMultiplierAlert = s.areaWaveMultiplierAlert;
            areaWaveMultiplierSuspect = s.areaWaveMultiplierSuspect;
            areaColorDefault = s.areaColorDefault;
            areaColorSuspect = s.areaColorSuspect;
            areaColorAlert = s.areaColorAlert;
            playerTeam = s.playerTeam;
            suspectTime = s.suspectTime;
            continuousAttack = s.continuousAttack;
            suspectSound = s.suspectSound;
            alertSound = s.alertSound;
            exclamationMarkUnit = s.exclamationMarkUnit;
            exclamationMarkOffset = s.exclamationMarkOffset;
            confirmTime = s.confirmTime;

            visionAngleActual = visionAngle;
            stopRange = visionRadius - Math.max(2f * tilesize, 0.25f * visionRadius);
            type.range = tilesize * 0.1f;
            defaultPos.set(x, y);
            defaultRotation = rotation;

            if(debug) Log.info("Key loaded");
        }
    }

    private Unit p = null;
    private SentryAlertStorer.SentryAlertStorerBuild s = null;
    private int status = 0; // 0:default, 1:suspect, 2:alert
    private int globalStatus = 0; // 0:safe, 1:caution, 2:alert
    private float statusTimer = 0;
    private int playerCount = 1;
    private float areaWaveTimer = 0;
    private float areaColorTarget = 0;
    private float areaColorProgress = 0;
    private float visionAngleActual = 80f;
    private boolean visionEnabled = true;
    private Vec2 lastPos = new Vec2(0, 0);
    private float movingWarmup = 0;
    private boolean rotateToLastPos = false;
    private Unit exclamationMarkUnitSpawned = null;
    private boolean debug = true;
    private boolean suspectSoundPlayed = false;
    private boolean alertSoundPlayed = false;
    private boolean exclamationMarkCreated = false;
    private float predictMultiplier = 30f;
    private Vec2 lastVel = new Vec2(0, 0);
    private float stopRange;
    private float lastRot = 0f;
    private float ringColorTarget = 0f;
    private float ringColorProgress = 0f;
    private float confirmTimer = 0f;
    private boolean confirming = false;
    private float confirmAlpha = 0f;

    public void getPlayerCount(){
        playerCount = 0;
        for(Unit u : Groups.unit){
            if(u.isPlayer() && u.team == playerTeam){
                playerCount++;
            }
        }
        //Log.info("Player count: " + playerCount);
    }
    public void bindPlayer(){
        p = null;
        for(Unit u : Groups.unit){
            if(u.isPlayer() && u.team == playerTeam){
                if(p == null || !Objects.equals(u.type.name, bindType)) p = u;
                if(getDistance(p.x, p.y, x, y) > getDistance(u.x, u.y, x, y)) p = u;
            }
        }
        if(p != null) bindType = p.type.name;
        if(debug && p != null) Log.info("Player bind: " + p.type.localizedName);
        if(debug && p == null) Log.info("Player bind failed");
    }
    public void bindStorer(){
        s = null;
        for(Building b : Groups.build){
            if(b instanceof SentryAlertStorer.SentryAlertStorerBuild sb && b.team == team){
                s = sb;
                break;
            }
        }
        if(debug && s != null) Log.info("Storer bind: " + s.block.localizedName);
        if(debug && s == null) Log.info("Storer bind failed");
    }

    public void checkAreaWithin(){
        float d = getDistance(p.x, p.y, x, y);
        float a = Angles.angleDist(rotation, Mathf.radiansToDegrees * Mathf.atan2(p.x - x, p.y - y));
        areaColorTarget = 0;
        confirming = false;
        if(d <= visionRadius + suspectRadius && a <= visionAngle * 0.5f && playerVisible(p)){
            lastVel.set(p.vel.x, p.vel.y);
            lastRot = p.rotation;
            if(vel.len2() <= 1f && movingWarmup >= movingWarmupTime && status == 1) confirming = true;
            for(int i = 0; i <= predictMultiplier; i++){
                Tile tile = world.tileWorld(p.x + lastVel.x * i, p.y + lastVel.y * i);
                if(tile == null || !tile.solid()) {
                    lastPos.set(p.x + lastVel.x * i, p.y + lastVel.y * i);
                }else{
                    break;
                }
            }
            rotateToLastPos = true;
            if(d > visionRadius && statusTimer <= safeTime && status == 0 && globalStatus < 3 && !suspectSoundPlayed){
                suspectSound.at(x, y, 1f, 0.5f);
                if(debug) Log.info("Suspect");
                suspectSoundPlayed = true;
            }
            if((status == 2 || globalStatus == 3 || d <= visionRadius) && (statusTimer <= safeTime || status == 1)){
                if(globalStatus < 2 && !alertSoundPlayed) {
                    alertSound.at(x, y, 1f, 0.5f);
                    if(debug) Log.info("Alert");
                    alertSoundPlayed = true;
                }
                if(globalStatus < 3 && !exclamationMarkCreated){
                    if(debug) Log.info("Exclamation mark");
                    exclamationMarkCreated = true;
                    exclamationMarkUnitSpawned = exclamationMarkUnit.create(team);
                    exclamationMarkUnitSpawned.set(x, y + exclamationMarkOffset);
                    Events.fire(new EventType.UnitCreateEvent(exclamationMarkUnitSpawned, null, this));
                    if(!Vars.net.client()){
                        exclamationMarkUnitSpawned.add();
                        Units.notifyUnitSpawn(exclamationMarkUnitSpawned);
                    }
                    exclamationMarkUnitSpawned.rotation = 90f;
                }
            }
            statusTimer = suspectTime + safeTime;
        }
        if(status < 2 && globalStatus < 3) {
            if (d <= visionRadius + suspectRadius && a <= visionAngle * 0.5f && playerVisible(p)) {
                if (d <= visionRadius || confirmTimer >= confirmTime) {
                    status = 2;
                    alertSend();
                    if(debug) Log.info("Alert1");
                    areaColorTarget = 1;
                } else {
                    status = 1;
                    cautionSend();
                    areaColorTarget = 0.5f;
                }
            }
        }else{
            if (d <= visionRadius + suspectRadius && a <= visionAngle * 0.5f && playerVisible(p)) {
                status = 2;
                alertSend();
                if(debug) Log.info("Alert2");
                areaColorTarget = 1f;
            }
        }
        if(status == 0){
            suspectSoundPlayed = alertSoundPlayed = exclamationMarkCreated = false;
        }
        if(status == 0) ringColorTarget = 0;
        if(status == 1 || globalStatus == 2) ringColorTarget = 0.5f;
        if(status == 2 || globalStatus == 3) ringColorTarget = 1f;
    }

    public void applyDisarm(){
        float d = getDistance(p.x, p.y, x, y);
        float a = Angles.angleDist(rotation, Mathf.radiansToDegrees * Mathf.atan2(p.x - x, p.y - y));
        boolean attack = false;
        if(d <= visionRadius + suspectRadius && a <= visionAngle * 0.5f && playerVisible(p)){
            if(status == 2 || globalStatus == 3) attack = true;
            if(d <= visionRadius || (status == 2 && statusTimer >= safeTime && continuousAttack)) attack = true;
        }
        if(!attack) apply(StatusEffects.disarmed, 5f);
    }

    public boolean playerVisible(Unit u){
        return !visionBlocked(u) && visionEnabled && visionAngleActual >= visionAngle * 0.9f;
    }

    public boolean visionBlocked(Unit u){
        float d = getDistance(u.x, u.y, x, y);
        float t = (int) Math.floor(d * 2);
        boolean blocked = false;
        for(int i = 0; i <= t; i++){
            Tile tile = world.tileWorld(x + i / t * (u.x - x), y + i / t * (u.y - y));
            Building build = world.buildWorld(x + i / t * (u.x - x), y + i / t * (u.y - y));
            if(build != null){
                if(build instanceof Door.DoorBuild db && !db.open) {
                    //if(debug) Log.info("Blocked by door");
                    blocked = true;
                    break;
                }
                if(!(build.block instanceof GenericCrafter) && build.block.solid) {
                    blocked = true;
                    break;
                }
            }
            if(build == null && tile != null && tile.solid()){
                blocked = true;
                break;
            }
        }
        return blocked;
    }

    private Vec2 commandPos = new Vec2(x, y);
    public void calculateAreaTimers(){
        if(status == 1) movingWarmup++;
        if(status == 0) movingWarmup = 0;
        if((movingWarmup >= movingWarmupTime && status == 1) || status == 2){
            commandPos = new Vec2(x, y);
            if(lastVel.len2() < 0.1f){
                lastRot = Mathf.radiansToDegrees * Mathf.atan2(lastPos.x - x, lastPos.y - y);
            }
            for(int i = 0; i <= 1000; i++){
                Tile tile = world.tileWorld(lastPos.x - i * Mathf.cosDeg(lastRot), lastPos.y - i * Mathf.sinDeg(lastRot));
                if((tile == null || !tile.solid()) && i <= stopRange) {
                    commandPos.set(lastPos.x - i * Mathf.cosDeg(lastRot), lastPos.y - i * Mathf.sinDeg(lastRot));
                }else{
                    break;
                }
            }
            if(!within(commandPos.x, commandPos.y, type.range)) command().commandPosition(commandPos, true);
        }
        if(statusTimer > 0 && within(commandPos.x, commandPos.y, type.range + tilesize)){
            statusTimer--;
            if(statusTimer <= safeTime){
                status = 0;
            }
        }
        commandFix();
        if(confirming) confirmTimer ++; stopTrackingTimer = 0;
        if(status == 0) confirmTimer = 0; stopTrackingTimer = 0;
    }

    public void commandFix(){
        if(command().hasCommand()){
            if(within(commandPos.x, commandPos.y, type.range) || type.speed <= 0){
                Vec2 tv = new Vec2(x, y);
                commandPos = tv;
                command().commandPosition(tv, true);
            }
            if(!within(commandPos.x, commandPos.y, type.range) && vel.len2() < 0.1f){
                stopTrackingTimer++;
                if(stopTrackingTimer > movingWarmupTime * 3f){
                    Vec2 tv = new Vec2(x, y);
                    commandPos = tv;
                    command().commandPosition(tv, true);
                    status = 0;
                }
            }else{
                stopTrackingTimer = 0f;
            }
        }
    }

    private int currentPatrolPoint;
    private void patrol(){
        if(patrolPoints.isEmpty()){
            if(statusTimer <= 0){
                if(!within(defaultPos.x, defaultPos.y, type.range)){
                    commandPos.set(defaultPos.x, defaultPos.y);
                    command().commandPosition(commandPos, true);
                }else{
                    rotating = true;
                    rotateTo(this, x + tilesize * Mathf.cosDeg(defaultRotation), y + tilesize * Mathf.sinDeg(defaultRotation));
                    rotatingTarget = defaultRotation;
                }
            }
        }
    }


    private float bindTimer = 0f;
    private float bindTime = 60f;
    private boolean keyLoaded = false;
    private String bindType;
    private float previousRot;
    private float stopRotTimer = 0f;
    private float stopTrackingTimer = 0f;
    @Override
    public void update(){
        super.update();
        rotating = false;
        bindTimer++;
        areaWaveTimer += status == 2? areaWaveMultiplierAlert : (status == 1? areaWaveMultiplierSuspect : 1);
        if(areaWaveTimer >= areaWaveInterval + areaWaveTime){
            areaWaveTimer -= areaWaveInterval + areaWaveTime;
        }

        if(!keyLoaded){
            loadKeys();
            keyLoaded = true;
            bindStorer();
        }

        areaColorProgress = Mathf.approachDelta(areaColorProgress, areaColorTarget, 0.03f);
        ringColorProgress = Mathf.approachDelta(ringColorProgress, ringColorTarget, 0.03f);
        confirmAlpha = Mathf.approachDelta(confirmAlpha, confirming? 1 : 0, 0.01f);

        if(bindTimer >= bindTime){
            bindTimer = 0;
            getPlayerCount();
            bindTime = 120f;
            if(p == null || !p.isPlayer() || p.dead()) bindPlayer();
            if(playerCount > 1) bindTime = 30f;
            if(s == null) bindStorer();
        }
        if(p == null || p.dead()) bindPlayer();
        if(s != null) globalStatus = s.status;
        if(s == null || s.dead()) bindStorer();
        if(p != null){
            checkAreaWithin();
            applyDisarm();
        }else{
            apply(StatusEffects.disarmed, 5f);
        }

        calculateAreaTimers();

        if(rotateToLastPos){
            if(p != null && getDistance(p.x, p.y, x, y) <= visionRadius + suspectRadius
                    && Angles.angleDist(rotation, Mathf.radiansToDegrees * Mathf.atan2(p.x - x, p.y - y))
                    <= 0.5f * visionAngle && playerVisible(p)){
                rotateTo(this, p.x, p.y);
            }else {
                rotateTo(this, lastPos.x, lastPos.y);
                if (statusTimer <= safeTime && Angles.angleDist(Mathf.radiansToDegrees
                        * Mathf.atan2(lastPos.x - x, lastPos.y - y), rotation()) <= 0.1f) {
                    rotateToLastPos = false;
                }
            }
        }

        if(exclamationMarkUnitSpawned != null){
            exclamationMarkUnitSpawned.set(x, y + exclamationMarkOffset);
        }

        patrol();

        if(!rotating && getDistance(commandPos.x, commandPos.y, x, y) > 2 * tilesize){
            rotatingTarget = Mathf.radiansToDegrees * Mathf.atan2(vel.x, vel.y);
            rotating = true;
        }
        if(rotating && Angles.angleDist(rotation, rotatingTarget) >= 100f){
            visionEnabled = false;
        }
        if((!visionEnabled && Angles.angleDist(rotation, rotatingTarget) <= type.rotateSpeed * 5f)){
            visionEnabled = true;
        }
        visionAngleActual = Mathf.approachDelta(visionAngleActual, visionEnabled ? visionAngle : 0,  2f + 0.3f * Math.abs(visionAngleActual - (visionEnabled ? visionAngle : 0)));
        if(Float.isNaN(visionAngleActual)) visionAngleActual = visionAngle;
        if(!visionEnabled){
            if(previousRot != rotation){
                previousRot = rotation;
            }else{
                stopRotTimer++;
                if(stopRotTimer >= 60f) visionEnabled = true;
            }
        }else stopRotTimer = 0f;

        if(type.speed <= 0) commandPos.set(x, y);
    }

    public void cautionSend(){
        if(s != null) {
            s.caution();
            s.lastPos.set(lastPos.x, lastPos.y);
            if(debug) Log.info("Caution sending to " + s);
        }
    }
    public void alertSend(){
        if(s != null) {
            s.alert();
            s.lastPos.set(lastPos.x, lastPos.y);
            if(debug)  Log.info("Alert sending to " + s);
        }
    }

    public void drawArea(){
        float fraction = (float) Math.sqrt(Mathf.clamp(visionAngleActual / visionAngle));
        Draw.z(Layer.blockUnder + 0.1f);
        Draw.color(areaColorDefault, areaColorSuspect, areaColorAlert, areaColorProgress);
        Draw.alpha(0.2f * fraction);
        Fill.arc(x, y, visionRadius, visionAngleActual / 360f, rotation - 0.5f * visionAngleActual);
        Draw.alpha(0.5f * (1 - Mathf.clamp(areaWaveTimer / areaWaveTime)) * fraction);
        Fill.arc(x, y, (visionRadius + suspectRadius) * Mathf.clamp(areaWaveTimer / areaWaveTime),
                visionAngleActual / 360f, rotation - 0.5f * visionAngleActual);
        Lines.stroke(1f);
        Draw.alpha(0.5f * fraction);
        Lines.arc(x, y, visionRadius, visionAngleActual / 360f, rotation - 0.5f * visionAngleActual);
        Draw.alpha(0.3f * fraction);
        Lines.lineAngle(x, y, rotation + visionAngleActual * 0.5f, visionRadius);
        Lines.lineAngle(x, y, rotation - visionAngleActual * 0.5f, visionRadius);
        if(confirmAlpha > 0.01f && status == 1){
            Draw.alpha(confirmAlpha * 0.75f);
            Draw.color(areaColorAlert);
            Lines.lineAngle(x, y, rotation + visionAngleActual * 0.5f * (1 - Mathf.clamp(confirmTimer / confirmTime)), visionRadius + suspectRadius);
            Lines.lineAngle(x, y, rotation - visionAngleActual * 0.5f * (1 - Mathf.clamp(confirmTimer / confirmTime)), visionRadius + suspectRadius);
            Lines.lineAngle(x, y, rotation, visionRadius + suspectRadius);
            Draw.alpha(confirmAlpha * Mathf.clamp(confirmTimer / confirmTime) * 0.75f);
            Fill.arc(x, y, visionRadius, visionAngleActual * (1 - Mathf.clamp(confirmTimer / confirmTime)) / 360f, rotation - 0.5f * visionAngleActual * (1 - Mathf.clamp(confirmTimer / confirmTime)));
        }
        if(visionEnabled) {
            Lines.stroke(2f);
            Draw.z(Layer.bullet - 0.01f);
            Draw.color(areaColorDefault, areaColorSuspect, areaColorAlert, ringColorProgress);
            Lines.arc(x, y, type.hitSize, 0.2f, -36f);
            Lines.arc(x, y, type.hitSize, 0.2f, 180f - 36f);
        }
        if(debug){
            Draw.z(Layer.blockUnder + 0.1f);
            Draw.color(areaColorAlert);
            Lines.stroke(2f);
            Lines.circle(lastPos.x, lastPos.y, tilesize * 1.5f);
            Draw.color(areaColorDefault);
            Lines.circle(commandPos.x, commandPos.y, tilesize * 1.5f);
        }
        Draw.color();
    }

    @Override
    public void draw(){
        super.draw();
        drawArea();
    }

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }

    private boolean rotating;
    private float rotatingTarget = 0f;
    public void rotateTo(Unit unit, float x, float y){
            float ta = Mathf.radiansToDegrees * Mathf.atan2(x - unit.x, y - unit.y);
            float sin = Mathf.sinDeg(ta - unit.rotation);
            rotatingTarget = ta;
            if(Angles.angleDist(ta, unit.rotation()) <= unit.type.rotateSpeed){
                unit.rotation(ta);
                rotating = false;
            }else {
                if (sin > 0) {
                    unit.rotation += unit.type.rotateSpeed;
                }else{
                    unit.rotation -= unit.type.rotateSpeed;
                }
            }
    }
}
