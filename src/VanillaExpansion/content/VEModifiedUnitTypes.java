package VanillaExpansion.content;

import arc.graphics.Blending;
import arc.graphics.Color;
import mindustry.content.UnitTypes;
import mindustry.entities.part.RegionPart;
import mindustry.graphics.Pal;
import mindustry.type.Planet;
import mindustry.type.UnitType;

public class VEModifiedUnitTypes {

    public static void load(){

        generalShownPlanetsSettings();
        generalDatabaseTag();
        generalModify();

    }

    public static void generalModify(){
        UnitType cor = UnitTypes.corvus;
        cor.targetUnderBlocks = false;
        cor.speed = 0.4f;
        cor.legCount = 6;
        cor.legMaxLength = 2;
        cor.legContinuousMove = true;
        cor.legSplashDamage = 80;
        cor.legSplashRange = 12;
        cor.rippleScale = 1.25f;
        cor.legForwardScl = 0.8f;
        cor.range = 456;
        cor.parts.add(new RegionPart(){{
            suffix = "-glow";
            outline = under = false;
            blending = Blending.additive;
            progress = PartProgress.charge.mul(4f).add(PartProgress.heat.mul(PartProgress.heat));
            color = Pal.heal.cpy().a(0f);
            colorTo = Pal.heal.cpy().a(0.6f);
        }});
    }

    public static void generalDatabaseTag(){
        addDatabaseTag("01core-unit",
                UnitTypes.alpha, UnitTypes.beta, UnitTypes.gamma,
                UnitTypes.evoke, UnitTypes.incite, UnitTypes.emanate);
        addDatabaseTag("02serpulo-ground-unit",
                UnitTypes.dagger, UnitTypes.mace, UnitTypes.fortress, UnitTypes.scepter, UnitTypes.reign,
                UnitTypes.nova, UnitTypes.pulsar, UnitTypes.quasar, UnitTypes.vela, UnitTypes.corvus,
                UnitTypes.crawler, UnitTypes.atrax, UnitTypes.spiroct, UnitTypes.arkyid, UnitTypes.toxopid);
        addDatabaseTag("03serpulo-air-unit",
                UnitTypes.flare, UnitTypes.horizon, UnitTypes.zenith, UnitTypes.antumbra, UnitTypes.eclipse,
                UnitTypes.mono, UnitTypes.poly, UnitTypes.mega, UnitTypes.quad, UnitTypes.oct);
        addDatabaseTag("04-serpulo-naval-unit",
                UnitTypes.risso, UnitTypes.minke, UnitTypes.bryde, UnitTypes.sei, UnitTypes.omura,
                UnitTypes.retusa, UnitTypes.oxynoe, UnitTypes.cyerce, UnitTypes.aegires, UnitTypes.navanax);
        addDatabaseTag("05erekir-tank-unit",
                UnitTypes.stell, UnitTypes.locus, UnitTypes.precept, UnitTypes.vanquish, UnitTypes.conquer);
        addDatabaseTag("06erekir-ship-unit",
                UnitTypes.elude, UnitTypes.avert, UnitTypes.obviate, UnitTypes.quell, UnitTypes.disrupt);
        addDatabaseTag("07erekir-mech-unit",
                UnitTypes.merui, UnitTypes.cleroi, UnitTypes.anthicus, UnitTypes.tecta, UnitTypes.collaris);
    }

