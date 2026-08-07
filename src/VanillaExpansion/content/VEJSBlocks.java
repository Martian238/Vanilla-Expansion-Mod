package VanillaExpansion.content;

import VanillaExpansion.MultiCrafter;
import VanillaExpansion.expand.world.block.defense.ShieldArcPowerTurret;
import VanillaExpansion.expand.world.block.liquid.LiquidSorter;
import VanillaExpansion.expand.world.block.power.ShakeGenerator;
import VanillaExpansion.expand.world.block.production.CoolantDrill;
import arc.struct.EnumSet;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.meta.BlockFlag;

public class VEJSBlocks {
    public static void load() {

        // group 0 原版普通新物流元件
        new DuctJunction("duct-junction");
        new DuctBridge("armored-bridge-conveyor");
        new OverflowDuct("armored-overflow-gate");
        new OverflowDuct("armored-underflow-gate");
        new DuctRouter("armored-router");
        new DirectionalUnloader("armored-unloader");
        new Unloader("phase-unloader");

        // group 1 原版重型物流元件
        new Duct("silicide-duct");
        new Duct("silicide-armored-duct");
        new Junction("silicide-junction");
        new BufferedItemBridge("silicide-bridge-conveyor");
        new Sorter("silicide-sorter");
        new Sorter("silicide-inverted-sorter");
        new Router("silicide-router");
        new Router("silicide-distributor");
        new OverflowGate("silicide-overflow-gate");
        new OverflowGate("silicide-underflow-gate");
        new Unloader("silicide-unloader");
        new ItemSource("silicide-item-source");
        new ItemVoid("silicide-item-void");

        // group 2 赛克轨道
        new Conveyor("rail");
        new Junction("rail-junction");
        new DuctRouter("rail-router");
        new OverflowDuct("rail-overflow-gate");
        new OverflowDuct("rail-underflow-gate");
        new ItemBridge("rail-bridge");
        new DirectionalUnloader("rail-unloader");
        new StackConveyor("stack-rail");

        // group 3 赛克重型轨道
        new Conveyor("silicide-rail");
        new Junction("silicide-rail-junction");
        new DuctRouter("silicide-rail-router");
        new OverflowDuct("silicide-rail-overflow-gate");
        new OverflowDuct("silicide-rail-underflow-gate");
        new ItemBridge("silicide-rail-bridge");
        new DirectionalUnloader("silicide-rail-unloader");
        new StackConveyor("silicide-stack-rail");

        // group 4 赛克高级物流
        new ArmoredConveyor("chromium-conveyor");
        new Sorter("chromium-sorter");
        new Conveyor("cobalt-rail");
        new OverflowGate("cobalt-rail-underflow-gate");
        new ItemBridge("cobalt-rail-bridge");
        new Unloader("cobalt-rail-unloader");
        new Unloader("multi-unloader");
        new MassDriver("mass-launcher");
        new MassDriver("mass-railgun");
        new MassDriver("warp-driver");

        // group 5 火瓜物流
        new ArmoredConveyor("ferric-rail");
        new Conveyor("ferric-conveyor");
        new ArmoredConveyor("ferric-conveyor-armored");
        new Junction("valve-cross");
        new ItemBridge("ferric-bridge");
        new Sorter("valve-sort");
        new Sorter("valve-inverted-sort");
        new Router("valve-distribute");
        new OverflowGate("valve-overflow");
        new OverflowGate("valve-underflow");
        new Unloader("valve-unload");
        new ItemBridge("phase-rail-bridge");
        new StackConveyor("phase-stack-rail");

        // group 6 原版新工厂
        new GenericCrafter("cryo-pool");
        new GenericCrafter("quartz-extractor");

        // group 7 赛克碳硅系列
        new GenericCrafter("isomorphic-press");
        new GenericCrafter("hydraulic-press");
        new GenericCrafter("quartz-separator");
        new GenericCrafter("quartz-separator-large");
        new GenericCrafter("isomorphic-smelter");
        new GenericCrafter("substitution-chamber");
        new GenericCrafter("silicide-crucible");
        new GenericCrafter("silicide-mixer");
        new GenericCrafter("coal-synthezer");
        new GenericCrafter("carbon-shale-smelter");

        // group 8 赛克化工系列
        new GenericCrafter("salt-electrolyzer");
        new GenericCrafter("melting-electrolyzer");
        new GenericCrafter("surge-electrolyzer");
        new GenericCrafter("gasification-chamber");
        new GenericCrafter("sodium-carbon-fixator");
        new GenericCrafter("titanium-extractor");
        new GenericCrafter("cracking-compressor");
        new GenericCrafter("chain-transferer");
        new GenericCrafter("carbonization-chamber");

        // group 9 赛克材料系列
        new GenericCrafter("eddy-melter");
        new Separator("decomposer");
        new Separator("extractor");
        new Separator("recycler");
        new GenericCrafter("lava-cooler");
        new GenericCrafter("catalyzon-crafter");

        // group 10 赛克其他工厂
        new GenericCrafter("isomorphic-kiln");
        new GenericCrafter("isomorphic-pulverizer");
        new GenericCrafter("large-pulverizer");
        new GenericCrafter("pyratiter");
        new GenericCrafter("blaster");
        new GenericCrafter("plant-press");
        new GenericCrafter("spore-blender");
        new GenericCrafter("cell-laboratory");
        new GenericCrafter("nitroalkoss-reactor");
        new GenericCrafter("fibralt-extender");
        new GenericCrafter("fusion-refueller");
        new GenericCrafter("fusion-disfueller");
        new GenericCrafter("phase-loom");
        new GenericCrafter("surge-coagulator");
        new GenericCrafter("warper");

        // group 11 火星红尘系列
        new GenericCrafter("blast-furnace");
        new GenericCrafter("multi-furnace");
        new GenericCrafter("centrifuge-kiln");
        new GenericCrafter("reflector-thermolyzer");
        new MultiCrafter("m-reduction-chamber");

        // group 11.1 火星碳硅系列
        new GenericCrafter("magnetic-separator");
        new GenericCrafter("magnetic-separator-large");

        // group 11.2 火星其他工厂
        new MultiCrafter("config-melter");
        new GenericCrafter("catalyzon-workshop");
        new MultiCrafter("m-upgraded-reaction-pool");
        new HeatCrafter("thermoplastic-compressor");

        // group 11.3 火星热量
        new HeatConductor("heat-conduct-conductor");
        new HeatConductor("heat-conduct-conductor-small");
        new HeatConductor("heat-conduct-distributor");
        new MultiCrafter("m-boiler");
        new HeatProducer("reactor-core");

        // group 12 瓜星工厂
        new GenericCrafter("silicon-oxidator");
        new GenericCrafter("eddy-mixer");
        new GenericCrafter("filter");
        new GenericCrafter("blender");
        new GenericCrafter("concentrator");
        new GenericCrafter("grinder");

        // group 13 原版新墙
        new Wall("copper-wall-huge");
        new Wall("titanium-wall-huge");
        new Wall("thorium-wall-huge");
        new Wall("defensive-wall");
        new Wall("defensive-wall-large");
        new Wall("defensive-wall-huge");
        new Wall("effective-wall");
        new Wall("effective-wall-large");
        new Wall("two-billion-wall");

        // group 14 赛克墙
        new Wall("aluminium-wall");
        new Wall("aluminium-wall-large");
        new Wall("aluminium-wall-huge");
        new Wall("aluminium-lead-wall");
        new Wall("aluminium-lead-wall-large");
        new Wall("aluminium-lead-wall-huge");
        new Wall("silicide-aluminium-wall");
        new Wall("silicide-aluminium-wall-large");
        new Wall("silicide-aluminium-wall-huge");
        new Wall("silicide-wall");
        new Wall("silicide-wall-large");
        new Wall("chromium-wall");
        new Wall("chromium-wall-large");
        new Wall("chromium-wall-huge");
        new Wall("silicide-chromium-wall");
        new Wall("silicide-chromium-wall-large");
        new Wall("silicide-chromium-wall-huge");
        new Door("mechanical-gate-small");
        new Door("mechanical-gate");
        new Door("mechanical-gate-silicide");
        new AutoDoor("automatic-gate");
        new AutoDoor("automatic-gate-silicide");
        new PowerNode("advanced-plastanium-wall");
        new PowerNode("advanced-plastanium-wall-large");
        new RegenProjector("fibralt-wall");
        new RegenProjector("fibralt-wall-large");
        new BaseShield("blocking-wall");
        new BaseShield("blocking-wall-silicide");
        new ShieldWall("surge-shield-wall");

        // group 15 火瓜墙
        new Wall("ferrum-wall");
        new Wall("ferrum-wall-large");
        new Wall("ferrum-wall-huge");
        new Wall("silicide-ferrum-wall");
        new Wall("silicide-ferrum-wall-large");
        new Wall("silicide-ferrum-wall-huge");
        new Wall("tantalum-wall");
        new Wall("tantalum-wall-large");
        new Wall("tantalum-wall-huge");
        new Wall("silicide-tantalum-wall");
        new Wall("silicide-tantalum-wall-large");
        new Wall("silicide-tantalum-wall-huge");
        new PowerTurret("crystallon-wall");
        new PowerTurret("crystallon-wall-large");

        // group 16 效果1
        new ConsumeGenerator("sweeper");
        new OverdriveProjector("overdriver");
        new MendProjector("mend-dome");
        new GenericCrafter("team-projector-sharded"){{
            flags = EnumSet.of(BlockFlag.core,BlockFlag.storage,BlockFlag.generator,BlockFlag.turret,BlockFlag.factory,BlockFlag.repair,BlockFlag.battery,BlockFlag.reactor,BlockFlag.drill,BlockFlag.shield);
        }};
        new GenericCrafter("team-projector-alpha");
        new GenericCrafter("team-projector-crux");
        new GenericCrafter("team-projector-omega");
        new GenericCrafter("team-projector-zenith");
        new GenericCrafter("team-projector-zenith-flipped");
        new GenericCrafter("team-projector-erekir-sharded");
        new GenericCrafter("team-projector-erekir-malis");
        new MendProjector("pog-mender");
        new OverdriveProjector("pog-overdriver");
        new ConsumeGenerator("sandbox-blast");
        new BaseShield("force-source");

        // group 17 效果2
        new MendProjector("mend-point");
        new MendProjector("menderator");
        new MendProjector("mend-globe");
        new OverdriveProjector("overdrive-point");
        new OverdriveProjector("overdrivator");
        new OverdriveProjector("overdrive-globe");
        new ForceProjector("forcerator");
        new Router("plastanium-stool");
        new PowerTurret("trap-magnetic");
        new PowerTurret("trap-flame");
        new PowerTurret("trap-blast");
        new PowerTurret("trap-electric");
        new PointDefenseTurret("warp-defender");
        new PowerTurret("spore-bomb");
        new MultiCrafter("cargo-anchor");
        new PowerTurret("nuke");

        // group 18 核心
        new CoreBlock("core-nucleus-root");
        new CoreBlock("core-singularity-root");
        new CoreBlock("core-nucleus-root-sitrullus");
        new CoreBlock("isomorphic-core-shard");
        new CoreBlock("isomorphic-core-foundation");
        new CoreBlock("isomorphic-core-nucleus");
        new CoreBlock("core-quark");
        new CoreBlock("core-singularity");
        new CoreBlock("core-general");

        // group 19 储存
        new StorageBlock("shelf");
        new StorageBlock("bank");
        new StorageBlock("shelf-silicide");
        new StorageBlock("container-silicide");
        new StorageBlock("bank-silicide");
        new StorageBlock("warp-disc");
        new StorageBlock("warp-base");
        new StorageBlock("storage-extender");

        // group 20 效果3
        new GenericCrafter("lamp");
        new GenericCrafter("lamp-alarm");
        new LandingPad("advanced-landing-pad");
        new Accelerator("isomorphic-accelerator-small");
        new Accelerator("isomorphic-accelerator");

        // group 21 流体
        new Pump("isomorphic-pump");
        new Pump("pressure-pump");
        new Pump("platform-pump");
        new Pump("chained-pump");
        new Conduit("isomorphic-conduit");
        new LiquidRouter("fluid-router");
        new LiquidJunction("fluid-junction");
        new LiquidSorter("fluid-sorter");
        new LiquidBridge("isomorphic-bridge-conduit");
        new ArmoredConduit("pressure-conduit");
        new LiquidBridge("platform-bridge-conduit");
        new LiquidRouter("can");
        new LiquidRouter("tank");
        new LiquidRouter("can-silicide");
        new LiquidRouter("tank-silicide");
        new Conduit("silicide-conduit");
        new ArmoredConduit("silicide-plated-conduit");
        new LiquidRouter("silicide-fluid-router");
        new LiquidJunction("silicide-fluid-junction");
        new LiquidBridge("silicide-bridge-conduit");
        new LiquidSource("silicide-fluid-source");
        new LiquidVoid("silicide-fluid-void");
        new Conduit("silver-conduit");
        new ArmoredConduit("silver-conduit-armored");
        new LiquidRouter("valve-fluid-distribute");
        new LiquidJunction("valve-fluid-cross");
        new LiquidBridge("silver-bridge");

        // group 22 赛克电力
        new PowerNode("isomorphic-node");
        new BeamNode("advanced-node");
        new Wall("node-blocker");
        new PowerNode("insulated-node");
        new PowerNode("isomorphic-node-large");
        new BeamNode("advanced-node-large");
        new LongPowerNode("sector-power-tower");
        new Battery("power-battery");
        new Battery("silicide-battery");
        new Battery("sodium-sulphur-battery");
        new GenericCrafter("charger");
        new ConsumeGenerator("discharger");
        new ConsumeGenerator("large-combustion-generator");
        new ConsumeGenerator("large-turbine-generator");
        new ConsumeGenerator("internal-combustion-generator");
        new ConsumeGenerator("sodium-reactor");
        new SolarGenerator("solar-pad");
        new ThermalGenerator("geothermal-generator");
        new ConsumeGenerator("lava-thermal-generator");
        new ConsumeGenerator("solid-fuel-cell");
        new ConsumeGenerator("fluid-fuel-cell");
        new ConsumeGenerator("radioactive-thermal-generator");
        new NuclearReactor("micro-reactor");
        new NuclearReactor("isomorphic-reactor");
        new ImpactReactor("fusion-reactor");

        // group 23 火星电力
        new PowerNode("cluster-node");
        new SolarGenerator("spot-solar-pad");
        new ConsumeGenerator("blast-engine");
        new MultiCrafter("heat-turbine-generator");
        new VariableReactor("thermal-reactor");

        // group 24 瓜星电力
        new TileableLogicDisplay("cable");
        new PowerNode("battery-node");
        new PowerNode("battery-tower");
        new NuclearReactor("melon-cell");
        new ConsumeGenerator("cycle-turbine-generator");
        new ConsumeGenerator("fermentor");
        new ConsumeGenerator("activator-generator");
        new ShakeGenerator("mect-reactor");

        // group 25 原版及赛克钻头和培养机
        new Drill("mechanical-drill-micro");
        new Drill("mechanical-drill-huge");
        new Drill("isomorphic-drill");
        new Drill("isomorphic-drill-huge");
        new Drill("power-drill");
        new BeamDrill("laser-bore");
        new Drill("beam-drill");
        new Drill("silicide-drill");
        new Drill("hydraulic-drill");
        new Drill("powder-digger");
        new BurstDrill("floor-crusher");
        new Drill("space-digger");
        new SolidPump("power-well");
        new SolidPump("lava-well");
        new SolidPump("pressure-well");
        new SolidPump("extraction-platform");
        new AttributeCrafter("tissue-cultivator");
        new AttributeCrafter("cultivate-tank");
        new GenericCrafter("nitroalkoss-cultivator");
        new GenericCrafter("nitroalkoss-cultivator-silicide");

        // group 26 火星钻头和培养机
        new CoolantDrill("surge-digger");
        new GenericCrafter("reflector-cultivator");

        // group 27 瓜星钻头及培养机
        new Drill("collector");
        new Drill("magnetic-digger");
        new SolidPump("mechanical-well");
        new AttributeCrafter("rot-chamber");

        // group 28 铝炮硅化物炮
        new ItemTurret("click");
        new ItemTurret("frag");
        new ItemTurret("rise");
        new ItemTurret("bake");
        new PowerTurret("pulse");
        new LiquidTurret("waterer");
        new ItemTurret("beat");
        new ItemTurret("crack");
        new PowerTurret("shock");

        // group 29 铬炮钴炮
        new ItemTurret("buffet");
        new ContinuousLiquidTurret("burn");
        new ItemTurret("dot");
        new LiquidTurret("spurt");
        new ItemTurret("snipe");
        new ItemTurret("shell");
        new PowerTurret("halberd");

        // group 30 钍炮合金炮
        new ItemTurret("buster");
        new ItemTurret("parasite");
        new ItemTurret("shower");
        new PowerTurret("chain");
        new PowerTurret("sweep");
        new ItemTurret("stab");
        new LiquidTurret("sans");

        // group 31 铁炮银炮
        new ItemTurret("double");
        new ItemTurret("burst");
        new ItemTurret("ash");
        new ItemTurret("pantype");
        new ItemTurret("meteor");
        new PowerTurret("vector");
        new PowerTurret("guarden");
        new PowerTurret("incandescence");
        new PowerTurret("melonic-array-pillar");

        //group 31.1 钽炮布炮
        new PowerTurret("stellar");
        new TractorBeamTurret("disintegrate");
        new ItemTurret("asylum");
        new ShieldArcPowerTurret("blocker");

        // group 32 原版单位载荷
        new Reconstructor("light-refabricator");
        new PayloadConveyor("gigantic-payload-conveyor");
        new PayloadConveyor("world-payload-conveyor");

        // group 33 赛克单位
        new UnitFactory("ground-fabricator");
        new UnitFactory("air-fabricator");
        new UnitFactory("naval-fabricator");
        new UnitFactory("special-fabricator");
        new UnitFactory("integrated-constructor");
        new UnitCargoLoader("platform-theta");
        new UnitCargoLoader("platform-lambda");
        new UnitCargoLoader("platform-sigma");
        new Reconstructor("isomorphic-additive-reconstructor");
        new Reconstructor("isomorphic-multiplicative-reconstructor");
        new Reconstructor("junior-armorcar-reconstructor");
        new Reconstructor("junior-hovership-reconstructor");
        new Reconstructor("junior-crabbot-reconstructor");
        new Reconstructor("senior-armorcar-reconstructor");
        new Reconstructor("senior-hovership-reconstructor");
        new Reconstructor("senior-crabbot-reconstructor");
        new Reconstructor("super-reconstructor");
        new Reconstructor("ultra-reconstructor");
        new Reconstructor("junior-refabricator");
        new Reconstructor("senior-refabricator");
        new RepairTurret("isomorphic-repair-point");

        // group 34 赛克载荷
        new PayloadConveyor("payload-rail-small");
        new PayloadConveyor("payload-rail");
        new PayloadRouter("payload-rail-router");
        new PayloadConveyor("payload-rail-large");
        new PayloadRouter("payload-rail-router-large");
        new PayloadConveyor("cobalt-payload-rail");
        new PayloadRouter("cobalt-payload-rail-router");
        new PayloadConveyor("cobalt-payload-rail-large");
        new PayloadRouter("cobalt-payload-rail-router-large");
        new PayloadAmmoTurret("warp-teleporter");
        new Constructor("assemble-packer");
        new UnitAssemblerModule("assemble-selector1");
        new UnitAssemblerModule("assemble-selector2");
        new UnitAssemblerModule("assemble-selector3");
        new Wall("super-assemble-pack");
        new Wall("ultra-assemble-pack");
        new Wall("hyper-assemble-pack");
        new UnitAssembler("special-assembler");
        new UnitAssembler("super-assembler");
        new UnitAssembler("ultra-assembler");
        new UnitAssembler("boss-assembler");

        // group 34.1 火星单位载荷
        new Reconstructor("junior-reconstruct-pad");
        new Reconstructor("senior-reconstruct-pad");

        // group 35 瓜星单位载荷
        new PayloadConveyor("general-payload-conveyor");
        new UnitFactory("small-unit-constructor");
        new UnitFactory("small-unit-constructor-sharded");
        new UnitFactory("standard-constructor");
        new UnitFactory("standard-constructor-sharded");
        new Reconstructor("elite-upgrader");
        new Reconstructor("elite-upgrader-sharded");
        new RepairTower("melon-repair-bay");
        new Constructor("watermelon-printer");
        new Constructor("element-printer");
        new GenericCrafter("element-ferric");
        new GenericCrafter("element-silver");
        new GenericCrafter("element-melonic");
        new GenericCrafter("element-effective");

        // group 36 逻辑
        new MessageBlock("isomorphic-message");
        new SwitchBlock("isomorphic-switch");
        new LogicBlock("isomorphic-processor");
        new MemoryBlock("isomorphic-memory-cell");
        new MemoryBlock("isomorphic-memory-bank");
        new LogicBlock("quantum-processor");
        new MemoryBlock("quantum-memory-cell");
    }
}
