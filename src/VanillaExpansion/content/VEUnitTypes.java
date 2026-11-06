package VanillaExpansion.content;

import VanillaExpansion.expand.type.unit.Hotbar.HotbarUnit;
import arc.graphics.Color;
import mindustry.ai.types.BuilderAI;
import mindustry.ai.types.CommandAI;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import VanillaExpansion.VEPal;
import VanillaExpansion.expand.abilities.BoostAbility;
import VanillaExpansion.expand.units.AncientEngine;
import VanillaExpansion.expand.bullets.ChainBulletType;
import VanillaExpansion.expand.bullets.DelayedPointBulletType;
import VanillaExpansion.expand.util.PosLightning;

/**
 * 比邻星单位类型定义
 */
public class VEUnitTypes {

    public static UnitType proximaCoreMech;
    public static HotbarUnit testHotbarUnit;


    public static void load() {
        /*proximaCoreMech = new UnitType("proxima-core-mech") {{
            constructor = UnitEntity::create;
            controller = u -> u.team.isAI() ? new BuilderAI(true, 400f) : new CommandAI();
            isEnemy = false;
            range = 240f;
            //abilities.add(new AttackSlowAbility(3f, 1f, 120f));
            abilities.add(new BoostAbility(3f, 5f));
            targetBuildingsMobile = false;
            lowAltitude = true;
            flying = true;
            // 建造能力
            mineSpeed = 6.5f;
            mineTier = 1;
            buildSpeed = 1.0f;

            drag = 0.05f;
            speed = 4.5f;
            rotateSpeed = 15f;
            accel = 0.1f;
            fogRadius = 0f;
            itemCapacity = 30;
            health = 250f;
            engineOffset = 6f;
            engineSize = -1;
            engines.add(new AncientEngine(-2f, -7.5f, 1f, -90));
            engines.add(new AncientEngine(2f, -7.5f, 1f, -90));
            engines.add(new AncientEngine(0f, -8.5f, 1.5f, -90, 0.45f, 0.6f, 2.6f));

            hitSize = 10.4f;

            alwaysUnlocked = true;

            wreckSoundVolume = 0.8f;
            deathSoundVolume = 0.7f;
            weapons.add(new Weapon("proxima-core-cannon") {{
                x = 0f;
                y = 0f;

                mirror = false;
                rotate = false;
                reload = 120f;
                recoil = 1.25f;
                minWarmup = 0.935f;
                cooldownTime = reload - 30f;
                shootY = 1.5f;

                shootSound = Sounds.shootLaser;

                bullet = new DelayedPointBulletType() {{
                    width = 10f;
                    damage = 80;
                    hitColor = VEPal.ancientLightMid;
                    lightColor = lightningColor = trailColor = VEPal.ancientLightMid;
                    rangeOverride = 240f;

                    trailEffect = Fx.none;

                    hitEffect = Fx.hitLaserBlast;
                    despawnEffect = Fx.smokeCloud;

                    status = StatusEffects.melting;
                    statusDuration = 600f;

                    despawnShake = hitShake = 2f;
                    collidesAir = collidesGround = true;

                    fragBullets = 1;
                    fragBullet = new ChainBulletType(80) {{
                        length = 0;
                        collidesAir = collidesGround = true;
                        quietShoot = true;
                        hitColor = VEPal.ancientLightMid;
                        lightColor = lightningColor = trailColor = VEPal.lightSkyMiddle;
                        thick = 7f;
                        maxHit = 3;
                        hitEffect = Fx.chainLightning;
                        effectController = (f, t) -> {
                            PosLightning.createEffect(f, t, hitColor, boltNum, thick);
                        };
                    }};
                }};
            }});
        }};*/
        testHotbarUnit = new HotbarUnit("test-hotbar-unit") {{
            health = 600f;
            speed = 1.3f;
            hitSize = 10f;
            armor = 3f;
            flying = true;
            constructor = UnitEntity::create;
            engineOffset = 5f;
            engineSize = 2.5f;
            outlineColor = Color.valueOf("2a2a2a");
        }};
    }
}
