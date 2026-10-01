package VanillaExpansion.expand.type.unit.annihilate;

import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.world;
import static mindustry.type.UnitType.shadowTX;
import static mindustry.type.UnitType.shadowTY;

public class AnnihilatePartUnit extends UnitEntity {

    public static UnitEntity create(){
        return new AnnihilatePartUnit();
    }

    public Unit owner = null;
    public boolean ownerDead = false;
    public boolean targetable = false; // automatically targetable
    public boolean vulnerable = false; // when true, there's no damage reduction
    public boolean flip = false;

    /** (-infinite, 0.25]: below building, (0.25, 0.5]: above ground,
     * (0.5, 0.75]: fly low, (0.75, +infinite): fly high */
    public float partLayer = 0.75f;
    public float partLayerOffset = 0f;
    public float elevationScl = 1f;
    public float defaultElevation = 2f;

    public float chargeDelay = 0f;
    public float chargeTime = 0f;
    public int charges = 0;
    public float interruptThreshold = 1500f; //calculated on rawDamage()
    public float interruptCooldown = 6f; //constant
    public int interrupts = 0;
    public float chargeTimeMax = 0f;
    public float chargeDelayMax = 0f;
    public boolean interrupted = false;

    public TextureRegion region = new TextureRegion();
    public TextureRegion shadowRegion = new TextureRegion();
    public Color chargeRingColor1 = Color.valueOf("f25555");
    public Color chargeRingColor2 = chargeRingColor1.cpy().mul(1.25f);
    public Color chargeRingColor3 = chargeRingColor1.cpy().mul(1.5f);
    public Color chargeRingDelayColor = Color.valueOf("ffa665");

    public boolean isArm = false;
    public float armLength = 368f / 4f;
    public float armWidth = 138f / 4f;
    public float armOffset = 63f / 4f;
    public float armRotSpeed = 1f;
    public float armRotSpeedTarget = 1f;
    public boolean armLight = false;
    public boolean armRing = false;
    public Color armOutlineColor = Color.valueOf("16161c");
    public Color armLightColor = chargeRingColor3.cpy();
    public Color armRingColor = chargeRingColor1.cpy();
    public float armLightProgress = 0f;
    public float armRingProgress = 0f;
    public float armOutlineWidth = 0.75f;
    public float armRingRadius = armWidth * 1.5f;
    public float armRingGapAngle = 5f;
    public float armRingWidth = 24f;
    public TextureRegion armRegion1 = new TextureRegion();
    public TextureRegion armRegion2 = new TextureRegion();
    //public float armC = Color.white.toFloatBits();

    protected boolean drawInitialized = false;
    protected float interruptCooldownTimer = 0f;
    protected float chargeBright = 0f;
    private float regionOffsetX = 0f;
    private float regionOffsetY = 0f;
    protected float chargeRingRadius = 160f;
    protected float chargeRingStroke = 24f;
    protected float chargeRot = 0f;
    private float armRot = 0f;
    private float regionWidth, regionHeight;

    @Override
    public void update(){
        super.update();
        chargeUpdate();
        if(isArm){
            armUpdate();
        }
    }

    @Override
    public boolean killable(){
        return owner == null || owner.dead() || ownerDead;
    }

    public void armUpdate(){
        armRot += armRotSpeed * (flip ? -1 : 1);
        if(armRot >= 90f) armRot -= 60f;
        if(armRot < 30f) armRot += 60f;
        armRotSpeed = Mathf.approach(armRotSpeed, armRotSpeedTarget, 0.2f + 0.1f * Math.abs(armRotSpeed - armRotSpeedTarget));
        armLightProgress = Mathf.approach(armLightProgress, armLight ? 1f : 0f, 0.03f);
        armRingProgress = Mathf.approach(armRingProgress, armRing ? 1f : 0f, 0.03f);
    }

    public void chargeUpdate(){
        if(charges > 0 && interrupts <= 0) interrupted = false;
        if(chargeTimeMax < chargeTime) chargeTimeMax = chargeTime;
        if(chargeDelayMax < chargeDelay) chargeDelayMax = chargeDelay;
        if(charges <= 0){
            chargeTimeMax = 0;
            chargeDelayMax = 0;
            interrupts = 0;
        }
        if(interruptCooldownTimer > 0) interruptCooldownTimer--;
        if(chargeTime > 0 && chargeDelay <= 0){
            chargeTime--;
            chargeRot++;
            if(chargeRot >= 360f) chargeRot -= 360f;
        }
        if(chargeDelay > 0 || chargeTime <= 0) chargeBright = 1f;
        if(chargeBright > 0 && chargeDelay <= 0) chargeBright -= 1f / 20f;
        if(chargeDelay > 0) chargeDelay--;
        if((chargeTime <= 0 || interrupts >= charges) && charges > 0){
            if(interrupts >= charges) interrupted = true;
            charges = 0;
            interrupts = 0;
        }
    }

