package VanillaExpansion.expand.type.unit.annihilate;

import arc.Events;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.effect.WaveEffect;
import mindustry.game.EventType;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.world;
import static mindustry.type.UnitType.shadowTX;
import static mindustry.type.UnitType.shadowTY;

public class AnnihilateMainUnit extends AnnihilatePartUnit {

    public static UnitEntity create(){
        return new AnnihilateMainUnit();
    }

    public TextureRegion ringRegion = new TextureRegion();
    public TextureRegion ringTopRegion = new TextureRegion();
    public TextureRegion emojiBaseRegion = new TextureRegion();
    public TextureRegion emojiNormalRegion = new TextureRegion();
    public TextureRegion emojiIdleRegion = new TextureRegion();
    public TextureRegion emojiPauseRegion = new TextureRegion();
    public TextureRegion emojiPuzzledRegion = new TextureRegion();
    public TextureRegion emojiHurtRegion = new TextureRegion();

    public float ringRotSpeed = 2f;
    private float ringRot = 0f;
    public Effect ringEffect = new WaveEffect(){{
        sizeFrom = sizeTo = 62.5f;
        strokeFrom = 5f;
        strokeTo = 0f;
        layer = 100f;
        colorFrom = Color.valueOf("f2555500");
        colorTo = Color.valueOf("f25555").mul(1.3f);
        lifetime = 20f;
        followParent = true;
        sides = 64;
    }};
    public float ringEffectRand = 6.5f;
    public float ringEffectInterval = 5f;
    private float ringEffectIntervalTime = 0f;

    /** Emoji
     * 0: Empty, 1: Base only, 2: Idle, 3: Normal, 4: Hurt, 5: Pause, 6: Puzzled */
    public int emoji = 3;
    public Color emojiLightColor = Color.valueOf("f25555");

    private final boolean debug = true;
    private boolean updateInitialized = false;

    public Seq<AnniPart> anniParts = new Seq<>();
    public static class AnniPart {
        public AnniPart(String name){
            this.name = name;
        }
        public String name;
        public AnnihilatePartUnit unit;
        /** Position based on the map */
        public float wx;
        public float wy;
        public float wRot;
        /** Position based on the boss center */
        public boolean following;
        public float x;
        public float y;
        public float rot;
        /** Default position based on the boss center */
        public float dfx;
        public float dfy;
        public float dfRot;
        /** Whether the part is playing idle animation */
        public boolean idle;
    }

    public AnnihilatePartUnitType partTypeCap = AnnihilateUnitTypes.anniCap;
    public AnnihilatePartUnitType partTypeSideR = AnnihilateUnitTypes.anniSideR;
    public AnnihilatePartUnitType partTypeSideL = AnnihilateUnitTypes.anniSideL;
    public AnnihilatePartUnitType partTypeFrontR = AnnihilateUnitTypes.anniFrontR;
    public AnnihilatePartUnitType partTypeFrontL = AnnihilateUnitTypes.anniFrontL;
    public AnnihilatePartUnitType partTypeShieldR = AnnihilateUnitTypes.anniShieldR;
    public AnnihilatePartUnitType partTypeShieldL = AnnihilateUnitTypes.anniShieldL;
    public AnnihilatePartUnitType partTypeArmFR = AnnihilateUnitTypes.anniArmFR;
    public AnnihilatePartUnitType partTypeArmFL = AnnihilateUnitTypes.anniArmFL;
    public AnnihilatePartUnitType partTypeArmBR = AnnihilateUnitTypes.anniArmBR;
    public AnnihilatePartUnitType partTypeArmBL = AnnihilateUnitTypes.anniArmBL;

