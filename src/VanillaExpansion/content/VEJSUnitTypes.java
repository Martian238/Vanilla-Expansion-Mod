package VanillaExpansion.content;

import VanillaExpansion.expand.type.unit.*;
import mindustry.content.Fx;
import mindustry.gen.*;
import mindustry.type.UnitType;

public class VEJSUnitTypes {

    //Core units
    public static UnitType theta, lambda, sigma, deltaUnit, omega;
    public static UnitType zetaUnit, eta;
    public static UnitType iota, iotaFungikiller, iotaStealth, iotaStg, iotaFinal;

    //Cyclant new types
    public static UnitType conscript, arrange, charge, capture;
    public static DayunTankUnitType triumph;
    public static UnitType dust, mist, haze, hurricane, meteorology, flocculate, alleviate;
    public static UnitType smarb, mider, velocite, slidoid, hovopid;

    //Strengthened & Remoulded
    public static UnitType blade, hammer, ballistic, firelock, ray;
    public static UnitType stardust, vortex, nebula, galaxy, universe;
    public static UnitType aurora, plasma, solar, magnetic, corona;
    public static UnitType pointUnit, lineUnit, squareUnit, stereo, metaUnit;
    public static UnitType lance, hoe, astrologe, prominence, stringUnit, plain;

    //Special
    public static UnitType
            assemblyDrone, assemblyDroneHyper, skibidiBryde,
            flyLaserDrill, egnarra, wideFortress, magneticSmall,
            thoriumBlaster, thoriumBomber, thoriumRocketeer,
            duplicator, alev, antumbright, eclire, toxorpion;
    public static UnitType frostFortress;
    public static IronGolemType ferricFortress;
    public static UnitType fireBee, octPainted;

    //Maress new types
    public static UnitType shimmer, daybreak, sunrise;
    public static UnitType uprise, soar, hover, ambush, dive;
    public static UnitType stink, termite, stinging;

    //Lacuna
    public static UnitType sparkle, plasm, surge, tide, thunder;
    public static UnitType vibrate, shakeUnit, quake;

    //Bosses
    public static UnitType thoriumEradicator, hugeDagger, hyper;

    //Minigame elements
    public static SentryUnitType zenithSentry;

    //Internal
    public static UnitType textTrigger;