    public void interruptEffect(){

    }

    public void dealInterrupt(float damage){
        if(damage >= interruptThreshold && charges > 0 && chargeDelay <= 0 && interruptCooldownTimer <= 0){
            interrupts++;
            interruptCooldownTimer = interruptCooldown;
            interruptEffect();
        }
    }

    @Override
    public void draw(){
        if(!drawInitialized){
            drawInitialized = true;
            if(type instanceof AnnihilatePartUnitType ap){
                region = ap.partRegion;
                defaultElevation = ap.defaultElevation;
                shadowRegion = ap.softShadowRegion;
                regionOffsetX = ap.spriteX;
                regionOffsetY = ap.spriteY;
                chargeRingRadius = ap.chargeRingRadius;
                chargeRingStroke = ap.chargeRingStroke;
                if(isArm){
                    armRegion1 = ap.armRegion1;
                    armRegion2 = ap.armRegion2;
                }
                regionWidth = (region.width / 4f) * (flip ? -1f : 1f);
                regionHeight = region.height / 4f;
            }
        }

        if(shieldAlpha > 0 && !generalTargetable()){
            type.drawShield(this);
        }
        partBody();
        partCharge();
    }

    public void partShadow(float ax, float ay){
        float e = elevationScl * defaultElevation;
        float x = ax + shadowTX * e, y = ay + shadowTY * e;
        Floor floor = world.floorWorld(x, y);
        float dest = floor.canShadow ? 1f : 0f;
        shadowAlpha = shadowAlpha < 0 ? dest : Mathf.approachDelta(shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * shadowAlpha);
        Draw.rect(region, x + shadowTX * e, y + shadowTY * e, regionWidth, regionHeight, rotation - 90);
        Draw.color();
    }
    public void partArmShadow(float xl, float xr){
        float e = elevationScl * defaultElevation;
        float x = 2f * shadowTX * e, y = 2f * shadowTY * e;
        Floor floor = world.floorWorld(x + this.x, y + this.y);
        float dest = floor.canShadow ? 1f : 0f;
        shadowAlpha = shadowAlpha < 0 ? dest : Mathf.approachDelta(shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * shadowAlpha);
        float scale = Math.abs(armLength / xr);
        float dx = armOutlineWidth / scale;
        Fill.quad(x + armNodeX(xl - armOutlineWidth, 0f), y + armNodeY(xl - armOutlineWidth, 0f),
                x + armNodeX(-armOutlineWidth - dx, armLength + armOutlineWidth), y + armNodeY(-armOutlineWidth - dx, armLength + armOutlineWidth),
                x + armNodeX(armOutlineWidth + dx, armLength + armOutlineWidth), y + armNodeY(armOutlineWidth + dx, armLength + armOutlineWidth),
                x + armNodeX(xr + armOutlineWidth, 0f), y + armNodeY(xr + armOutlineWidth, 0f));
        Draw.color();
    }

    public void partSoftShadow(float ax, float ay){
        float sx = ax, sy = ay;
        if(isArm){
            float d = armLength * 0.5f;
            sx += d * Mathf.cosDeg(rotation);
            sy += d * Mathf.sinDeg(rotation);
        }
        Draw.color(0, 0, 0, 0.4f);
        float rad = 1.4f;
        Draw.rect(shadowRegion, sx, sy,
                region.width * region.scl() * rad * 1f,
                (region.height + (isArm ? armLength * 4f : 0f)) * region.scl() * rad * 1f, rotation - 90);
        Draw.color();
    }

    public void partBody(){
        float ax = x + regionOffsetX * Mathf.cosDeg(rotation - 90f) + regionOffsetY * Mathf.cosDeg(rotation);
        float ay = y + regionOffsetX * Mathf.sinDeg(rotation - 90f) + regionOffsetY * Mathf.sinDeg(rotation);
        Draw.z(getPartLayer(Math.max(partLayerOffset, 0f) - (partLayer <= 0.25f ? 0.01f : 0.9f)));
        partShadow(ax, ay);
        Draw.z(getPartLayer(partLayerOffset));
        partSoftShadow(ax, ay);
        Draw.alpha(1f);
        Draw.color();
        type.applyColor(this);
        Draw.rect(region, ax, ay, regionWidth, regionHeight, rotation - 90f);
        if(isArm) partArm();
        Draw.reset();
    }