    public AnniPart getPart(String name){
        return anniParts.find(p -> p.name.equals(name));
    }
    public void generalInitialize(){
        summonParts();
        setPartsData();
    }
    public void setPartsData(){
        getPart("sideL").unit.flip = true;
        getPart("frontL").unit.flip = true;
        getPart("armFL").unit.flip = true;
        getPart("armBL").unit.flip = true;
        getPart("shieldL").unit.flip = true;
        getPart("armFR").unit.isArm = true;
        getPart("armFL").unit.isArm = true;
        getPart("armBR").unit.isArm = true;
        getPart("armBL").unit.isArm = true;

        getPart("shieldR").unit.partLayerOffset = 0.1f;
        getPart("shieldL").unit.partLayerOffset = 0.1f;
        getPart("sideR").unit.partLayerOffset = 0.2f;
        getPart("sideL").unit.partLayerOffset = 0.2f;
        getPart("cap").unit.partLayerOffset = 0.3f;
        getPart("frontR").unit.partLayerOffset = 0.3f;
        getPart("frontL").unit.partLayerOffset = 0.3f;
        partLayerOffset = 0.4f;

        partsBackToDefault(true);
    }
    public void partsBackToDefault(boolean withOthers){
        for(AnniPart p : anniParts){
            p.x = p.dfx;
            p.y = p.dfy;
            p.rot = p.dfRot;
            p.following = true;
            if(withOthers){
                p.unit.partLayer = 0.75f;
                p.unit.elevationScl = 1f;
                p.unit.targetable = false;
                p.unit.vulnerable = false;
            }
        }
        if(withOthers) {
            getPart("armFR").unit.armRing = false;
            getPart("armFL").unit.armRing = false;
            getPart("armBR").unit.armRing = false;
            getPart("armBL").unit.armRing = false;
            getPart("armFR").unit.armLight = false;
            getPart("armFL").unit.armLight = false;
            getPart("armBR").unit.armLight = false;
            getPart("armBL").unit.armLight = false;
            getPart("armFR").unit.armRotSpeed = 2f;
            getPart("armFL").unit.armRotSpeed = -2f;
            getPart("armBR").unit.armRotSpeed = -2f;
            getPart("armBL").unit.armRotSpeed = 2f;
        }
    }
    public void summonParts(){
        summonPart(partTypeCap, "cap", 0, -80, 0);
        summonPart(partTypeSideR, "sideR", 60, 0, 0);
        summonPart(partTypeSideL, "sideL", -60, 0, 0);
        summonPart(partTypeFrontR, "frontR", 0, 90, 0);
        summonPart(partTypeFrontL, "frontL", 0, 90, 0);
        summonPart(partTypeShieldR, "shieldR", 105, 60, -50);
        summonPart(partTypeShieldL, "shieldL", -105, 60, 50);
        summonPart(partTypeArmFR, "armFR", 145, 83, -65);
        summonPart(partTypeArmFL, "armFL", -145, 83, 65);
        summonPart(partTypeArmBR, "armBR", 105, -83, -125);
        summonPart(partTypeArmBL, "armBL", -105, -83, 125);
    }
    public void summonPart(AnnihilatePartUnitType type, String name, float defaultX, float defaultY, float defaultRot){
        if(!anniParts.contains(p -> p.name.equals(name) && p.unit != null)) {
            AnnihilatePartUnit unitSpawn = (AnnihilatePartUnit) type.create(team);
            unitSpawn.set(x, y);
            Events.fire(new EventType.UnitCreateEvent(unitSpawn, null, this));
            if (!Vars.net.client()) {
                unitSpawn.add();
                Units.notifyUnitSpawn(unitSpawn);
            }
            unitSpawn.owner = this;
            unitSpawn.rotation(rotation);
            unitSpawn.targetable = unitSpawn.vulnerable = unitSpawn.ownerDead = false;
            float ux = x, uy = y;
            anniParts.add(new AnniPart(name) {{
                unit = unitSpawn;
                wx = ux;
                wy = uy;
                x = y = 0;
                wRot = rotation;
                rot = 0;
                following = true;
                dfx = defaultX;
                dfy = defaultY;
                dfRot = defaultRot;
                idle = true;
            }});
        }
    }

