package VanillaExpansion.content;

import VanillaExpansion.expand.world.block.defense.DrawerTractorBeamTurret;
import VanillaExpansion.expand.world.block.distribution.AdaptItemBridge;
import VanillaExpansion.expand.world.block.distribution.Junction;
import VanillaExpansion.expand.world.block.distribution.MechanicalArm;
import VanillaExpansion.expand.world.block.distribution.SideOutputConveyor;
import VanillaExpansion.expand.world.block.liquid.AdaptLiquidBridge;
import VanillaExpansion.expand.world.block.liquid.LiquidOverflowGate;
import VanillaExpansion.expand.world.block.liquid.LiquidSorter;
import VanillaExpansion.expand.world.block.liquid.Pipe;
import VanillaExpansion.expand.world.block.liquid.SideOutputConduit;
import VanillaExpansion.expand.world.block.optics.LaserEmitter;
import VanillaExpansion.expand.world.block.optics.LaserMirror;
import VanillaExpansion.expand.world.block.optics.LaserReceiver;
import VanillaExpansion.expand.exp.EField;
import VanillaExpansion.expand.exp.ExpTurret;
import VanillaExpansion.expand.exp.ExpLaserBulletType;
import VanillaExpansion.expand.exp.ExpWall;
import VanillaExpansion.expand.world.block.power.*;
import VanillaExpansion.expand.world.block.production.CoolantDrill;
import VanillaExpansion.expand.world.block.production.RockCoreDrill;
import VanillaExpansion.expand.world.block.production.RotatableCrafter;
import arc.struct.*;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Strings;
import mindustry.content.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.part.RegionPart;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.distribution.DuctRouter;
import mindustry.world.blocks.distribution.OverflowGate;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.environment.*;
import mindustry.world.consumers.ConsumeCoolant;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.draw.DrawTurret;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Stat;
import VanillaExpansion.expand.world.block.*;

import static mindustry.Vars.tilesize;

public class VEBlocks {
    public static Block oreIron, oreUranium, oreManganese, oreQuartz;
    public static final Seq<Block> proximaOres = new Seq<>();

    //i say 神TM的物流
    public static Block fastSideOutputConveyor;
    public static Block proximaJunction;
    public static Block adaptItemBridge;
    public static Block overflow;
    public static Block invertoverflow;
    public static Block proximaDuctRouter;
    public static Block proximaInvertSorter;
    public static Block proximaSorter;

    //i say 神TM的流体
    public static Block sideOutputConduit;
    public static Block liquidOverflowGate;
    public static Block liquidUnderflowGate;
    public static Block liquidSorter;
    public static Block adaptLiquidBridge;

    // 16方向测试
    public static Block test16Dir;
    // 电线杆
    public static Block powerPole;
    //钻头
    public static Block rockCoreDrill;
    // RBMK 燃料柱
    public static Block rbmkRod;
    // RBMK 控制棒
    public static Block rbmkControl;
    // RBMK 慢化控制棒
    public static Block rbmkControlModerated;
    // RBMK 自动控制棒
    public static Block rbmkControlAuto;
    // RBMK 慢化自动控制棒
    public static Block rbmkControlAutoModerated;
    // RBMK 熔毁残骸（16向随机朝向）
    public static Block rbmkWreckage;
    // RBMK 冷却器
    public static Block rbmkCooler;
    // RBMK 锅炉
    public static Block rbmkBoiler;
    // RBMK 反射器
    public static Block rbmkReflector;
    // RBMK 慢化剂
    public static Block rbmkModerator;
    // RBMK 吸收器
    public static Block rbmkAbsorber;
    // RBMK 控制台
    public static Block rbmkConsole;
    // 经验激光炮塔
    public static Block expTurret;
    // 经验墙
    public static Block expWall;

    //测试玩意
    public static Block testCoolantDrill;



