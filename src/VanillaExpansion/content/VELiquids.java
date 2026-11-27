package VanillaExpansion.content;
import arc.graphics.Color;
import mindustry.content.StatusEffects;
import mindustry.type.Liquid;
public class VELiquids {
    public static Liquid steam,hotSteam,superhotSteam,ultrahotSteam,spentSteam,moltenCore;

    public static void load(){
        steam = new Liquid("steam", Color.valueOf("e5e5e5")){{
            gas = true;
            temperature = 1.5f;
            heatCapacity = 0.8f;
            viscosity = 0.1f;
            explosiveness = 0.1f;
            effect = StatusEffects.wet;
        }};
        hotSteam = new Liquid("hot-steam", Color.valueOf("E7D6D6")){{
            gas = true;
            temperature = 2f;
            heatCapacity = 1f;
            viscosity = 0.1f;
            explosiveness = 0.12f;
            effect = StatusEffects.wet;
        }};
        superhotSteam = new Liquid("superhot-steam", Color.valueOf("E7B7B7")){{
            gas = true;
            temperature = 3f;
            heatCapacity = 1.2f;
            viscosity = 0.1f;
            explosiveness = 0.15f;
            effect = StatusEffects.wet;
        }};
        ultrahotSteam = new Liquid("ultrahot-steam", Color.valueOf("E39393")){{
            gas = true;
            temperature = 4f;
            heatCapacity = 1.4f;
            viscosity = 0.1f;
            explosiveness = 0.18f;
            effect = StatusEffects.wet;
        }};
        spentSteam = new Liquid("spent-steam", Color.valueOf("445772")){{
            gas = true;
            temperature = 0.5f;
            heatCapacity = 0.6f;
            viscosity = 0.12f;
            explosiveness = 0f;
            effect = StatusEffects.wet;
        }};
        moltenCore = new Liquid("molten-core", Color.valueOf("ff5a1f")){{
            viscosity = 0.95f;
            temperature = 10f;
            heatCapacity = 5f;
            flammability = 0f;
            explosiveness = 0f;
            effect = StatusEffects.burning;
            gasColor = Color.valueOf("ff3d00").a(0.4f);
        }};
    }
}
