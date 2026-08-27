package VanillaExpansion.content;

import VanillaExpansion.expand.type.unit.HyperUnit;
import mindustry.type.UnitType;

public class VEJSUnitTypes {

    public static UnitType hyper;

    public static void load(){


        hyper = new UnitType("hyper"){{
            constructor = HyperUnit::create;
        }};
    }
}
