package VanillaExpansion.expand.world.block.crux;

import VanillaExpansion.content.CustomFx;
import VanillaExpansion.ui.VEFonts;
import arc.Core;
import arc.audio.Sound;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Align;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.SoundEffect;
import mindustry.entities.effect.WaveEffect;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;
import mindustry.world.blocks.LaunchAnimator;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;


import java.util.Arrays;

import static mindustry.Vars.*;

public class ShockwaveCoreBlock extends CoreBlock {




    public float shockwaveShake = 35f;
    public float shockwaveShakeGlobal = 15f;

    public Sound hitUnitSound = Sounds.explosionPlasmaSmall;
    public float hitUnitShakeBase = 12f;
    public Sound hitBlockSound = Sounds.explosion;
    public float hitBlockShakeBase = 8f;
    public Effect hitBlockEffect = Fx.circleColorSpark;
    public Effect hitBlockSplashEffect = CustomFx.shockwaveSparks;
    public Effect hitTerrainSplashEffect = CustomFx.shockwaveSparksSmall;

    public float chargeDuration = 420f;
    public float shockwaveDuration = 90f;
    public float maxWarmup = 60f;

    public Sound chargeSound = Sounds.none;
    public float chargeSoundDuration = 420f;
    public float chargeVolumeFrom = 0f;
    public float chargeVolumeTo = 1f;
    public float chargeVolumeGlobalFrom = 0f;
    public float chargeVolumeGlobalTo = 0.2f;

    public Sound shockwaveSound = Sounds.drillImpact;
    public Effect shockwaveEffect = Fx.none;
    public Effect shockwaveGroundEffect = new MultiEffect(
            new WaveEffect(){{
                sides = 4;
                rotation = 45;
                sizeFrom = sizeTo = 20f * 1.414f;
                colorFrom = colorTo = Color.valueOf("f25555");
                lifetime = 60;
                strokeFrom = 4f;
                strokeTo = 0f;
            }},
            new SoundEffect(){{
                    effect = CustomFx.shockwaveHitGround;
                    sound = Sounds.shootBeamPlasma;
                    maxPitch = 1f;
                }}
    );
    public @Nullable Color genericColor;
    public Effect finalChargeEffect = Fx.none;
    public float finalCharge = 30f;

    public Effect chargeEffect1 = Fx.none;
    public float chargeEffect1ChanceBase = 0.25f;
    public float chargeEffect1End = 60f;
    public Effect chargeEffect2 = Fx.none;
    public float chargeEffect2Chance = 0.25f;
    public Effect chargeEffect3 = Fx.none;
    public float chargeEffect3Chance = 0.1f;
    public Sound warnSound = Sounds.none;

    public float selfHealPercent = 10;

    /** 为 true 时，护盾保护范围不再覆盖后方的整个地图区域，而是仅限于护盾范围和护盾后方一块较小区域 */
    public boolean safeAreaLimited = true;



    public ShockwaveCoreBlock(String name) {
        super(name);
        clipSize = 80000f;
    }

