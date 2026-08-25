package VanillaExpansion.content;

import VanillaExpansion.expand.graphics.VECacheLayer;
import VanillaExpansion.expand.world.block.environment.MantleTunnel;
import VanillaExpansion.expand.world.block.environment.NitroalkossProp;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.BuildVisibility;

public class VEEnvironBlocks {
    public static void load(){

        //没用玩意
        new Floor("core-zone-cyclant"){{
            buildVisibility = BuildVisibility.hidden;
        }};
        new StaticWall("dark-metal-repaired");
        //new Floor("lava-rock");
        //new Floor("volcanic-rock");
        new Prop("bush");



        //其他
        new Floor("accessible-deep-water");
        new Floor("white-ground");
        new Floor("red-ground");
        new Floor("blue-ground");
        new Floor("green-ground");
        new Floor("dark-ground");
        new StaticWall("pure-dark");
        new Floor("metal-tiles-13ve");
        new Floor("metal-tiles-14ve");
        new Floor("metal-tiles-15ve");
        new Floor("metal-tiles-16ve");



        //矿
        new OreBlock("ore-salt");
        new OreBlock("ore-aluminium");
        new OreBlock("ore-silver");
        new OreBlock("ore-silver-pure");
        new OreBlock("ore-catalyzon");
        new OreBlock("ore-pyratite");
        new OreBlock("ore-cobalt");
        new OreBlock("ore-ferrum");
        new OreBlock("ore-tantalum");
        new OreBlock("ore-thorium-maress");
        new OreBlock("ore-chromium");
        new OreBlock("ore-chromium-maress");
        new OreBlock("ore-wall-aluminium");
        new OreBlock("ore-wall-quartz");



        //赛克兰特
        new StaticTree("tree");
        new Floor("gravel");
        new Floor("carbon-shale");
        new StaticWall("carbon-shale-wall");
        new Prop("carbon-shale-boulder");
        new Floor("flowing-lava"){{cacheLayer = VECacheLayer.lava;}};
        new Floor("deep-water-oil");
        new Floor("hill-stone");
        new StaticWall("hill-stone-wall");
        new Prop("hill-stone-boulder");
        new Floor("salty-ice");
        new Floor("antigrass");
        new Floor("antigrass-dry");
        new StaticWall("antigrass-wall");
        new Floor("semigrass");
        new StaticWall("semigrass-wall");
        new WobbleProp("semigrass-bush");



        //玛瑞斯-荒地
        new Floor("red-soil-floor");
        new Floor("red-soil-finely");
        new Floor("red-soil-pebble");
        new StaticWall("red-soil-wall");
        new Floor("red-soil-wet-water");
        new Floor("red-soil-wet");
        new StaticWall("red-soil-wet-wall");
        //玛瑞斯-沼泽
        new Floor("swamp-water");
        new Floor("swamp-water-shallow");
        new Floor("swamp-water-muddy");
        new Floor("red-soil-muddy-heavy");
        new Floor("red-soil-muddy");
        //玛瑞斯-冰川
        new Floor("cold-water-deep");
        new Floor("cold-water");
        new Floor("salty-water-deep");
        new Floor("salty-water");
        new Floor("red-soil-frozen-water");
        new Floor("hard-snow-cold-water");
        new Floor("hard-snow-salty-water");
        new Floor("hard-snow-molten-water");
        new Floor("red-soil-frozen");
        new StaticWall("red-soil-frozen-wall");
        new Floor("hard-ice");
        new Floor("hard-ice-cracked");
        new Floor("hard-snow");
        new Floor("hard-snow-molten");
        new Floor("fiber-snow");
        new Floor("frostone");
        new Floor("frostone-radioactive");
        new StaticWall("frostone-wall");
        new Prop("frostone-boulder");
        new SteamVent("hydrogen-vent");
        //玛瑞斯-峡谷
        new Floor("gravel-ferric");
        new Floor("ferric-shale");
        new StaticWall("ferric-shale-wall");
        new Prop("ferric-shale-boulder");
        new TallBlock("ferric-rock");
        new Floor("pooled-acid"){{cacheLayer = VECacheLayer.acid;}};
        new Floor("deepslate");
        new StaticWall("deepslate-wall");
        new Floor("deepslate-brick");
        new StaticWall("deepslate-brick-wall");
        //玛瑞斯-植物
        new OverlayFloor("withered-reflector");
        new WobbleProp("mirmat");
        new WobbleProp("glasspam");
        new OverlayFloor("glasspam-root");
        new WobbleProp("outrayer");
        new TreeBlock("silvelade");
        new TreeBlock("silvelade-large");
        new TreeBlock("multi-silvelade");
        new NitroalkossProp("nitroalkoss-plant");
        //玛瑞斯-设施
        new MantleTunnel("mantle-tunnel");



        //西楚洛斯-西瓜地形
        new Floor("melonwater-deep");
        new Floor("melonwater");
        new Floor("melondirt-water");
        new Floor("melondirt");
        new StaticWall("melondirt-wall");
        new Floor("melondirt-white");
        new Floor("melon-vascular");
        new Floor("melon-shell");
        new Floor("melon-shell-dark");
        new StaticWall("melon-shell-wall");
        new Prop("watermelon");
        //西楚洛斯-孢染地形
        new Floor("spore-melonwater-deep");
        new Floor("spore-melonwater");
        new Floor("spore-melondirt");
        new Floor("spore-melondirt-moss");
        new StaticWall("spore-melondirt-wall");
        //西楚洛斯-苍穹基地
        new Floor("zenith-base-lamp-unlit");
        new Floor("zenith-base-lamp");
        new Floor("zenith-base-lamp-red");
        new Floor("zenith-base-lamp-green");
        new Floor("zenith-base-lamp-purple");



        //加维纳
        new Floor("dysharmony-fluid-floor"){{cacheLayer = VECacheLayer.dysharmony;}};



        //普罗克西玛



    }
}
