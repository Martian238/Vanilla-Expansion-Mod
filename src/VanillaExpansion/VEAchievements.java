package VanillaExpansion;

import VanillaExpansion.expand.ui.AchievementRegistry;
import arc.Core;
import arc.scene.style.TextureRegionDrawable;

public class VEAchievements {

    public static void load(){



    }

    public static void register(String id, String name, String tex, int rarity, int maxLevel){
        AchievementRegistry.register(
                id,// 存档 id
                "achievement.ve-" + name + ".name",// 名称key
                "achievement.ve-" + name + ".desc",// 描述key
                new arc.scene.style.TextureRegionDrawable(Core.atlas.find(tex)),
                rarity,
                maxLevel
        );
        //new arc.scene.style.TextureRegionDrawable(Core.atlas.find("ve-team-projector-crux-glow"))
    }
}
