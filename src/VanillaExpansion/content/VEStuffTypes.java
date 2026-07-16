package VanillaExpansion.content;

import VanillaExpansion.VEPal;
import VanillaExpansion.expand.abilities.BoostAbility;
import VanillaExpansion.expand.type.unit.ExpandUnit;
import VanillaExpansion.expand.type.unit.StuffSlot;
import VanillaExpansion.expand.type.unit.StuffType;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.part.HaloPart;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.weapons.PointDefenseWeapon;
import mindustry.type.weapons.RepairBeamWeapon;

import static mindustry.Vars.*;

public class VEStuffTypes {

    public static StuffType laserCannon;
    public static StuffType missileLauncher;
    public static StuffType pointDefenseModule;
    public static StuffType flamethrower;
    public static StuffType forceField;
    public static StuffType repairBeam;
    public static StuffType speedBooster;
    public static StuffType miningDrill;
    public static StuffType buildAccelerator;
    public static StuffType healthBooster;
    public static StuffType ancientEngine;

    public static void load() {
        // 激光加农炮
        laserCannon = new StuffType("laser-cannon") {{
            stuffName = "Laser Cannon";
            description = "Mounts a powerful laser weapon.";
            slot = StuffSlot.weapon;
            weight = 2.5f;
            tier = 2;
            rarityColor = Color.valueOf("ff6b6b");

            weapons.add(new Weapon("laser-cannon-weapon") {{
                x = 0f;
                y = 4f;
                reload = 15f;
                recoil = 1f;
                shootY = 2f;
                shootSound = Sounds.shootLaser;

                bullet = new LaserBulletType(20f) {{
                    damage = 25;
                    lifetime = 30f;
                    status = StatusEffects.melting;
                    statusDuration = 120f;
                    hitEffect = Fx.hitLaser;
                    shootEffect = Fx.none;
                }};
            }});
        }};

        // 导弹发射器
        missileLauncher = new StuffType("missile-launcher") {{
            stuffName = "Missile Launcher";
            description = "Launches explosive missiles.";
            slot = StuffSlot.weapon;
            weight = 3f;
            tier = 2;
            rarityColor = Color.valueOf("ffa94d");

            weapons.add(new Weapon("missile-launcher-weapon") {{
                x = 0f;
                y = 5f;
                reload = 40f;
                recoil = 2f;
                shootY = 3f;
                shootSound = Sounds.shoot;

                bullet = new BasicBulletType(5f, 30) {{
                    lifetime = 60f;
                    damage = 40;
                    splashDamage = 60;
                    splashDamageRadius = 30f;
                    status = StatusEffects.burning;
                    statusDuration = 180f;
                    hitEffect = Fx.none;
                    shootEffect = Fx.shootBig;
                    smokeEffect = Fx.shootBigSmoke;
                }};
            }});
        }};

        // 点防御
        pointDefenseModule = new StuffType("point-defense") {{
            stuffName = "Point Defense Module";
            description = "Intercepts incoming enemy projectiles.";
            slot = StuffSlot.weapon;
            weight = 2f;
            tier = 3;
            rarityColor = Color.valueOf("4ecdc4");

            weapons.add(new PointDefenseWeapon("pd-weapon") {{
                x = 0f;
                y = 3f;
                reload = 5f;
                color = Color.valueOf("4ecdc4");
                bullet = new BasicBulletType(10f, 15) {{
                    lifetime = 20f;
                    damage = 15;
                    collidesAir = true;
                    collidesGround = false;
                }};
            }});
        }};

        // 火焰喷射器
        flamethrower = new StuffType("flamethrower") {{
            stuffName = "Flamethrower";
            description = "Burns enemies in short range.";
            slot = StuffSlot.weapon;
            weight = 2f;
            tier = 1;
            rarityColor = Color.valueOf("ff4757");

            weapons.add(new Weapon("flamethrower-weapon") {{
                x = 0f;
                y = 3f;
                reload = 2f;
                shootY = 2f;
                shootSound = Sounds.shootFlame;

                bullet = new BasicBulletType(3f, 8) {{
                    lifetime = 30f;
                    damage = 8;
                    status = StatusEffects.burning;
                    statusDuration = 60f;
                    backColor = Color.valueOf("ff6b35");
                    frontColor = Color.valueOf("ffd93d");
                    hitEffect = Fx.hitFlameBeam;
                }};
            }});
        }};

        // 力场
        forceField = new StuffType("force-field") {{
            stuffName = "Force Field";
            description = "Projects a force field that absorbs bullets.";
            slot = StuffSlot.defense;
            weight = 4f;
            tier = 3;
            passive = true;
            rarityColor = Color.valueOf("a29bfe");

            abilities.add(new ForceFieldAbility(60f, 0.5f, 360f, 60f * 5f) {{
                sides = 8;
                rotation = 0f;
            }});
        }};

        // 修复光束
        repairBeam = new StuffType("repair-beam") {{
            stuffName = "Repair Beam";
            description = "Heals nearby friendly units.";
            slot = StuffSlot.utility;
            weight = 2f;
            tier = 2;
            rarityColor = Color.valueOf("55efc4");

            weapons.add(new RepairBeamWeapon("repair-beam-weapon") {{
                x = 0f;
                y = 3f;
                repairSpeed = 0.5f;
                targetUnits = true;
                targetBuildings = true;
            }});
        }};

        // 速度助推器
        speedBooster = new StuffType("speed-booster") {{
            stuffName = "Speed Booster";
            description = "Increases movement speed by 30%.";
            slot = StuffSlot.utility;
            weight = 1f;
            tier = 1;
            passive = true;
            rarityColor = Color.valueOf("fdcb6e");

            processor = (unit, stuff, slot) -> {
                UnitType type = unit.type;
                if (type != null) {
                    type.speed = type.speed * 1.3f;
                }
            };
        }};

        // 采矿钻头
        miningDrill = new StuffType("mining-drill") {{
            stuffName = "Mining Drill";
            description = "Increases mining speed and tier.";
            slot = StuffSlot.utility;
            weight = 1.5f;
            tier = 2;
            passive = true;
            rarityColor = Color.valueOf("f8a5c2");

            processor = (unit, stuff, slot) -> {
                UnitType type = unit.type;
                if (type != null) {
                    type.mineSpeed = Math.max(type.mineSpeed * 2f, 2f);
                    type.mineTier = Math.max(type.mineTier, 3);
                }
            };
        }};

        // 建造加速器
        buildAccelerator = new StuffType("build-accelerator") {{
            stuffName = "Build Accelerator";
            description = "Increases building speed by 50%.";
            slot = StuffSlot.utility;
            weight = 1.5f;
            tier = 2;
            passive = true;
            rarityColor = Color.valueOf("81ecec");

            processor = (unit, stuff, slot) -> {
                UnitType type = unit.type;
                if (type != null) {
                    type.buildSpeed = Math.max(type.buildSpeed * 1.5f, 1f);
                }
            };
        }};

        // 生命值增强器 (替代能量存储)
        healthBooster = new StuffType("health-booster") {{
            stuffName = "Health Booster";
            description = "Increases maximum health by 100.";
            slot = StuffSlot.passive;
            weight = 2f;
            tier = 1;
            passive = true;
            rarityColor = Color.valueOf("ff6b6b");

            processor = (unit, stuff, slot) -> {
                // 使用标准的 health 系统
                unit.maxHealth(unit.maxHealth() + 100f);
                unit.heal(100f);
            };
        }};

        // 古代引擎
        ancientEngine = new StuffType("ancient-engine") {{
            stuffName = "Ancient Engine";
            description = "Powerful thrust with ancient effects.";
            slot = StuffSlot.special;
            weight = 3f;
            tier = 3;
            passive = true;
            rarityColor = VEPal.ancientLightMid;

            parts.add(new HaloPart() {{
                haloRotation = 0f;
                haloRadius = 5f;
                color = VEPal.ancientLightMid;
                layer = Layer.effect;
            }});

            abilities.add(new BoostAbility(2f, 8f));
        }};
    }

    public static Seq<StuffType> all() {
        return StuffType.allStuffs;
    }
}