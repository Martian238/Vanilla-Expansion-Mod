package VanillaExpansion;

import VanillaExpansion.MultiCrafter;
import mindustry.world.Block;

public class JavaBlocks {
    public static Block
    fluidSorter, cargoAnchor,
    boiler, heatTurbineGenerator,
    reductionChamber, upgradedReactionPool, configMelter
            ;

    public static void load(){

        fluidSorter = new MultiCrafter("m-fluid-sorter");
        cargoAnchor = new MultiCrafter("cargo-anchor");
        boiler = new MultiCrafter("m-boiler");
        heatTurbineGenerator = new MultiCrafter("heat-turbine-generator");
        reductionChamber = new MultiCrafter("m-reduction-chamber");
        upgradedReactionPool = new MultiCrafter("m-upgraded-reaction-pool");
        configMelter = new MultiCrafter("config-melter");
    }
}
