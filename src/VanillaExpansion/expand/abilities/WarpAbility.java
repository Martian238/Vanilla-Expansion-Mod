package VanillaExpansion.expand.abilities;

import VanillaExpansion.expand.effects.WarpParticleEffect;
import arc.Core;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Angles;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.UnitStance;
import mindustry.ai.types.CommandAI;
import mindustry.ai.types.MinerAI;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.part.DrawPart;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.type.StatusEffect;

import static mindustry.Vars.tilesize;

public class WarpAbility extends Ability {
    public WarpAbility() {

    }

    public float warpMinWarmup = 15f;
    public float warpMinWarmupDistance = 800f;
    public float warpBaseWarmup = 15f;
    public float warpCooldown = 30;

    public Seq<DrawPart> warpEndParts = new Seq<>();
    public Effect warpEndEffect = Fx.none;
    public Effect warpBeginEffect = Fx.none;
    public Sound warpSound = Sounds.none;
    public float warpSoundVolume = 2f;
    public float warpShake = 12f;
    public Color warpEffectColor = Color.valueOf("00ffce");
    public Effect warpPrepareEffect = Fx.none;
    public float warpPrepareEffectChance = 0.1f;
    public float warpPrepareEffectSpread = 1f;
    public Color warpTrailColor1 = Color.valueOf("00ffce").a(0.5f);
    public Color warpTrailColor2 = warpTrailColor1.cpy().mul(1.2f);
    public Color warpTrailColor3 = Color.white;
    public float warpTrailStroke = 20f;
    public float warpTrailLife = 30f;
    public StatusEffect warpStatus = StatusEffects.none;
    public boolean useAutoWarpEffect = true;

