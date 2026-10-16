package VanillaExpansion;

import VanillaExpansion.content.VEJSLiquids;
import arc.Events;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mods;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.meta.Env;

import static mindustry.Vars.state;

public class InitCorrosion {

    private static float timer = 0f;
    /** 动态腐蚀检测间隔 */
    private static float checkInterval;
    /** 因功能免疫沉积酸腐蚀的建筑 */
    public static Seq<String> acidBlockWhitelist1 = Seq.with(
            "liquid-source","liquid-void","incinerator"
    );
    /** 因带有防护而免疫沉积酸腐蚀的建筑 */
    public static Seq<String> acidBlockWhitelist2 = Seq.with(
            "ve-silicide-fluid-source","ve-silicide-fluid-void",
            "ve-silver-conduit","ve-silver-conduit-armored","ve-valve-fluid-cross","ve-valve-fluid-distribute",
            "ve-silver-bridge","ve-chained-pump","ve-fluid-sorter","ve-acid-resistant-pump","ve-acid-resistant-conduit"
    );
    /** 仅在非灼热环境下免疫沉积酸腐蚀的建筑 */
    public static Seq<String> acidErekirBlockWhitelist = Seq.with(
            "reinforced-conduit","reinforced-bridge-conduit","reinforced-liquid-junction",
            "reinforced-liquid-router","reinforced-liquid-container","reinforced-liquid-tank",
            "reinforced-pump","slag-incinerator"
    );
    /** 会被镓腐蚀的建筑 */
    public static Seq<String> galliumBlacklist = Seq.with(
            "ve-isomorphic-conduit","ve-fluid-router","ve-isomorphic-bridge-conduit"
    );
    /** 是否触发了沉积酸腐蚀 */
    public static boolean hasAcidCorrosion;
    /** 是否触发了镓腐蚀 */
    public static boolean hasGalliumCorrosion;
    /** 检测到附属模组VEEE */
    public static boolean veee = false;

    public static void init(){

        //全图腐蚀处理
        Events.run(EventType.Trigger.update, () -> {
            if(state.isPaused() || !state.rules.fire) return;
            timer += Time.delta;
            if(checkInterval < 10f){
                checkInterval = 60f;//防止卡毙掉
            }
            if(timer < checkInterval) return;
            timer = 0f;
            hasAcidCorrosion = hasGalliumCorrosion = false;
            for(Building building : Groups.build){
                if(building.block instanceof LiquidBlock || building.block instanceof LiquidBridge){
                    if(building.liquids.get(VEJSLiquids.acid) > 0.01f &&
                            !acidBlockWhitelist1.contains(building.block.name) &&
                            !acidBlockWhitelist2.contains(building.block.name) &&
                            !(acidErekirBlockWhitelist.contains(building.block.name) && !state.rules.hasEnv(Env.scorching))
                    ){
                        if(Mathf.chanceDelta(0.33f)) {
                            building.damagePierce(Math.abs(Mathf.range(0.01f, 0.1f)) * building.block.health * (building.liquids.get(VEJSLiquids.acid) / building.block.liquidCapacity));
                            building.liquids.remove(VEJSLiquids.acid, 0.1f);
                        }
                        hasAcidCorrosion = true;
                    }
                    if(veee && building.liquids.get(Liquids.gallium) > 0.01f &&
                            galliumBlacklist.contains(building.block.name)
                    ){
                        if(Mathf.chanceDelta(0.33f)) {
                            building.damagePierce(Math.abs(Mathf.range(0.005f, 0.05f)) * building.block.health * (building.liquids.get(Liquids.gallium) / building.block.liquidCapacity));
                            building.liquids.remove(Liquids.gallium, 0.01f);
                        }
                        hasGalliumCorrosion = true;
                    }
                }
            }
            if(hasAcidCorrosion || hasGalliumCorrosion){
                checkInterval = 10f;
            }else{
                checkInterval = 60f;
            }
        });

        Events.run(EventType.WorldLoadEndEvent.class, () -> {
                Seq<Mods.LoadedMod> m = Vars.mods.getMods();
                veee = false;
                if (m != null) {
                    for (Mods.LoadedMod mod : m) {
                        if (mod.name.startsWith("veee-") && mod.enabled() &&
                                mod.dependencies.contains(mo -> mo.name.equals("ve"))) {
                            veee = true;
                        }
                    }
                }
        });
    }
}
