package VanillaExpansion.expand.type.unit.annihilate;

import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.UnitEntity;
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

    public float ringRotSpeed = 5f;
    private float ringRot = 0f;

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
        getPart("sideR").unit.flip = true;
        getPart("frontR").unit.flip = true;
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
        summonPart(partTypeCap, "cap", 0, -92, 0);
        summonPart(partTypeSideR, "sideR", 70, 0, 0);
        summonPart(partTypeSideL, "sideL", -70, 0, 0);
        summonPart(partTypeFrontR, "frontR", 0, 90, 0);
        summonPart(partTypeFrontL, "frontL", 0, 90, 0);
        summonPart(partTypeShieldR, "shieldR", 100, 65, 60);
        summonPart(partTypeShieldL, "shieldL", -100, 65, -60);
        summonPart(partTypeArmFR, "armFR", 135, 93, 60);
        summonPart(partTypeArmFL, "armFL", -135, 93, -60);
        summonPart(partTypeArmBR, "armBR", 125, -93, 120);
        summonPart(partTypeArmBL, "armBL", -125, -93, -120);
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
            }});
        }
    }

    @Override
    public boolean killable(){
        return false;
    }

    /** Update methods */
    @Override
    public void update(){
        super.update();

        partsUpdate();
        ringUpdate();
    }

    public void ringUpdate(){
        ringRot += ringRotSpeed;
        if(ringRot >= 90) ringRot -= 90f;
        if(ringRot < 0) ringRot += 90f;
    }


    public void partsUpdate(){
        for(AnniPart p : anniParts){
            if(p.unit != null){
                if(p.following){
                    p.unit.x(x + p.x * xCos() + p.y * yCos());
                    p.unit.y(y + p.x * xSin() + p.y * ySin());
                    p.unit.rotation(p.rot);
                }else{
                    p.unit.x(p.wx);
                    p.unit.y(p.wy);
                    p.unit.rotation(p.wRot);
                }
            }
        }
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
            }
        }

        if(shieldAlpha > 0 && !generalTargetable()){
            type.drawShield(this);
        }
        partBody();
        partCharge();
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
        Draw.alpha(1f);
        Draw.rect(ringTopRegion, ax, ay, rotation - 90f);
        Draw.reset();
    }
}