    @Override
    public boolean killable(){
        return false;
    }
    @Override
    public void remove(){
        for(AnniPart p : anniParts){
            if(p.unit != null) p.unit.remove();
        }
        super.remove();
    }

    /** Update methods */
    @Override
    public void update(){
        super.update();
        if(!updateInitialized){
            updateInitialized = true;
            generalInitialize();
        }

        partsUpdate();
        ringUpdate();
    }

    public void ringUpdate(){
        ringRot += ringRotSpeed;
        if(ringRot >= 90) ringRot -= 90f;
        if(ringRot < 0) ringRot += 90f;
        if(!Vars.headless) {
            ringEffectIntervalTime += Math.abs(ringRotSpeed / 4f);
            if (ringEffectIntervalTime >= ringEffectInterval) {
                ringEffectIntervalTime = 0;
                float a = Mathf.range(90f);
                float d = Mathf.range(ringEffectRand);
                Tmp.v1.trns(rotation - 90f,
                        d * Mathf.cosDeg(a),
                        d * Mathf.sinDeg(a));
                ringEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, Color.white, this);
            }
        }
    }


    public void partsUpdate(){
        for(AnniPart p : anniParts){
            if(p.unit != null){
                if(p.following){
                    if(p.idle && p.unit.isArm) {
                        if(p.name.contains("F")){
                            partFloatingIdle(p, this, 45f, 0f, 6f);
                        }else{
                            partFloatingIdle(p, this, 45f, 0.7f, 6f);
                        }
                    }else if(p.idle && p.name.contains("shield")) {
                        partFloatingIdle(p, this, 45f, 1.1f, 3f);
                    }else{
                        p.unit.x(x + p.x * xCos() + p.y * yCos());
                        p.unit.y(y + p.x * xSin() + p.y * ySin());
                        p.unit.rotation(p.rot + rotation);
                    }
                }else{
                    p.unit.x(p.wx);
                    p.unit.y(p.wy);
                    p.unit.rotation(p.wRot);
                }
            }
        }
    }
    public void partFloatingIdle(AnniPart p, Unit u, float scale, float offset, float mag){
        float d = mag * Mathf.sin(Time.time / scale + offset);
        p.unit.x(powMove(p.unit.x,
                u.x + p.dfx * xCos() + p.dfy * yCos() + d * Mathf.cosDeg(u.rotation + p.dfRot),
                10f));
        p.unit.y(powMove(p.unit.y,
                u.y + p.dfx * xSin() + p.dfy * ySin() + d * Mathf.sinDeg(u.rotation + p.dfRot),
                10f));
        float da = -7.5f * (Angles.angleDist(p.unit.rotation, u.vel().angle()) / 180f) * (u.vel().len2() / u.speed()) * (p.unit.flip ? -1f : 1f);
        p.unit.rotation(powRot(p.unit.rotation, u.rotation + p.dfRot + da, 15f));
    }

    /** Chiniun Kun part move method */
    public float powMove(float current, float target, float scale){
        return (current * scale + target)/(scale + 1f);
    }
    public float powRot(float current, float target, float scale){
        if(Math.abs(current - target) >= 180f){
            float a = Angles.angleDist(current, target);
            if(a < 180f){
                if(Mathf.sinDeg(target - current) > 0){
                    return (current * scale + (current + a))/(scale + 1f);
                }else{
                    return (current * scale + (current - a))/(scale + 1f);
                }
            }
        }
        return (current * scale + target)/(scale + 1f);
    }
    /** Converts follow position to world position */
    public void setToWorld(AnniPart p){
        p.wx = xToWorld(p.x, p.y);
        p.wy = yToWorld(p.x, p.y);
        p.wRot = p.rot + rotation;
        p.following = false;
    }
    public float xToWorld(float fx, float fy){
        return x + fx * xCos() + fy * yCos();
    }
    public float yToWorld(float fx, float fy){
        return y + fx * xSin() + fy * ySin();
    }
    /** Converts world position to follow position */
    public void setToFollow(AnniPart p){
        p.x = xToFollow(p.wx, p.wy);
        p.y = yToFollow(p.wx, p.wy);
        p.rot = p.wRot - rotation;
        p.following = true;
    }
    public float xToFollow(float wx, float wy){
        return (wx - wy * yCos() / ySin() - x + y * yCos() / ySin()) / (xCos() - xSin() * yCos() / ySin());
    }
    public float yToFollow(float wx, float wy){
        return (wx - wy * xCos() / xSin() - x + y * xCos() / xSin()) / (yCos() - xCos() * ySin() / xSin());
    }

    public float xCos(){
        return Mathf.cosDeg(rotation - 90f);
    }
    public float yCos(){
        return Mathf.cosDeg(rotation);
    }
    public float xSin(){
        return Mathf.sinDeg(rotation - 90f);
    }
    public float ySin(){
        return Mathf.sinDeg(rotation);
    }

    /** Draw methods */
    @Override
    public void draw(){
        if(!drawInitialized){
            drawInitialized = true;
            if(type instanceof AnnihilateMainUnitType am){
                ringRegion = am.ringRegion;
                ringTopRegion = am.ringTopRegion;
                chargeRingRadius = am.chargeRingRadius;
                chargeRingStroke = am.chargeRingStroke;
                shadowRegion = am.softShadowRegion;
                emojiBaseRegion = am.emojiBaseRegion;
                emojiNormalRegion = am.emojiNormalRegion;
                emojiIdleRegion = am.emojiIdleRegion;
                emojiPauseRegion = am.emojiPauseRegion;
                emojiPuzzledRegion = am.emojiPuzzledRegion;
                emojiHurtRegion = am.emojiHurtRegion;
            }
        }

        if(shieldAlpha > 0 && !generalTargetable()){
            type.drawShield(this);
        }
        partBody();
        partCharge();
        drawEmoji();
    }
    public void drawEmoji(){
        if(emoji >= 1){
            Draw.z(Layer.fogOfWar + 1f);
            Draw.color();
            Draw.blend(Blending.additive);
            Draw.rect(emojiBaseRegion, x, y, rotation + 90f);
            if(emoji == 2) Draw.rect(emojiIdleRegion, x, y, rotation + 90f);
            if(emoji == 3) Draw.rect(emojiNormalRegion, x, y, rotation + 90f);
            if(emoji == 4) Draw.rect(emojiHurtRegion, x, y, rotation + 90f);
            if(emoji == 5) Draw.rect(emojiPauseRegion, x, y, rotation + 90f);
            if(emoji == 6) Draw.rect(emojiPuzzledRegion, x, y, rotation + 90f);
            Draw.color(emojiLightColor, 0.5f);
            Draw.rect(shadowRegion, x + 120f, y, 240f, 4f, 0f);
            Draw.rect(shadowRegion, x + 60f, y, 120f, 8f, 0f);
            Draw.rect(shadowRegion, x - 120f, y, 240f, 4f, 0f);
            Draw.rect(shadowRegion, x - 60f, y, 120f, 8f, 0f);
            Draw.blend();
        }
    }
    @Override
    public void partShadow(float ax, float ay){
        float e = elevationScl * defaultElevation;
        float x = ax + shadowTX * e, y = ay + shadowTY * e;
        Floor floor = world.floorWorld(x, y);
        float dest = floor.canShadow ? 1f : 0f;
        shadowAlpha = shadowAlpha < 0 ? dest : Mathf.approachDelta(shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * shadowAlpha);
        Draw.rect(ringRegion, x + shadowTX * e, y + shadowTY * e, rotation - 90 + ringRot);
        Draw.color();
    }
    @Override
    public void partSoftShadow(float ax, float ay){
        float sx = ax, sy = ay;
        if(isArm){
            float d = armLength * 0.5f;
            sx += d * Mathf.cosDeg(rotation);
            sy += d * Mathf.sinDeg(rotation);
        }
        Draw.color(0, 0, 0, 0.4f);
        float rad = 1.6f;
        Draw.rect(shadowRegion, sx, sy,
                ringRegion.width * region.scl() * rad * Draw.xscl,
                ringRegion.height * region.scl() * rad * Draw.yscl, rotation - 90);
        Draw.color();
    }
    @Override
    public void partBody(){
        float ax = x;
        float ay = y;
        Draw.z(getPartLayer(Math.max(partLayerOffset, 0f) - (partLayer <= 0.25f ? 0.01f : 0.9f)));
        partShadow(ax, ay);
        Draw.z(getPartLayer(partLayerOffset));
        partSoftShadow(ax, ay);
        Draw.alpha(1f);
        Draw.color();
        type.applyColor(this);
        Draw.rect(ringRegion, ax, ay, rotation - 90f + ringRot);
        Draw.alpha(Mathf.lerp(0f, 1f, ringRot / 90f));
        Draw.rect(ringRegion, ax, ay, rotation - 180f + ringRot);
        Draw.color(Color.black, 0.4f);
        Draw.rect(shadowRegion, x, y, 180f, 90f, rotation - 90f);
        Draw.color();
        Draw.alpha(1f);
        Draw.rect(ringTopRegion, ax, ay, rotation - 90f);
        Draw.reset();
    }

    /** Save and load */
    @Override
    public void anniWrite(Writes write){
        write.s(2);
        write.i(charges);
        write.f(chargeTime);
        write.f(chargeTimeMax);
        write.f(chargeDelay);
        write.f(chargeDelayMax);
        write.i(interrupts);
        write.bool(interrupted);
        write.f(interruptCooldownTimer);
        write.bool(armLight);
        write.f(armLightProgress);
        write.bool(armRing);
        write.f(armRingProgress);
        write.bool(targetable);
        write.bool(vulnerable);

        write.bool(updateInitialized);
        write.i(emoji);

        write.s(anniParts.size);
        for(AnniPart p : anniParts){
            write.str(p.name);
            mindustry.io.TypeIO.writeUnit(write, p.unit);
            write.f(p.wx);
            write.f(p.wy);
            write.f(p.wRot);
            write.bool(p.following);
            write.f(p.x);
            write.f(p.y);
            write.f(p.rot);
            write.f(p.dfx);
            write.f(p.dfy);
            write.f(p.dfRot);
            write.bool(p.idle);
        }
    }
    @Override
    public void anniRead(Reads read){
        short anniVer = read.s();
        if(anniVer >= 2) {
            charges = read.i();
            chargeTime = read.f();
            chargeTimeMax = read.f();
            chargeDelay = read.f();
            chargeDelayMax = read.f();
            interrupts = read.i();
            interrupted = read.bool();
            interruptCooldownTimer = read.f();
            armLight = read.bool();
            armLightProgress = read.f();
            armRing = read.bool();
            armRingProgress = read.f();
            targetable = read.bool();
            vulnerable = read.bool();

            updateInitialized = read.bool();
            emoji = read.i();

            anniParts.clear();
            short anniPartsCount = read.s();
            for(int i = 0; i < anniPartsCount; i++){
                anniParts.add(new AnniPart(read.str()){{
                    unit = (AnnihilatePartUnit) mindustry.io.TypeIO.readUnit(read);
                    wx = read.f();
                    wy = read.f();
                    wRot = read.f();
                    following = read.bool();
                    x = read.f();
                    y = read.f();
                    rot = read.f();
                    dfx = read.f();
                    dfy = read.f();
                    dfRot = read.f();
                    idle = read.bool();
                }});
            }
        }
    }
}
