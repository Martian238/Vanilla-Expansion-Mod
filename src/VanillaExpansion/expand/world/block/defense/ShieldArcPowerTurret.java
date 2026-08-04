package VanillaExpansion.expand.world.block.defense;

import arc.Core;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;


public class ShieldArcPowerTurret extends PowerTurret {

    // ========== 配置参数 ==========
    public float shieldRadius = 60f;
    public float shieldMax = 200f;
    public float shieldRegen = 0.1f;
    public float shieldCooldown = 60f * 5;
    public float shieldAngle = 80f;
    public float shieldWidth = 6f;
    public float shieldAngleOffset = 0f;
    public boolean whenShooting = true;
    public float chanceDeflect = -1f;
    public boolean drawArc = true;
    public @Nullable Color shieldColor;
    public boolean pushUnits = true;

    // ========== 构造函数 ==========
    public ShieldArcPowerTurret(String name) {
        super(name);
        // 确保炮台有电力消耗（如果是PowerTurret的话）
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.shieldHealth, shieldMax, StatUnit.none);
        stats.add(Stat.regenerationRate, shieldRegen * 60f, StatUnit.perSecond);
    }

    // ========== 自定义 Build 类 ==========
    public class ShieldArcTurretBuild extends PowerTurretBuild {
        public float shieldData = shieldMax;
        public float shieldAlpha = 0f;
        public float widthScale = 0f;
        public final Vec2 shieldPos = new Vec2();
        public boolean shieldBroken = false;
        public float cooldownTimer = 0f;

        @Override
        public void updateTile(){
            // 1. 先调用父类更新（处理射击等）
            super.updateTile();

            // 2. 更新护盾数据
            if(shieldBroken){
                cooldownTimer += Time.delta;
                if(cooldownTimer >= shieldCooldown){
                    shieldBroken = false;
                    shieldData = shieldMax * 0.5f; // 恢复一半
                    cooldownTimer = 0f;
                }
                // 护盾破碎期间不恢复
                widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
                return;
            }

            // 恢复护盾
            if(shieldData < shieldMax){
                shieldData += Time.delta * shieldRegen;
                if(shieldData > shieldMax) shieldData = shieldMax;
            }

            // 判断护盾是否激活
            boolean hasPower = power != null && power.status > 0.1f;
            boolean active = shieldData > 0.1f && (isActive() || !whenShooting) && hasPower;

            if(active){
                widthScale = Mathf.lerpDelta(widthScale, 1f, 0.06f);
                shieldAlpha = Mathf.lerpDelta(shieldAlpha, 1f, 0.02f);

                // 计算护盾位置
                shieldPos.set(0, 0).rotate(rotation - 90f).add(this);

                // 拦截子弹
                float reach = shieldRadius + shieldWidth;
                Groups.bullet.intersect(
                        shieldPos.x - reach, shieldPos.y - reach,
                        reach * 2f, reach * 2f,
                        b -> {
                            if(b.team != team && b.type.absorbable && shieldData > 0.1f){
                                // 检查是否在护盾弧范围内
                                float angleToBullet = shieldPos.angleTo(b);
                                if(Angles.within(angleToBullet, rotation + shieldAngleOffset, shieldAngle / 2f)){
                                    // 吸收子弹
                                    b.absorb();
                                    Fx.absorb.at(b);
                                    shieldData -= b.damage();

                                    // 护盾破碎
                                    if(shieldData <= 0){
                                        shieldData = 0;
                                        shieldBroken = true;
                                        cooldownTimer = 0f;
                                        Fx.arcShieldBreak.at(shieldPos.x, shieldPos.y, 0,
                                                shieldColor == null ? team.color : shieldColor, this);
                                    }
                                }
                            }
                        }
                );

                // 推送单位（可选）
                if(pushUnits){
                    Units.nearbyEnemies(team, shieldPos.x - reach, shieldPos.y - reach, reach * 2f, reach * 2f, u -> {
                        if(u.within(shieldPos, shieldRadius + shieldWidth)){
                            float angleToUnit = shieldPos.angleTo(u);
                            if(Angles.within(angleToUnit, rotation + shieldAngleOffset, shieldAngle / 2f)){
                                // 推开单位
                                float overlap = (shieldRadius + shieldWidth) - u.dst(shieldPos);
                                if(overlap > 0){
                                    u.move(Tmp.v1.set(u).sub(shieldPos).setLength(overlap + 1f));
                                }
                            }
                        }
                    });
                }
            }else{
                widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
                shieldAlpha = Mathf.lerpDelta(shieldAlpha, 0f, 0.02f);
            }
        }

        @Override
        public void draw(){
            // 1. 先绘制炮台本身
            super.draw();

            // 2. 绘制护盾（仅当宽度和透明度都大于阈值）
            if(widthScale > 0.01f && shieldAlpha > 0.01f && shieldData > 0.1f){
                Draw.z(Layer.shields);
                Color shieldCol = shieldColor == null ? team.color : shieldColor;
                Draw.color(shieldCol, shieldAlpha * 0.6f);

                // 计算护盾位置
                shieldPos.set(0, 0).rotate(rotation - 90f).add(this);

                if(drawArc){
                    Lines.stroke(shieldWidth * widthScale);
                    // 从 -angle/2 到 +angle/2
                    float startAngle = rotation + shieldAngleOffset - shieldAngle / 2f;
                    Lines.arc(shieldPos.x, shieldPos.y, shieldRadius, shieldAngle / 360f, startAngle);
                }

                // 绘制护盾内部填充（可选）
                if(shieldAlpha > 0.3f){
                    Draw.alpha(0.08f * shieldAlpha * widthScale);
                    Fill.circle(shieldPos.x, shieldPos.y, shieldRadius);
                }

                Draw.reset();
            }
        }

        @Override
        public void displayBars(Table table){
            super.displayBars(table);

            // 护盾值条
            table.add(new Bar(
                    () -> Core.bundle.format("bar.shieldhealth",
                            Strings.autoFixed(shieldData, 1),
                            Strings.autoFixed(shieldMax, 1)),
                    () -> shieldBroken ? Pal.gray : Pal.accent,
                    () -> shieldData / shieldMax
            )).row();

            // 护盾状态条（如果破碎显示冷却）
            if(shieldBroken){
                table.add(new Bar(
                        () -> Core.bundle.format("bar.shieldcooldown",
                                Strings.autoFixed((shieldCooldown - cooldownTimer) / 60f, 1)),
                        () -> Pal.remove,
                        () -> cooldownTimer / shieldCooldown
                )).row();
            }
        }

        // 判断炮台是否在开火
        public boolean isActive(){
            // 使用父类的 isShooting 或检查 reload 状态
            return this.isShooting || (reload < 1f && this.warmup() > 0.1f);
        }
    }

    // ========== 让炮台使用自定义 Build 类 ==========

    public TurretBuild createTurretBuild(){
        return new ShieldArcTurretBuild();
    }
}