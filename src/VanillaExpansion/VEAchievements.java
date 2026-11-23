package VanillaExpansion;

import VanillaExpansion.expand.ui.AchievementRegistry;
import arc.Core;
import arc.scene.style.TextureRegionDrawable;

public class VEAchievements {

    public static void load(){

        //通关星球
        register("planet-serpulo", "planet-serpulo",
                "core-shard-ui", 3, 1);
        register("planet-cyclant", "planet-cyclant",
                "core-nucleus-ui", 4, 1);
        register("planet-maress", "planet-maress",
                "isomorphic-core-nucleus", 5, 1);
        register("planet-sitrullus", "planet-sitrullus",
                "core-general", 4, 1);
        register("planet-thavina", "planet-thavina",
                "core-singularity", 6, 1);

        //主线战役
        register("ch1-barrier-hill", "ch1-barrier-hill",
                "sector-ve-barrier-hill", 1, 2);
        register("ch1-intervalley", "ch1-intervalley",
                "sector-ve-intervalley", 1, 2);
        register("ch1-fungus-factory", "ch1-fungus-factory",
                "sector-ve-fungus-factory", 1, 2);
        register("ch1-unit-laboratory", "ch1-unit-laboratory",
                "sector-ve-unit-laboratory", 1, 2);
        register("ch1-nuclear-powerplant", "ch1-nuclear-powerplant",
                "sector-ve-nuclear-powerplant", 1, 2);
        register("ch1-planetary-cargo-center", "ch1-planetary-cargo-center",
                "sector-ve-planetary-cargo-center", 1, 2);
        register("ch2-negotiation", "ch2-negotiation",
                "sector-ve-negotiation", 1, 2);
        register("chs-melon-onset", "chs-melon-onset",
                "sector-ve-melon-onset", 1, 1);

        //击败头目
        register("boss-hyper", "boss-hyper",
                "ve-bar-hyper", 5, 2);
        register("boss-fungitron", "boss-fungitron",
                "ve-bar-fungitron", 4, 2);
        register("boss-zentack", "boss-zentack",
                "ve-bar-zentack", 4, 3);
        register("boss-chiniunkun", "boss-chiniunkun",
                "ve-bar-chiniun", 5, 3);

        //支线战役
        register("ch1s-tutorials", "ch1s-tutorials",
                "sector-ve-tutorial-assemble-selector", 1, 1);
        register("ch1s-isolate-island", "ch1s-isolate-island",
                "sector-ve-isolate-island", 1, 2);
        register("ch1s-blast-test", "ch1s-blast-test",
                "sector-ve-blast-test", 1, 2);

        //其他
        register("ot-teleport-blast", "ot-teleport-blast",
                "ve-thorium-blaster-full", 1, 1);
        register("ot-hyper-mine", "ot-hyper-mine",
                "unit-mono-full", 1, 1);
        register("ot-phase-boom", "ot-phase-boom",
                "block-ve-config-phase-heater-ui", 1, 1);
        register("ot-orbit-strike", "ot-orbit-strike",
                "ve-orbit-strike-beacon", 1, 1);

    }

    public static void register(String id, String name, String tex, int rarity, int maxLevel){
        AchievementRegistry.register(
                id,// 存档 id
                "achievement.ve-" + name + ".name",// 名称key
                "achievement.ve-" + name + ".desc",// 描述key
                new arc.scene.style.TextureRegionDrawable(Core.atlas.find(tex)),
                rarity,
                maxLevel,
                "achievement.ve-" + name + ".level"
        );
        //new arc.scene.style.TextureRegionDrawable(Core.atlas.find("ve-team-projector-crux-glow"))
    }
}
