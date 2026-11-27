package VanillaExpansion.content;
import static VanillaExpansion.content.VEBlocks.*;
import static VanillaExpansion.content.VEItems.*;
import static mindustry.content.TechTree.*;
public class VETechTree {
    public static void load() {
        VEPlanets.proxima.techTree = nodeRoot("proxima", rockCoreDrill, () -> {
            node(proximaJunction, () -> {
                node(fastSideOutputConveyor, () -> {
                    node(overflow, () -> {
                        node(invertoverflow, () -> {});
                    });
                    node(adaptItemBridge, () -> {});
                });
                node(proximaDuctRouter, () -> {
                    node(proximaSorter, () -> {
                        node(proximaInvertSorter, () -> {});
                    });
                });
            });
            node(sideOutputConduit, () -> {
                node(liquidOverflowGate, () -> {
                    node(liquidUnderflowGate, () -> {});
                });
                node(liquidSorter, () -> {});
                node(adaptLiquidBridge, () -> {});
            });
            node(powerPole, () -> {
                node(test16Dir, () -> {});
            });
            nodeProduce(iron, () -> {
                nodeProduce(manganese, () -> {
                    nodeProduce(gold, () -> {
                        nodeProduce(uranium, () -> {});
                    });
                });
            });
        });
    }
}