    public static void generalShownPlanetsSettings(){
        addShownPlanet(VEJSPlanets.cyclant,
                UnitTypes.dagger, UnitTypes.mace, UnitTypes.fortress, UnitTypes.scepter, UnitTypes.reign,
                UnitTypes.nova, UnitTypes.pulsar, UnitTypes.quasar, UnitTypes.vela, UnitTypes.corvus,
                UnitTypes.crawler, UnitTypes.atrax, UnitTypes.spiroct, UnitTypes.arkyid, UnitTypes.toxopid,
                UnitTypes.flare, UnitTypes.horizon, UnitTypes.zenith, UnitTypes.antumbra, UnitTypes.eclipse,
                UnitTypes.mono, UnitTypes.poly, UnitTypes.mega, UnitTypes.quad, UnitTypes.oct,
                UnitTypes.risso, UnitTypes.minke, UnitTypes.bryde, UnitTypes.sei, UnitTypes.omura,
                UnitTypes.retusa, UnitTypes.oxynoe, UnitTypes.cyerce, UnitTypes.aegires, UnitTypes.navanax,
                UnitTypes.gamma);
        addShownPlanet(VEJSPlanets.phoon,
                UnitTypes.dagger, UnitTypes.mace, UnitTypes.fortress, UnitTypes.scepter, UnitTypes.reign,
                UnitTypes.nova, UnitTypes.pulsar, UnitTypes.quasar, UnitTypes.vela, UnitTypes.corvus,
                UnitTypes.crawler, UnitTypes.atrax, UnitTypes.spiroct, UnitTypes.arkyid, UnitTypes.toxopid,
                UnitTypes.flare, UnitTypes.horizon, UnitTypes.zenith, UnitTypes.antumbra, UnitTypes.eclipse,
                UnitTypes.mono, UnitTypes.poly, UnitTypes.mega, UnitTypes.quad, UnitTypes.oct,
                UnitTypes.risso, UnitTypes.minke, UnitTypes.bryde, UnitTypes.sei, UnitTypes.omura,
                UnitTypes.retusa, UnitTypes.oxynoe, UnitTypes.cyerce, UnitTypes.aegires, UnitTypes.navanax,
                UnitTypes.gamma);
        addShownPlanet(VEJSPlanets.thavina,
                UnitTypes.dagger, UnitTypes.mace, UnitTypes.fortress, UnitTypes.scepter, UnitTypes.reign,
                UnitTypes.nova, UnitTypes.pulsar, UnitTypes.quasar, UnitTypes.vela, UnitTypes.corvus,
                UnitTypes.crawler, UnitTypes.atrax, UnitTypes.spiroct, UnitTypes.arkyid, UnitTypes.toxopid,
                UnitTypes.flare, UnitTypes.horizon, UnitTypes.zenith, UnitTypes.antumbra, UnitTypes.eclipse,
                UnitTypes.mono, UnitTypes.poly, UnitTypes.mega, UnitTypes.quad, UnitTypes.oct,
                UnitTypes.risso, UnitTypes.minke, UnitTypes.bryde, UnitTypes.sei, UnitTypes.omura,
                UnitTypes.retusa, UnitTypes.oxynoe, UnitTypes.cyerce, UnitTypes.aegires, UnitTypes.navanax,
                UnitTypes.alpha, UnitTypes.beta, UnitTypes.gamma);
        addShownPlanet(VEJSPlanets.sitrullus,
                UnitTypes.flare, UnitTypes.horizon, UnitTypes.zenith,
                UnitTypes.mono, UnitTypes.poly, UnitTypes.mega,
                UnitTypes.gamma);
        addShownPlanet(VEJSPlanets.maress,
                UnitTypes.dagger, UnitTypes.mace, UnitTypes.fortress,
                UnitTypes.nova, UnitTypes.pulsar, UnitTypes.quasar,
                UnitTypes.crawler, UnitTypes.atrax, UnitTypes.spiroct,
                UnitTypes.flare, UnitTypes.horizon, UnitTypes.zenith,
                UnitTypes.mono, UnitTypes.poly, UnitTypes.mega,
                UnitTypes.risso, UnitTypes.minke, UnitTypes.bryde,
                UnitTypes.retusa, UnitTypes.oxynoe, UnitTypes.cyerce);
    }

    public static void addDatabaseTag(String tag, UnitType... types){
        for(UnitType type : types){
            type.databaseTag = tag;
        }
    }

    public static void addShownPlanet(Planet planet, UnitType... types){
        for(UnitType type : types){
            type.shownPlanets.add(planet);
        }
    }

}
