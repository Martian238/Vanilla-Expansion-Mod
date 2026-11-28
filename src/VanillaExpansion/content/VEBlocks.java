package VanillaExpansion.content;

import VanillaExpansion.expand.world.block.SixteenDirectionBlock;
import VanillaExpansion.expand.world.block.debug.DebugBlock;
import VanillaExpansion.expand.world.block.debug.Module.DebugModules;
import VanillaExpansion.expand.world.block.distribution.AdaptItemBridge;
import VanillaExpansion.expand.world.block.distribution.CableCar;
import VanillaExpansion.expand.world.block.distribution.Junction;
import VanillaExpansion.expand.world.block.distribution.SideOutputConveyor;
import VanillaExpansion.expand.world.block.liquid.AdaptLiquidBridge;
import VanillaExpansion.expand.world.block.liquid.LiquidOverflowGate;
import VanillaExpansion.expand.world.block.liquid.LiquidSorter;
import VanillaExpansion.expand.world.block.liquid.SideOutputConduit;
import VanillaExpansion.expand.world.block.power.*;
import VanillaExpansion.expand.world.block.production.CoolantDrill;
import VanillaExpansion.expand.world.block.sandbox.TitleTextBlock;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.DuctRouter;
import mindustry.world.blocks.distribution.OverflowGate;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.meta.BuildVisibility;
public class VEBlocks {
    //矿
    public static Block oreIron, oreUranium, oreManganese, oreQuartz;
    public static final Seq<Block> proximaOres = new Seq<>();
    //i say 神TM的物流
    public static Block fastSideOutputConveyor,proximaJunction,adaptItemBridge,overflow,invertoverflow,proximaDuctRouter,proximaInvertSorter,proximaSorter;
    //i say 神TM的流体
    public static Block sideOutputConduit,liquidOverflowGate,liquidUnderflowGate,liquidSorter,adaptLiquidBridge;
    //雷霆电力
    public static Block powerPole;
    // RBMK
    public static Block rbmkRod,rbmkControl,rbmkControlModerated,rbmkControlAuto,rbmkControlAutoModerated,rbmkWreckage,rbmkCooler,rbmkBoiler,rbmkReflector,rbmkModerator,rbmkAbsorber,rbmkConsole;
    //测试玩意
    public static Block rockCoreDrill,test16Dir,testCoolantDrill,TitleTextBlock,cableCar;
    public static DebugBlock debug;