    public static void load(){
        oreIron = new OreBlock(VEItems.iron){{
            variants = 3;
        }};

        oreUranium = new OreBlock(VEItems.uranium){{
            variants = 3;
        }};

        oreManganese = new OreBlock(VEItems.manganese){{
            variants = 3;
        }};


        proximaOres.addAll(oreIron, oreUranium, oreManganese, oreQuartz);

        // 分类物品桥
        adaptItemBridge = new AdaptItemBridge("adapt-item-bridge"){{
            requirements(Category.distribution, ItemStack.with(
                VEItems.iron, 13,
                VEItems.manganese, 13
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;

            hasPower = false;
            range = 6;
            health = 300;

            placeableLiquid = true;
        }};

        // 分类流体桥
        adaptLiquidBridge = new AdaptLiquidBridge("adapt-liquid-bridge"){{
            requirements(Category.liquid, ItemStack.with(
                VEItems.iron, 15,
                VEItems.manganese, 15
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;

            hasPower = false;
            range = 6;
            health = 350;
        }};
        // 万用交叉器
        proximaJunction = new Junction("proxima-junction"){{
            requirements(Category.distribution,ItemStack.with(
                VEItems.iron, 5,
                VEItems.manganese, 5
            ));
            speed = 8;
            displayedSpeed = 38;
        }};
        // 高速侧输出传送带
        fastSideOutputConveyor = new SideOutputConveyor("fast-side-output-conveyor"){{
            speed = 0.15f;
            displayedSpeed = 20f;
            requirements(Category.distribution, ItemStack.with(
                VEItems.iron, 1
            ));
            junctionReplacement = proximaJunction;
            bridgeReplacement = adaptItemBridge;
        }};
        // 侧向输出导管
        sideOutputConduit = new SideOutputConduit("side-output-conduit"){{
            requirements(Category.liquid, ItemStack.with(
                VEItems.manganese, 1
            ));
            bridgeReplacement = adaptLiquidBridge;
            junctionReplacement = proximaJunction;
        }};
        proximaDuctRouter = new DuctRouter("proxima-duct-router"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 5
            ));
            health = 50;
            speed = 2;
            solid = false;
        }};
        proximaInvertSorter = new Sorter("proxima-inverted-sorter"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            invert = true;
            health =50;
        }};
        proximaSorter = new Sorter("proxima-sorter"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            invert = false;
            health =50;
        }};
        // 流体溢流门
        liquidOverflowGate = new LiquidOverflowGate("liquid-overflow-gate"){{
            requirements(Category.liquid, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 45;
            invert = false;
        }};
        // 流体反向溢流门
        liquidUnderflowGate = new LiquidOverflowGate("liquid-underflow-gate"){{
            requirements(Category.liquid, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 45;
            invert = true;
        }};
        // 流体分类器
        liquidSorter = new LiquidSorter("liquid-sorter"){{
            requirements(Category.liquid, ItemStack.with(
                    VEItems.iron, 4,
                    VEItems.manganese, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 60;
            rotate = false;
        }};
        overflow = new OverflowGate("proxima-overflow-gate"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 1
            ));
            health = 45;
            invert = false;
        }};
        invertoverflow = new OverflowGate("proxima-underflow-gate"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 1
            ));
            health = 45;
            invert = true;
        }};

        // 16方向测试方块
        test16Dir = new SixteenDirectionBlock("test-16dir"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 1));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            size = 1;
            destructible = true;
            health = 200;
            instantBuild = true;
            quickRotate = false;
        }};
        // 电线杆
        powerPole = new PowerPole("power-pole"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 5,
                VEItems.manganese, 5
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            size = 2;
            health = 120;
        }};
        // 岩芯钻机
        rockCoreDrill = new RockCoreDrill("rock-core-drill"){{
            requirements(Category.production, ItemStack.with(
                    VEItems.iron, 20
            ));
            // 基础属性
            size = 2;
            tier = 3;
            drillTime = 1120f;      // 单个钻头挖掘时间
            warmupSpeed = 0.015f;
            alwaysUnlocked = true;

            // 定义4个钻孔的偏移坐标（相对于方块中心，单位：像素）
            // size=2时，方块大小为64x64像素，中心点偏移4像素到四个象限
            drillCount = 4;
            drillOffsetX = new float[]{-4f, 4f, -4f, 4f};
            drillOffsetY = new float[]{-4f, -4f, 4f, 4f};

            // 可选：设置每个钻孔的转速乘数（默认都是1.0f）
            drillSpeedMultipliers = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

            // 可选：设置显示效果
            drawMultipleDrills = true;
            drawMineItem = true;

            // 启用液体强化
            liquidBoostIntensity = 1.6f;  // 2.56倍速度提升

            // 添加液体消耗（水）
            consume(new ConsumeLiquid(Liquids.water, 4f / 60f){{
                optional = true;   // 可选，不是必需的
                booster = true;    // 标记为强化剂
            }}); // 6/秒，转换为每帧消耗
        }};

        //测试
        testCoolantDrill = new CoolantDrill("test-coolant-drill"){{
            size = 4;
            buildVisibility = BuildVisibility.sandboxOnly;
            alwaysUnlocked = true;
            drillTime = 280f;
            tier = 5;
            itemCapacity = 100;
            liquidCapacity = 200f;
            consumePower(3f);
            consumeCoolant(0.1f).boost();
            liquidBoostIntensity = 1.8f;
            rotateSpeed = 6f;
            Seq<Liquid> liquidSeq = new Seq<>();
            liquidSeq.add(Liquids.water);
            liquidSeq.add(Liquids.cryofluid);
            coolants = liquidSeq.toArray(Liquid.class);
        }};

        // 注册 RBMK 燃料数据（依赖 VEItems.load() 已执行）
        RBMKFuelData.initDefaultFuels();

        // RBMK 燃料柱
        rbmkRod = new RBMKRod("rbmk-rod"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 50,
                VEItems.uranium, 25
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 400;
        }};

        // RBMK 熔毁残骸（仅在熔毁事件中生成，16向随机朝向）
        rbmkWreckage = new RBMKWreckage("rbmk-wreckage"){{
            size = 2;
        }};

        // RBMK 控制棒
        rbmkControl = new RBMKControl("rbmk-control"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 30,
                VEItems.uranium, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
        }};

        // RBMK 慢化控制棒
        rbmkControlModerated = new RBMKControl("rbmk-control-moderated"){{
            moderated = true;
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 40,
                VEItems.uranium, 15
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 380;
        }};

        // RBMK 自动控制棒
        rbmkControlAuto = new RBMKControlAuto("rbmk-control-auto"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 35,
                VEItems.uranium, 12
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
        }};

        // RBMK 慢化自动控制棒
        rbmkControlAutoModerated = new RBMKControlAuto("rbmk-control-auto-moderated"){{
            moderated = true;
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 45,
                VEItems.uranium, 17
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 380;
        }};

        // RBMK 锅炉
        rbmkBoiler = new RBMKBoiler("rbmk-boiler"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 60,
                VEItems.uranium, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
        }};

        // RBMK 冷却器（应急散热：消耗冷 PFM，5×5 列范围每 tick 降温 200°C）
        rbmkCooler = new RBMKCooler("rbmk-cooler"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 100,
                Items.lead, 80,
                Items.metaglass, 50
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
        }};

        // RBMK 反射器（把逃逸中子弹回源燃料柱）
        rbmkReflector = new RBMKReflector("rbmk-reflector"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 40,
                VEItems.uranium, 5,
                Items.lead, 15
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
        }};

        // RBMK 慢化剂（快中子变慢中子，增益下游 SLOW/ANY 燃料）
        rbmkModerator = new RBMKModerator("rbmk-moderator"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 30,
                Items.graphite, 25
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
        }};

        // RBMK 吸收器（通量→热，完全/部分吸收中子流）
        rbmkAbsorber = new RBMKAbsorber("rbmk-absorber"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 30,
                Items.lead, 20
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
        }};

        // RBMK 控制台（15×15 扫描 + 批控）
        rbmkConsole = new RBMKConsole("rbmk-console"){{
            requirements(Category.logic, ItemStack.with(
                VEItems.iron, 80,
                VEItems.uranium, 10,
                VEItems.manganese, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 500;
        }};

        // 经验激光炮塔（移植自 Project Unity 的 laser-turret 注册，简化去 powerUse/chargeTime）
        expTurret = new ExpTurret("exp-laser-turret"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 90, Items.silicon, 40, Items.titanium, 15));
            size = 2;
            health = 600;

            reload = 35f;
            range = 140f;
            targetAir = false;
            shootSound = Sounds.shootLaser;
            consumePower(2f);

            maxLevel = 10;
            expFields = new EField[]{
                new EField.ELinear(v -> range = v, 120f, 2f, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks"),
                new EField.EBool(v -> targetAir = v, false, 5, Stat.targetsAir)
            };

            shoot = new ShootPattern(){{
                shotDelay = 12f;
                shots = 2;
            }};

            shootType = new ExpLaserBulletType(140f, 25f){{
                damageInc = 7f;
                expGain = buildingExpGain = 2;
                fromColor = Pal.accent;
                toColor = Pal.lancerLaser;
            }};
        }};

        // 经验墙（移植自 Project Unity 的 steel-wall 注册，基于简化 ExpWall）
        expWall = new ExpWall("exp-wall"){{
            requirements(Category.defense, ItemStack.with(Items.titanium, 24, Items.silicon, 24));
            size = 2;
            health = 3240;

            maxLevel = 12;
            damageExp = 1 / 20f;
            expFields = new EField[]{
                new EField.ELinear(v -> damageReduction = v, 0.1f, 0.02f, Stat.armor, f -> Strings.autoFixed(Mathf.roundPositive(f * 10000f) / 100f, 2) + "%")
            };
        }};
    }
}