    public DrawBlock drawer = new DrawDefault();

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }



    public class ShockwaveCoreBuild extends CoreBuild implements LaunchAnimator{

        private float[] safeAngles = new float[3600];
        private float[] safeDistance = new float[3600];
        private float[][] safeTiles = new float[3600][800];

        private float shockwaveRadius = 6400;//tilesize * getDistance(0, 0, (float) state.map.width,  (float) state.map.height);

        private Color effectColor = genericColor == null? team.color : genericColor;

        public WaveEffect shockwaveEffectGlobal = new WaveEffect(){{
            sides = 96;
            strokeFrom = 60f;
            strokeTo = 60f;
            sizeFrom = 1;
            sizeTo = 6400f;
            colorFrom = Color.valueOf("f25555a0");
            colorTo = Color.valueOf("f25555a0");
            layer = Layer.flyingUnit + 4f;
            interp = Interp.pow2In;
            lifetime = shockwaveDuration * 1.414f;
            clip = 8000;
        }};

        @Override
        public void draw(){
            //draw thrusters when just landed

            drawer.draw(this);
            if(thrusterTime > 0){
                float frame = thrusterTime;

                Draw.alpha(1f);
                drawThrusters(frame);
                Draw.rect(block.region, x, y);
                Draw.alpha(Interp.pow4In.apply(frame));
                drawThrusters(frame);
                Draw.reset();

                drawTeamTop();
            }else{


                if(enabled && !state.isEditor() && Vars.ui.hudfrag.shown){
                    drawChargeBar();
                    Drawf.light(x, y, 80f, team.color, 1f);
                }
            }
        }



        private float shockwaveWarmup = 0f, shockwaveCharge = 0f;
        private static ShockwaveCoreBuild core;
        private static ShockwaveCoreBlock coreBlock;
        private Vec2 corePos = new Vec2(x, y);


        @Override
        public void updateTile(){
            iframes -= Time.delta;
            thrusterTime -= Time.delta/90f;

            if(enabled && !state.isEditor() && !(thrusterTime > 0)){
                if(shockwaveWarmup < maxWarmup) {
                    shockwaveWarmup += 1;
                    if(shockwaveWarmup < 0) shockwaveWarmup = 0;
                }

                shockwaveCharge += 1;
                if(finalCharge > 0 && shockwaveCharge == chargeDuration - finalCharge){
                    finalChargeEffect.at(x, y);
                }
                if(shockwaveCharge >= chargeDuration) {
                    shockwaveRadius = 3200f;//tilesize * getDistance(0, 0, (float) state.map.width,  (float) state.map.height);
                    //Log.info("Shockwave radius: " + shockwaveRadius);
                    shockwaveRelease();
                    shockwaveCharge = - shockwaveDuration;
                }else if(shockwaveCharge > 0){
                    if(shockwaveCharge < chargeDuration - chargeEffect1End) {
                        if (Mathf.chance(Mathf.lerp(chargeEffect1ChanceBase, 1f, shockwaveCharge / chargeDuration))) {
                            chargeEffect1.at(x, y);
                        }
                    }
                    if(Mathf.chance(Mathf.lerp(0f, chargeEffect2Chance, shockwaveCharge / chargeDuration))){
                        chargeEffect2.at(x, y, Mathf.range(180f));
                    }
                    if(Mathf.chance(Mathf.lerp(0f, chargeEffect3Chance, shockwaveCharge / chargeDuration))){
                        chargeEffect3.at(x, y, Mathf.range(180f));
                    }
                }

                if(shockwaveCharge > chargeDuration - chargeSoundDuration) {
                    corePos.x = x;
                    corePos.y = y;
                    Vars.control.sound.loop(chargeSound, corePos, Mathf.lerp(chargeVolumeFrom, chargeVolumeTo, Mathf.clamp((shockwaveCharge - chargeDuration + chargeSoundDuration) / chargeSoundDuration)));
                    Vars.control.sound.loop(chargeSound, Mathf.lerp(chargeVolumeGlobalFrom, chargeVolumeGlobalTo, Mathf.clamp((shockwaveCharge - chargeDuration + chargeSoundDuration) / chargeSoundDuration) * Mathf.clamp((shockwaveCharge - chargeDuration + chargeSoundDuration) / chargeSoundDuration) * Mathf.clamp((shockwaveCharge - chargeDuration + chargeSoundDuration) / chargeSoundDuration)));
                }
            }else{
                if(shockwaveWarmup > 0) {
                    shockwaveWarmup -= 1;
                    if(shockwaveWarmup > maxWarmup) shockwaveWarmup = maxWarmup;
                }

                shockwaveCharge = 0f;
            }
        }




        public void shockwaveRelease(){
            shockwaveSound.at(Core.camera.position, 1f, 2f);
            shockwaveSound.at(x, y, 1f, 4f);
            shockwaveEffect.at(x, y);
            shockwaveEffectGlobal.at(x, y);
            shockwaveGroundEffect.at(x, y);
            Effect.shake(shockwaveShake, shockwaveShake * 2.5f, x, y);
            Effect.shake(shockwaveShakeGlobal, shockwaveShakeGlobal * 5, Core.camera.position);
            if(selfHealPercent > 0 && health < block.health){
                health += block.health * selfHealPercent / 100;
                if(health > block.health) health = block.health;
            }
            core = this;
            coreBlock = (ShockwaveCoreBlock) this.block;
            checkSafeArea();
            Units.nearby(0, 0, state.map.width * tilesize, state.map.height * tilesize, unitConsumer);
            damageBuildings();
            dustEffects();
        }

        public void dustEffects(){
            for(int i = 0; i < state.map.height * state.map.width / 100; i++){
                float ix = Math.abs(Mathf.range(0, tilesize * state.map.width));
                float iy = Math.abs(Mathf.range(0, tilesize * state.map.height));
                float id = core.getDistance(core.x, core.y, ix, iy);
                Floor floor = world.floorWorld(ix, iy);
                if(!floor.isLiquid) {
                    Time.run((float) (Math.sqrt(id / core.shockwaveRadius) * (coreBlock.shockwaveDuration)), () -> {
                        Fx.breakProp.at(ix, iy, Math.abs(Mathf.range(2f)) + 2f, floor.mapColor);
                        coreBlock.hitTerrainSplashEffect.at(ix, iy, Mathf.radiansToDegrees * Mathf.atan2(ix - core.x, iy - core.y), core.team.color);
                    });
                }else{
                    Time.run((float) (Math.sqrt(id / core.shockwaveRadius) * (coreBlock.shockwaveDuration)), () -> {
                        Sounds.stepWater.at(ix, iy);
                        Fx.hitLiquid.at(ix, iy, Math.abs(Mathf.range(2f)) + 2f,  floor.liquidDrop.color);
                    });
                }
            }
        }






        public void checkSafeArea(){
            Arrays.fill(core.safeAngles, -361f);
            Arrays.fill(core.safeDistance, -1f);
            for(int j = 0; j < core.safeTiles.length; j++ ){
                Arrays.fill(safeTiles[j], -1f);
            }
            for(Building b : Groups.build){
                if(b.block instanceof BaseShield bsb && b.team != core.team && b.efficiency > 0.1f){
                    float r = Math.max(bsb.radius, b.block.size * tilesize * 0.707f);
                    float d = Math.max(r + tilesize, core.getDistance(b.x, b.y, core.x, core.y));
                    float dir = core.angleStandardize(Mathf.radiansToDegrees * Mathf.atan2(b.x - core.x, b.y - core.y));
                    float angle = (float) (Mathf.radiansToDegrees * Math.asin(r / d));
                    float dirFrom = core.angleStandardize(dir - angle);
                    float dirTo = core.angleStandardize(dir + angle);
                    for(int i = 0; i < 3600; i++){
                        float dirI = i * 0.1f;
                        if((dirI >= dirFrom && dirI <= dirTo) || (dirFrom > dirTo && (dirI >= dirFrom || dirI <= dirTo))){
                            core.safeAngles[i] = dirI;
                            float angleDist = Angles.angleDist(dir, dirI);
                            float sinB = Mathf.sinDeg(angleDist) * d / r;
                            float angleB = (float) (180f - (Math.asin(sinB) * Mathf.radiansToDegrees));
                            float angleC = 180f - angleB - angleDist;
                            float safeD = d * Mathf.sinDeg(angleC) / sinB;
                            if(safeD > 40f) {
                                Time.run((float) (Math.sqrt(safeD / core.shockwaveRadius) * (coreBlock.shockwaveDuration)),
                                        () -> {
                                            coreBlock.hitTerrainSplashEffect.at(Mathf.cosDeg(dirI) * safeD + core.x, Mathf.sinDeg(dirI) * safeD + core.y, dirI, b.team.color);
                                        });
                            }
                            if(!coreBlock.safeAreaLimited) {
                                if (core.safeDistance[i] > safeD || core.safeDistance[i] <= 0) {
                                    core.safeDistance[i] = safeD;
                                }
                            }else{
                                float safeRange = 0f;
                                if(bsb.radius >= b.block.size * tilesize * 0.707f) {
                                    float roundAngle = 180f - 2 * (180f - angleB);
                                    float rountCutLength = (float) Math.sqrt((2 - 2 * Mathf.cosDeg(roundAngle)) * r * r);
                                    safeRange = 2 * rountCutLength;
                                }else{
                                    safeRange = 80f * b.block.size + b.block.size * tilesize;
                                }
                                int iiiR = 0;
                                for(int iii = 0; true; iii += 1){
                                    //Log.info(core.safeTiles[i][iii]);
                                    if (iii >= 798) {
                                        iiiR = -1;
                                        break;
                                    }else if (core.safeTiles[i][iii] <= 0) {
                                        iiiR = iii;
                                        break;
                                    }
                                }
                                //Log.info(dirI + " " + safeRange + " " + iiiR);
                                if(iiiR >= 0){
                                    core.safeTiles[i][iiiR] = safeD;
                                    core.safeTiles[i][iiiR + 1] = safeD + safeRange;
                                }
                            }

                            //Log.info("Safe added: " + dirI + ", " + safeD + ", B=" + angleB + ", C=" + angleC + ", Dist=" + angleDist + ", sinB=" + sinB);
                        }
                    }
                }
            }
        }

        public float angleStandardize(Float a){
            if(a >= 360) return a - 360;
            return a < 0 ? a + 360f : a;
        }

        @Override
        public float warmup(){
            return Mathf.clamp(shockwaveWarmup / maxWarmup);
        }

        @Override
        public float progress(){
            return 1 - Mathf.clamp(shockwaveCharge / chargeDuration);
        }

        public static final Cons<Unit> unitConsumer = u -> {
            if(!core.isProtected(u) && !u.dead()){
                float d = core.getDistance(core.x, core.y, u.x, u.y);
                WaveEffect killEffect = new WaveEffect(){{
                    sizeFrom = sizeTo = (float) (u.type.hitSize * 1.4);
                    sides = 4;
                    colorFrom = Color.valueOf("f25555");
                    colorTo = Color.valueOf("f2555500");
                    lifetime = (float) (Math.sqrt(d / core.shockwaveRadius) * (coreBlock.shockwaveDuration));
                    strokeFrom = strokeTo = (float) (2 * Math.min(3.5f, 0.5f * Math.sqrt(u.type.hitSize / tilesize)));
                }};
                killEffect.at(u.x, u.y);
                coreBlock.warnSound.at(u.x, u.y);
                Time.run((float) (Math.sqrt(d / core.shockwaveRadius) * (coreBlock.shockwaveDuration)),
                        () -> {
                    coreBlock.hitUnitSound.at(u.x, u.y, 1f, Mathf.clamp(u.hitSize / 40));
                    Effect.shake(coreBlock.hitUnitShakeBase * u.hitSize / tilesize, u.hitSize * 2, u.x, u.y);
                    if(u.type.killable) {
                        u.damage(1f);
                        u.vel.x = u.vel.x + ((u.x - core.x) / d) * Mathf.clamp(128 / u.hitSize) * 6f;
                        u.vel.y = u.vel.y + ((u.y - core.y) / d) * Mathf.clamp(128 / u.hitSize) * 6f;
                        u.health(0f);
                    }else{
                        u.remove();
                    }
                });
            }
        };

        public void specialKill(Unit u){
            for(Unit ug : Groups.unit){
                if(ug == u){

                }
            }
        }

        public boolean isProtected(Unit u){
            if(u.team == core.team) return true;
            if(u.type.hidden && !u.isMissile()) return true;
            int t = u.type.hitSize <= 8? 1 : (int) Math.floor(u.type.hitSize / 8);
            for(int i = 0; i < t; i++){
                float dx = Mathf.range(u.type.hitSize / 2);
                float dy = Mathf.range(u.type.hitSize / 2);
                if(i == 0){
                    dx = dy = 0;
                }
                float d = getDistance(u.x + dx, u.y + dy, core.x, core.y);
                float au = Mathf.radiansToDegrees * Mathf.atan2(u.x + dx - core.x, u.y + dy - core.y);
                au = core.angleStandardize(au);
                //Log.info("Unit point angle: " + au);
                boolean b1 = false;
                for(int ii = 0; ii < 3600; ii++){
                    if(Angles.angleDist(core.safeAngles[ii], au) < 0.1f){
                        if(!coreBlock.safeAreaLimited) {
                            if (core.safeDistance[ii] <= d && core.safeDistance[ii] > 0) {
                                b1 = true;
                            }
                        }else{
                            if(core.safeTiles[ii][0] > 0) {
                                for (int iii = 0; true; iii += 2) {
                                    if (iii >= 798 || core.safeTiles[ii][iii] <= 0) {
                                        break;
                                    } else if (d >= core.safeTiles[ii][iii] && d <= core.safeTiles[ii][iii + 1]) {
                                        b1 = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if(!b1) return false;
            }
            return true;
        }

        public void damageBuildings(){
            for(Building b : Groups.build){
                if(b.team != core.team && !(b.block instanceof CoreBlock)){
                    if(!core.isProtectedBuilding(b) || b.block instanceof BaseShield){
                        float d = core.getDistance(core.x, core.y, b.x, b.y);
                        if(!(b.block instanceof BaseShield && b.efficiency > 0.1f)) {
                            WaveEffect killEffect = new WaveEffect() {{
                                sizeFrom = sizeTo = (float) (b.block.size * tilesize);
                                sides = 4;
                                rotation = 45;
                                colorFrom = Color.valueOf("f25555");
                                colorTo = Color.valueOf("f2555500");
                                lifetime = (float) (Math.sqrt(d / core.shockwaveRadius) * (coreBlock.shockwaveDuration));
                                strokeFrom = strokeTo = 0.5f * (float) (2 * Math.min(4, Math.sqrt(b.block.size)));
                            }};
                            killEffect.at(b.x, b.y);
                        }
                        Time.run((float) (Math.sqrt(d / core.shockwaveRadius) * (coreBlock.shockwaveDuration)), () -> {
                            coreBlock.hitBlockSound.at(b.x, b.y, 1f, 0.67f * b.block.size);
                            Effect.shake(coreBlock.hitBlockShakeBase * b.block.size * Mathf.clamp(1 - d / shockwaveRadius + 0.125f), coreBlock.hitBlockShakeBase * b.block.size * 2, b.x, b.y);
                            if(b.block instanceof BaseShield bs && b.efficiency > 0.1f) {
                                b.damagePierce(Math.min(b.block.health * 0.5f, b.block.health * 0.1f + 5000f * Mathf.clamp(1 - d / shockwaveRadius)));
                                WaveEffect blockEffect = new WaveEffect(){{
                                    sides = bs.sides;
                                    sizeFrom = bs.radius;
                                    sizeTo = bs.radius * 1.25f + tilesize;
                                    strokeFrom = Math.min(8f, 4f * bs.radius / 14);
                                    strokeTo = 0f;
                                    colorFrom = colorTo = b.team.color;
                                    clip = bs.radius * 2;
                                    lifetime = 90f;
                                }};
                                blockEffect.at(b.x, b.y);
                            }else{
                                coreBlock.hitBlockEffect.at(b.x, b.y, 0f, core.team.color);
                                coreBlock.hitBlockSplashEffect.at(b.x, b.y, Mathf.radiansToDegrees * Mathf.atan2(b.x - core.x, b.y - core.y), core.team.color);
                                b.damagePierce(b.block.health * 0.1f + 5000f * Mathf.clamp(1 - d / shockwaveRadius));
                            }
                        });
                    }
                }
            }
        }

        public boolean isProtectedBuilding(Building b){
                float d = getDistance(b.x, b.y, core.x, core.y);
                float au = Mathf.radiansToDegrees * Mathf.atan2(b.x - core.x, b.y - core.y);
                au = core.angleStandardize(au);
                boolean b1 = false;
                for(int ii = 0; ii < 3600; ii++){
                    if(Angles.angleDist(core.safeAngles[ii], au) < 0.1f){
                        if(!safeAreaLimited) {
                            if (core.safeDistance[ii] <= d && core.safeDistance[ii] > 0) {
                                b1 = true;
                            }
                        }else{
                            if(core.safeTiles[ii][0] > 0) {
                                for (int iii = 0; true; iii += 2) {
                                    if (core.safeTiles[ii][iii] <= 0 || iii >= 798) {
                                        break;
                                    } else if (d >= core.safeTiles[ii][iii] && d <= core.safeTiles[ii][iii + 1]) {
                                        b1 = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if(!b1) return false;
            return true;
        }

        public float getDistance(float ax, float ay, float bx, float by){
            return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
        }


        public void drawChargeBar(){
            Draw.color(Color.black);
            Draw.alpha(1f);
            Draw.z(Layer.end - 10f);
            float w = Core.camera.width * 0.5f;
            float fs = Core.camera.width * 0.0001f;
            float h = Core.camera.height * 0.0246f;
            float dy = Core.camera.height * 0.425f;
            float thick = Core.camera.width * 0.005f;
            Fill.rect(Core.camera.position.x, Core.camera.position.y + dy, w, h);
            Draw.alpha(0.5f);
            Fill.rect(Core.camera.position.x, Core.camera.position.y + dy, w + thick * 2, h + thick * 2);
            Draw.color(Color.valueOf("f25555"));
            Draw.alpha(1f);
            Draw.z(Layer.end - 9.9f);
            Fill.rect(Core.camera.position.x - 0.5f * w * (progress()), Core.camera.position.y + dy, w * (1 - progress()), h);
            if(shockwaveCharge < 0){
                Draw.color(Color.valueOf("f25555").mul(2 -(shockwaveCharge + shockwaveDuration) / shockwaveDuration));
                Draw.alpha(1 -(shockwaveCharge + shockwaveDuration) / shockwaveDuration);
                Draw.z(Layer.end - 9.8f);
                Fill.rect(Core.camera.position.x, Core.camera.position.y + dy, w, h);
            }
            //Font font = VEFonts.novo;

            //font.draw("TEST", Core.camera.position.x,  Core.camera.position.y + dy, Color.white, fs, true, 1);
        }
    }
}