    public void partArm(){
        float x1 = 0.5f * armWidth * Mathf.cosDeg(armRot - 60f);
        float x2 = 0.5f * armWidth * Mathf.cosDeg(armRot);
        float x3 = 0.5f * armWidth * Mathf.cosDeg(armRot + 60f);
        float x4 = 0.5f * armWidth * Mathf.cosDeg(armRot + 120f);
        float a1 = Mathf.clamp(Math.abs(Mathf.cosDeg(armRot - 60f) - Mathf.cosDeg(armRot)));
        float a2 = Mathf.clamp(Math.abs(Mathf.cosDeg(armRot) - Mathf.cosDeg(armRot + 60f)));
        float a3 = Mathf.clamp(Math.abs(Mathf.cosDeg(armRot + 60f) - Mathf.cosDeg(armRot + 120f)));
        Draw.color(armOutlineColor);
        armQuadOutline(x4, x1);
        Draw.color();
        type.applyColor(this);
        armQuad(x2, x1, armRegion2);
        armQuad(x3, x2, armRegion2);
        armQuad(x4, x3, armRegion2);
        Draw.alpha(a1);
        armQuad(x2, x1, armRegion1);
        Draw.alpha(a2);
        armQuad(x3, x2, armRegion1);
        Draw.alpha(a3);
        armQuad(x4, x3, armRegion1);
        if(armLightProgress > 0) {
            Draw.blend(Blending.additive);
            Draw.color(armLightColor, armLightProgress);
            armQuad(x2, x1);
            armQuad(x3, x2);
            armQuad(x4, x3);
            Draw.blend();
        }
        if(armRingProgress > 0){
            Draw.alpha(1f);
            Draw.color(armRingColor);
            Draw.z(Layer.effect);
            float rx1 = armRingRadius * Mathf.cosDeg(armRot - 60f + armRingGapAngle);
            float rx2 = armRingRadius * Mathf.cosDeg(armRot - armRingGapAngle);
            float rx3 = armRingRadius * Mathf.cosDeg(armRot + armRingGapAngle);
            float rx4 = armRingRadius * Mathf.cosDeg(armRot + 60f - armRingGapAngle);
            float rx5 = armRingRadius * Mathf.cosDeg(armRot + 60f + armRingGapAngle);
            float rx6 = armRingRadius * Mathf.cosDeg(armRot + 120f - armRingGapAngle);
            float w = armRingWidth * armRingProgress;
            armRing(rx2, rx1, w);
            armRing(rx4, rx3, w);
            armRing(rx6, rx5, w);
        }
        Draw.z(getPartLayer(Math.max(partLayerOffset, 0f) - (partLayer <= 0.25f ? 0.01f : 0.9f)));
        partArmShadow(x4, x1);
        Draw.color();
    }

    public void armQuad(float xl, float xr, TextureRegion region){
        float armBit = Color.toFloatBits(Draw.getColor().r, Draw.getColor().g, Draw.getColor().b, Draw.getColor().a);
        Draw.quad(region, armNodeX(xl, 0f), armNodeY(xl, 0f), armBit,
                armNodeX(0f, armLength), armNodeY(0f, armLength), armBit,
                armNodeX(0f, armLength), armNodeY(0f, armLength), armBit,
                armNodeX(xr, 0f), armNodeY(xr, 0f), armBit);
    }
    public void armQuad(float xl, float xr){
        Fill.quad(armNodeX(xl, 0f), armNodeY(xl, 0f),
                armNodeX(0f, armLength), armNodeY(0f, armLength),
                armNodeX(0f, armLength), armNodeY(0f, armLength),
                armNodeX(xr, 0f), armNodeY(xr, 0f));
    }
    public void armQuadOutline(float xl, float xr){
        float scale = Math.abs(armLength / xr);
        float dx = armOutlineWidth / scale;
        Fill.quad(armNodeX(xl - armOutlineWidth, 0f), armNodeY(xl - armOutlineWidth, 0f),
                armNodeX(-armOutlineWidth - dx, armLength + armOutlineWidth), armNodeY(-armOutlineWidth - dx, armLength + armOutlineWidth),
                armNodeX(armOutlineWidth + dx, armLength + armOutlineWidth), armNodeY(armOutlineWidth + dx, armLength + armOutlineWidth),
                armNodeX(xr + armOutlineWidth, 0f), armNodeY(xr + armOutlineWidth, 0f));
    }
    public void armRing(float xl, float xr, float w){
        Fill.quad(armNodeX(xl, -w / 2f), armNodeY(xl, -w / 2f),
                armNodeX(xl, w / 2f), armNodeY(xl, w / 2f),
                armNodeX(xr, w / 2f), armNodeY(xr, w / 2f),
                armNodeX(xr, -w / 2f), armNodeY(xr, -w / 2f));
    }
    public float armNodeX(float nx, float ny){
        return x + (nx + regionOffsetX) * Mathf.cosDeg(rotation - 90f) + (ny + regionOffsetY + armOffset) * Mathf.cosDeg(rotation);
    }
    public float armNodeY(float nx, float ny){
        return y + (nx + regionOffsetX) * Mathf.sinDeg(rotation - 90f) + (ny + regionOffsetY + armOffset) * Mathf.sinDeg(rotation);
    }