    public static void load(){
        oreIron = new OreBlock(VEItems.iron){{variants = 3;}};
        oreUranium = new OreBlock(VEItems.uranium){{variants = 3;}};
        oreManganese = new OreBlock(VEItems.manganese){{variants = 3;}};
        proximaOres.addAll(oreIron, oreUranium, oreManganese, oreQuartz);
        rockCoreDrill = new Block("test"){{requirements(Category.liquid, ItemStack.with());}};//占位
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
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
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
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        proximaJunction = new Junction("proxima-junction"){{
            requirements(Category.distribution,ItemStack.with(
                VEItems.iron, 5,
                VEItems.manganese, 5
            ));
            speed = 8;
            displayedSpeed = 38;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        fastSideOutputConveyor = new SideOutputConveyor("fast-side-output-conveyor"){{
            speed = 0.15f;
            displayedSpeed = 20f;
            requirements(Category.distribution, ItemStack.with(
                VEItems.iron, 1
            ));
            junctionReplacement = proximaJunction;
            bridgeReplacement = adaptItemBridge;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        sideOutputConduit = new SideOutputConduit("side-output-conduit"){{
            requirements(Category.liquid, ItemStack.with(
                VEItems.manganese, 1
            ));
            bridgeReplacement = adaptLiquidBridge;
            junctionReplacement = proximaJunction;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        proximaDuctRouter = new DuctRouter("proxima-duct-router"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 5
            ));
            health = 50;
            speed = 2;
            solid = false;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        proximaInvertSorter = new Sorter("proxima-inverted-sorter"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            invert = true;
            health =50;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        proximaSorter = new Sorter("proxima-sorter"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            invert = false;
            health =50;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        liquidOverflowGate = new LiquidOverflowGate("liquid-overflow-gate"){{
            requirements(Category.liquid, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 45;
            invert = false;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        liquidUnderflowGate = new LiquidOverflowGate("liquid-underflow-gate"){{
            requirements(Category.liquid, ItemStack.with(
                    VEItems.iron, 2,
                    VEItems.manganese, 2
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 45;
            invert = true;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        liquidSorter = new LiquidSorter("liquid-sorter"){{
            requirements(Category.liquid, ItemStack.with(
                    VEItems.iron, 4,
                    VEItems.manganese, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 60;
            rotate = false;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        overflow = new OverflowGate("proxima-overflow-gate"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 1
            ));
            health = 45;
            invert = false;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        invertoverflow = new OverflowGate("proxima-underflow-gate"){{
            requirements(Category.distribution, ItemStack.with(
                    VEItems.iron, 1
            ));
            health = 45;
            invert = true;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        test16Dir = new SixteenDirectionBlock("test-16dir"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 1));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            size = 1;
            destructible = true;
            health = 200;
            instantBuild = true;
            quickRotate = false;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        powerPole = new PowerPole("power-pole"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 5,
                VEItems.manganese, 5
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            size = 2;
            health = 120;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
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
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        RBMKFuelData.initDefaultFuels();
        rbmkRod = new RBMKRod("rbmk-rod"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 50,
                VEItems.uranium, 25
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 400;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkWreckage = new RBMKWreckage("rbmk-wreckage"){{
            size = 2;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkControl = new RBMKControl("rbmk-control"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 30,
                VEItems.uranium, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkControlModerated = new RBMKControl("rbmk-control-moderated"){{
            moderated = true;
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 40,
                VEItems.uranium, 15
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 380;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkControlAuto = new RBMKControlAuto("rbmk-control-auto"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 35,
                VEItems.uranium, 12
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkControlAutoModerated = new RBMKControlAuto("rbmk-control-auto-moderated"){{
            moderated = true;
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 45,
                VEItems.uranium, 17
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 380;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkBoiler = new RBMKBoiler("rbmk-boiler"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 60,
                VEItems.uranium, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkCooler = new RBMKCooler("rbmk-cooler"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 100,
                Items.lead, 80,
                Items.metaglass, 50
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkReflector = new RBMKReflector("rbmk-reflector"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 40,
                VEItems.uranium, 5,
                Items.lead, 15
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkModerator = new RBMKModerator("rbmk-moderator"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 30,
                Items.graphite, 25
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkAbsorber = new RBMKAbsorber("rbmk-absorber"){{
            requirements(Category.power, ItemStack.with(
                VEItems.iron, 30,
                Items.lead, 20
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 360;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        rbmkConsole = new RBMKConsole("rbmk-console"){{
            requirements(Category.logic, ItemStack.with(
                VEItems.iron, 80,
                VEItems.uranium, 10,
                VEItems.manganese, 10
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;
            health = 500;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
        TitleTextBlock = new TitleTextBlock("TitleTextBlock"){{
            requirements(Category.defense, ItemStack.with(Items.titanium, 1));
            size = 2;
            health = 3240;
            buildVisibility = BuildVisibility.sandboxOnly;
        }};
        debug = new DebugBlock("debug"){{
            requirements(Category.logic, ItemStack.with());
            buildVisibility = BuildVisibility.sandboxOnly;
            alwaysUnlocked = true;
        }};
        DebugModules.register();
        cableCar = new CableCar("cable-car"){{
            requirements(Category.distribution, ItemStack.with());
            health = 120;
            size = 2;
            maxConnections = 4;
            maxRange = 25f;
            itemSpeed = 2f;
            basketCount = 1;
            itemsPerTrip = 40;
            shownPlanets = ObjectSet.with(VEPlanets.proxima);
        }};
    }
}