    /** 被指挥时是否自动跃迁 */
    public boolean commandAutoWarp = true;
    /** 任何情况下移动目标超过此距离自动使用跃迁 */
    public float commandAutoWarpDistance = 480f;
    /** 能否用助推姿态触发指挥跃迁 */
    public boolean commandAutoWarpBoost = true;
    /** 助推姿态下，移动目标超过此距离自动使用跃迁 */
    public float commandAutoWarpBoostDistance = 160f;
    /** 能否用载荷指令触发指挥跃迁 */
    public boolean commandAutoWarpPayload = true;
    /** 载荷指令下，移动目标超过此距离自动使用跃迁 */
    public float commandAutoWarpPayloadDistance = 80f;
    /** 能否用辅助指令触发指挥跃迁 */
    public boolean commandAutoWarpSupport = true;
    /** 辅助指令下，移动目标超过此距离自动使用跃迁 */
    public float commandAutoWarpSupportDistance = 120f;
    /** 移动端是否自动跃迁 */
    public boolean mobileAutoWarp = true;
    /** 在移动端，作为玩家的单位离镜头超过此距离自动使用跃迁 */
    public float mobileAutoWarpDistance = 240f;
    /** 在移动端，镜头停止移动超过此时间才能使用跃迁 */
    public float mobileAutoWarpTime = 30f;

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(abilityStat("warpwarmup", Strings.autoFixed(warpMinWarmup / 60f, 2), Strings.autoFixed(warpMinWarmupDistance / tilesize, 2)));
        t.row();
        if(warpBaseWarmup > 0) {
            t.add(abilityStat("warpbasewarmup", Strings.autoFixed(warpBaseWarmup / 60f, 2)));
            t.row();
        }
        t.add(abilityStat("warpcooldown", Strings.autoFixed(warpCooldown / 60f, 2)));
    }

    private final KeyCode warpKey = KeyCode.altLeft;
    private final KeyCode warpBanKey = KeyCode.o;
    private float warpWarmup = 0, warpCooldownTimer = 0;
    private float wx, wy, wRot;
    private boolean rotAllowWarp = false, warpCharging = true, autoWarping = false;
    private float mobileAutoWarpTimer, mobileCamXLast, mobileCamYLast;

    public boolean tryingWarp(Unit unit){
        autoWarping = false;
        if(Vars.mobile && mobileAutoWarp && unit.isPlayer() && mobileAutoWarpTimer >= mobileAutoWarpTime){
            if(Mathf.dst2(unit.x, unit.y, Core.camera.position.x, Core.camera.position.y) >= mobileAutoWarpDistance * mobileAutoWarpDistance){
                wx = Core.camera.position.x;
                wy = Core.camera.position.y;
                autoWarping = true;
                return true;
            }
        }
        if(!unit.isPlayer() && commandAutoWarp && unit.command().targetPos != null){
            CommandAI c = unit.command();
            boolean boosting = commandAutoWarpBoost && c.hasStance(UnitStance.boost);
            boolean payloadActing = commandAutoWarpPayload &&
                    c.command == UnitCommand.loopPayloadCommand ||
                    c.command == UnitCommand.loadUnitsCommand ||
                    c.command == UnitCommand.loadBlocksCommand ||
                    c.command == UnitCommand.unloadPayloadCommand;
            boolean supportActing = commandAutoWarpSupport &&
                    c.command == UnitCommand.mineCommand ||
                    c.command == UnitCommand.rebuildCommand ||
                    c.command == UnitCommand.assistCommand ||
                    c.command == UnitCommand.repairCommand;
            float d = Mathf.dst(unit.command().targetPos.getX(), unit.command().targetPos.getY(), unit.x, unit.y);
            boolean shouldWarp = (boosting && d > commandAutoWarpBoostDistance) ||
                    (payloadActing && d > commandAutoWarpPayloadDistance) ||
                    (supportActing && d > commandAutoWarpSupportDistance) ||
                    (d > commandAutoWarpDistance);
            //useless for now
            if(unit.controller() instanceof MinerAI m){
                if(m.ore != null){
                    wx = m.ore.worldx();
                    wy = m.ore.worldy();
                    autoWarping = true;
                    return true;
                }
            }
            if(shouldWarp){
                wx = unit.command().targetPos.getX();
                wy = unit.command().targetPos.getY();
                autoWarping = true;
                return true;
            }
        }
        return unit.isPlayer() && (Core.input.keyDown(warpKey) && !Core.input.keyDown(warpBanKey)) && !unit.isShooting() && warpCooldownTimer <= 0;
    }

    @Override
    public void update(Unit unit) {
        if(commandAutoWarpBoost && !unit.type.stances.contains(UnitStance.boost)) unit.type.stances.add(UnitStance.boost);
        if(Vars.mobile && mobileAutoWarp){
            if(mobileCamXLast != Core.camera.position.x || mobileCamYLast != Core.camera.position.y){
                mobileAutoWarpTimer = 0;
            }else{
                mobileAutoWarpTimer += Time.delta;
            }
            mobileCamXLast = Core.camera.position.x;
            mobileCamYLast = Core.camera.position.y;
        }
        if(tryingWarp(unit)) {
            warpCharging = true;
            warpWarmup += Time.delta;
            if(!autoWarping) {
                wx = Math.min(Math.max(Core.input.mouseWorldX(), 0), Vars.state.map.width * tilesize);
                wy = Math.min(Math.max(Core.input.mouseWorldY(), 0), Vars.state.map.height * tilesize);
            }
            wRot = Mathf.atan2(wx - unit.x, wy - unit.y) * Mathf.radiansToDegrees;
            if(Mathf.chanceDelta(warpPrepareEffectChance)) {
                warpPrepareEffect.at(unit.x + Mathf.range(warpPrepareEffectSpread), unit.y + Mathf.range(warpPrepareEffectSpread));
            }
            rotateToWarp(unit);
            if(warpStatus != StatusEffects.none){
                unit.apply(warpStatus, 2f);
            }
            float d = Mathf.dst(unit.x, unit.y, wx, wy);
            if(rotAllowWarp && warpWarmup >= warpMinWarmup * (d / warpMinWarmupDistance) + (warpBaseWarmup > 0 ? warpBaseWarmup : 0f)) {
                warp(unit);
            }
        }else{
            warpWarmup = 0;
            warpCharging = false;
        }
        if(warpCooldownTimer > 0){
            warpCooldownTimer -= Time.delta;
            warpCharging = false;
        }
    }

    public void warp(Unit unit){
        warpCooldownTimer = warpCooldown;
        float x1 = unit.x, y1 = unit.y;
        float x2 = wx, y2 = wy;
        float r = wRot;
        float d = Mathf.dst(x1, y1, x2, y2);
        unit.x(x2);
        unit.y(y2);
        warpBeginEffect.at(x1, y1, r, warpEffectColor);
        warpEndEffect.at(x2, y2, r, warpEffectColor);
        Effect line = new Effect(warpTrailLife, 80000f, e -> {
            Drawf.light(x1, y1, x2, y2, warpTrailStroke * e.fout(), warpEffectColor, 1f);
            Lines.stroke(warpTrailStroke * e.fout());
            Draw.color(warpTrailColor1);
            Lines.line(x1, y1, x2, y2);
            Lines.stroke(warpTrailStroke * e.fout() * 0.6f);
            Draw.color(warpTrailColor2);
            Lines.line(x1, y1, x2, y2);
            Lines.stroke(warpTrailStroke * e.fout() * 0.3f);
            Draw.color(warpTrailColor3);
            Lines.line(x1, y1, x2, y2);
        });
        line.at(x1, y1);
        if(useAutoWarpEffect){
            WarpParticleEffect wp = new WarpParticleEffect(){{
                generalLength = d;
                lengthFrom = Math.min(20f, d / 36f);
                lengthTo = Math.min(5f, d / 144f);
                stroke = 2f;
                colorFrom = warpEffectColor;
                colorTo = warpEffectColor;
                lifetime = 60f;
                particles = Mathf.ceil(d / 14.4f);
                width = unit.hitSize() * 0.8f;
            }};
            wp.at(x1, y1, r);
        }
        warpSound.at(x1, y1, 1f, warpSoundVolume * Math.max(0.33f, d / warpMinWarmupDistance));
        warpSound.at(x2, y2, 1f, warpSoundVolume * Math.max(0.33f, d / warpMinWarmupDistance));
        Effect.shake(warpShake, warpShake * 2f, x1, y1);
        Effect.shake(warpShake, warpShake * 2f, x2, y2);
    }

    public void rotateToWarp(Unit unit){
        rotAllowWarp = false;
        if(Angles.angleDist(wRot, unit.rotation()) < unit.type.rotateSpeed * unit.speedMultiplier() * Time.delta){
            unit.rotation(wRot);
            rotAllowWarp = true;
            return;
        }
        if(Mathf.sinDeg(wRot - unit.rotation()) > 0){
            unit.rotation(unit.rotation() + unit.type.rotateSpeed * unit.speedMultiplier() * Time.delta);
        }else{
            unit.rotation(unit.rotation() - unit.type.rotateSpeed * unit.speedMultiplier() * Time.delta);
        }
    }

    @Override
    public void draw(Unit unit){
        if(warpCharging){
            if(warpEndParts.size > 0){
                float prog = Mathf.clamp(warpWarmup / warpMinWarmup);
                DrawPart.params.set(prog, prog, prog, prog, prog, prog, wx, wy, wRot);
                DrawPart.params.life = prog;
                for(DrawPart p : warpEndParts){
                    p.draw(DrawPart.params);
                }
            }
        }
    }
}