    public static void load(){

        theta = new UnitType("theta"){{constructor = UnitEntity::create;}};
        lambda = new UnitType("lambda"){{constructor = UnitEntity::create;}};
        sigma = new UnitType("sigma"){{constructor = UnitEntity::create;}};
        deltaUnit = new UnitType("delta"){{constructor = PayloadUnit::create;}};
        omega = new UnitType("omega"){{constructor = PayloadUnit::create;}};

        zetaUnit = new UnitType("zeta"){{constructor = LegsUnit::create;}};
        eta = new UnitType("eta"){{constructor = LegsUnit::create;}};

        iota = new UnitType("iota"){{constructor = UnitEntity::create;}};
        iotaFungikiller = new UnitType("iota-fungikiller"){{constructor = UnitEntity::create;}};
        iotaStealth = new UnitType("iota-stealth"){{constructor = ElevationMoveUnit::create;}};
        iotaStg = new UnitType("iota-stg"){{constructor = UnitEntity::create;}};
        iotaFinal = new UnitType("iota-final"){{constructor = ElevationMoveUnit::create;}};

        conscript = new UnitType("conscript"){{constructor = TankUnit::create;}};
        arrange = new UnitType("arrange"){{constructor = TankUnit::create;}};
        charge = new UnitType("charge"){{constructor = TankUnit::create;}};
        capture = new UnitType("capture"){{constructor = TankUnit::create;}};
        triumph = new DayunTankUnitType("triumph"){{constructor = DayunTankUnit::create;}};

        dust = new UnitType("dust"){{constructor = ElevationMoveUnit::create;}};
        mist = new UnitType("mist"){{constructor = ElevationMoveUnit::create;}};
        haze = new UnitType("haze"){{constructor = ElevationMoveUnit::create;}};
        hurricane = new UnitType("hurricane"){{constructor = ElevationMoveUnit::create;}};
        meteorology = new UnitType("meteorology"){{constructor = ElevationMoveUnit::create;}};
        flocculate = new UnitType("flocculate"){{constructor = ElevationMoveUnit::create;}};
        alleviate = new UnitType("alleviate"){{constructor = ElevationMoveUnit::create;}};

        smarb = new UnitType("smarb"){{constructor = LegsUnit::create;}};
        mider = new UnitType("mider"){{constructor = LegsUnit::create;}};
        velocite = new UnitType("velocite"){{constructor = LegsUnit::create;}};
        slidoid = new UnitType("slidoid"){{constructor = LegsUnit::create;}};
        hovopid = new UnitType("hovopid"){{constructor = LegsUnit::create;}};

        blade = new UnitType("blade"){{constructor = MechUnit::create;}};
        hammer = new UnitType("hammer"){{constructor = MechUnit::create;}};
        ballistic = new UnitType("ballistic"){{constructor = MechUnit::create;}};
        firelock = new UnitType("firelock"){{constructor = MechUnit::create;}};
        ray = new UnitType("ray"){{constructor = MechUnit::create;}};

        stardust = new UnitType("stardust"){{constructor = MechUnit::create;}};
        vortex = new UnitType("vortex"){{constructor = MechUnit::create;}};
        nebula = new UnitType("nebula"){{constructor = MechUnit::create;}};
        galaxy = new UnitType("galaxy"){{constructor = MechUnit::create;}};
        universe = new UnitType("universe"){{constructor = MechUnit::create;}};

        aurora = new UnitType("aurora"){{constructor = UnitEntity::create;}};
        plasma = new UnitType("plasma"){{constructor = UnitEntity::create;}};
        solar = new UnitType("solar"){{constructor = UnitEntity::create;}};
        magnetic = new UnitType("magnetic"){{constructor = UnitEntity::create;}};
        corona = new UnitType("corona"){{constructor = UnitEntity::create;}};

        pointUnit = new UnitType("point"){{constructor = UnitEntity::create;}};
        lineUnit = new UnitType("line"){{constructor = UnitEntity::create;}};
        squareUnit = new UnitType("square"){{constructor = PayloadUnit::create;}};
        stereo = new UnitType("stereo"){{constructor = PayloadUnit::create;}};
        metaUnit = new UnitType("meta"){{constructor = PayloadUnit::create;}};

        lance = new UnitType("lance"){{constructor = MechUnit::create;}};
        hoe = new UnitType("hoe"){{constructor = MechUnit::create;}};
        astrologe = new UnitType("astrologe"){{constructor = MechUnit::create;}};
        prominence = new UnitType("prominence"){{constructor = PayloadUnit::create;}};
        stringUnit = new UnitType("string"){{constructor = UnitEntity::create;}};
        plain = new UnitType("plain"){{constructor = PayloadUnit::create;}};

        assemblyDrone = new UnitType("assembly-drone-ve"){{constructor = BuildingTetherPayloadUnit::create;}};
        assemblyDroneHyper = new UnitType("assembly-drone-hyper"){{constructor = BuildingTetherPayloadUnit::create;}};
        skibidiBryde = new UnitType("skibidi-bryde"){{constructor = TankUnit::create;}};
        flyLaserDrill = new UnitType("fly-laser-drill"){{constructor = UnitEntity::create;}};
        egnarra = new UnitType("egnarra"){{constructor = TankUnit::create;}};
        wideFortress = new UnitType("wide-fortress"){{constructor = MechUnit::create;}};
        magneticSmall = new UnitType("magnetic-small"){{constructor = UnitEntity::create;}};
        thoriumBlaster = new UnitType("thorium-blaster"){{constructor = LegsUnit::create;}};
        thoriumBomber = new UnitType("thorium-bomber"){{constructor = UnitEntity::create;}};
        thoriumRocketeer = new UnitType("thorium-rocketeer"){{constructor = UnitWaterMove::create;}};
        duplicator = new UnitType("duplicator"){{constructor = UnitEntity::create;}};
        alev = new UnitType("alev"){{constructor = MechUnit::create;}};
        antumbright = new UnitType("antumbright"){{constructor = UnitEntity::create;}};
        eclire = new UnitType("eclire"){{constructor = UnitEntity::create;}};
        toxorpion = new UnitType("toxorpion"){{constructor = LegsUnit::create;}};

        frostFortress = new UnitType("frost-fortress"){{constructor = MechUnit::create;}};
        ferricFortress = new IronGolemType("ferric-fortress"){{
            constructor = IronGolemUnit::create;
            invincibleTime = 29f;
        }};
        fireBee = new UnitType("firebee"){{constructor = UnitEntity::create;}};
        octPainted = new UnitType("oct-painted"){{constructor = PayloadUnit::create;}};

        shimmer = new UnitType("shimmer"){{constructor = UnitEntity::create;}};
        daybreak = new UnitType("daybreak"){{constructor = UnitEntity::create;}};
        sunrise = new UnitType("sunrise"){{constructor = UnitEntity::create;}};

        uprise = new UnitType("uprise"){{constructor = UnitEntity::create;}};
        soar = new UnitType("soar"){{constructor = UnitEntity::create;}};
        hover = new UnitType("hover"){{constructor = UnitEntity::create;}};
        ambush = new UnitType("ambush"){{constructor = UnitEntity::create;}};
        dive = new UnitType("dive"){{constructor = UnitEntity::create;}};

        stink = new UnitType("stink"){{constructor = MechUnit::create;}};
        termite = new UnitType("termite"){{constructor = LegsUnit::create;}};
        stinging = new UnitType("stinging"){{constructor = UnitEntity::create;}};

        sparkle = new UnitType("sparkle"){{constructor = PayloadUnit::create;}};
        plasm = new UnitType("plasm"){{constructor = PayloadUnit::create;}};
        surge = new UnitType("surge"){{constructor = PayloadUnit::create;}};
        tide = new UnitType("tide"){{constructor = PayloadUnit::create;}};
        thunder = new UnitType("thunder"){{constructor = PayloadUnit::create;}};

        vibrate = new UnitType("vibrate"){{constructor = TankUnit::create;}};
        shakeUnit = new UnitType("shake"){{constructor = TankUnit::create;}};
        quake = new UnitType("quake"){{constructor = TankUnit::create;}};

        thoriumEradicator = new UnitType("thorium-eradicator"){{constructor = LegsUnit::create;}};
        hugeDagger = new UnitType("huge-dagger"){{constructor = MechUnit::create;}};
        hyper = new UnitType("hyper"){{constructor = HyperUnit::create;}};

        textTrigger = new UnitType("text-trigger"){{
            constructor = TimedKillUnit::create;
            lifetime = 30f;
            isEnemy = drawMinimap = false;
            hidden = true;
            targetable = hittable = false;
            drawBody = drawCell = drawSoftShadow = false;
            health = 9999999f;
            deathShake = deathSoundVolume = 0f;
            createWreck = createScorch = false;
            deathExplosionEffect = fallEffect = fallEngineEffect = Fx.none;
            wreckSound = Sounds.none;
        }};

        zenithSentry = new SentryUnitType("zenith-sentry"){{constructor = SentryUnit::create;}};
    }
}