    public float getPartLayer(float offset){
        if(partLayer > 0.75f) return Layer.flyingUnit + offset;
        if(partLayer > 0.5f) return Layer.flyingUnitLow + offset;
        if(partLayer > 0.25f) return Layer.legUnit + 6f + offset;
        return Layer.block - 0.75f + offset;
    }

    public void partCharge(){
        if(charges > 0 && chargeTime > 0 && chargeTimeMax > 0){
            Draw.z(Layer.effect - 1f);
            if(chargeDelay > 0 && chargeDelayMax > 0){
                Draw.color(chargeRingDelayColor);
                Lines.stroke(0.5f * chargeRingStroke);
                if(charges > 1) {
                    for(int i = 0; i < charges; i++){
                        Lines.arc(x, y, chargeRingRadius, (chargeDelay / chargeDelayMax) / charges, 360f * ((float) i / charges));
                    }
                }else Lines.arc(x, y, chargeRingRadius, chargeDelay / chargeDelayMax, 0f);
            }else{
                Draw.color(chargeRingColor1);
                Lines.stroke(0.2f * chargeRingStroke);
                if(charges > 1){
                    for(int i = 0; i < charges - interrupts; i++){
                        Lines.arc(x, y, chargeRingRadius * (chargeTime / chargeTimeMax) + chargeRingStroke,
                                0.75f / charges, 360f * ((float) i / charges));
                    }
                }
                addChargeArc(6, 1f, 0.25f, -10f);
                addChargeArc(4, 0.8f, 0.5f, 5f);
                Draw.color(chargeRingColor2);
                addChargeArc(3, 0.6f, 0.5f, 2.5f);
                Draw.color(chargeRingColor3);
                Lines.stroke(0.4f * chargeRingStroke);
                Lines.circle(x, y, chargeRingRadius * (chargeTime / chargeTimeMax));
                if(chargeBright > 0){
                    Draw.color(chargeRingColor1.cpy().lerp(Color.white, chargeBright));
                    Lines.stroke(chargeBright * chargeRingStroke);
                    Lines.circle(x, y, chargeRingRadius * (chargeTime / chargeTimeMax));
                }
            }
            Draw.reset();
        }
    }

    public void addChargeArc(int amount, float stroke, float alpha, float rotSpeed){
        Draw.alpha(alpha);
        Lines.stroke(stroke * chargeRingStroke);
        for(int i = 0; i < amount; i++){
            Lines.arc(x, y, chargeRingRadius * (chargeTime / chargeTimeMax), 0.5f / amount, 360f * ((float) i / amount) + chargeRot * rotSpeed);
        }
    }

    @Override
    public boolean targetable(Team targeter){
        return generalTargetable();
    }

    public boolean generalTargetable(){
        return targetable || (charges > 0 && chargeTime > 0);
    }

    @Override
    public void kill(){
        if(!killable()) return;
        super.kill();
    }

    @Override
    public void rawDamage(float d){
        dealInterrupt(d);
        float amount = d;
        if(!vulnerable && amount > 0) {
            amount = damageReduced(amount, interruptThreshold);
        }
        boolean hadShields = !generalTargetable();
        if (Float.isNaN(health)) health = 0.0F;
        if (hadShields) {
            shieldAlpha = 1.0F;
        }
        hitTime = 1.0F;
        if (amount > 0) {
            health -= amount;
            if(health < 0) health = 0.0F;
        }
    }

    public float damageReduced(float d, float threshold){
        float scale = 0.5f * d / Math.max(threshold, 1f);
        if(scale < 1f) return scale * scale * threshold;
        return (float) ((2 - Math.pow(Math.E, 2 - 2 * scale)) * threshold);
    }

    public void anniWrite(Writes write){
        write.s(1);
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
        write.bool(flip);
        write.bool(ownerDead);
        mindustry.io.TypeIO.writeUnit(write, owner);
    }
    public void anniRead(Reads read){
        short anniVer = read.s();
        if(anniVer >= 1) {
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
            flip = read.bool();
            ownerDead = read.bool();
            owner = mindustry.io.TypeIO.readUnit(read);
        }
    }
    @Override
    public void write(Writes write){
        super.write(write);
        anniWrite(write);
    }
    @Override
    public void read(Reads read){
        super.read(read);
        anniRead(read);
    }
}
