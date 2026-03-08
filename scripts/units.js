/*感谢 废土科技 MOD的js参考*/
function newUnit(name, unitType) {
	const u = extend(UnitType, name, {});
	u.constructor = () => extend(unitType, {});
	return exports[name] = u;
}
/*"flying" -> UnitEntity;
"mech" -> MechUnit;
"legs" -> LegsUnit;
"naval" -> UnitWaterMove;
"payload" -> PayloadUnit;
"missile" -> TimedKillUnit;
"tank" -> TankUnit;
"hover" -> ElevationMoveUnit;
"tether" -> BuildingTetherPayloadUnit;
"crawl" -> CrawlUnit;*/

newUnit("theta",UnitEntity);
newUnit("lambda",UnitEntity);
newUnit("sigma",UnitEntity);
newUnit("delta",PayloadUnit);
newUnit("omega",PayloadUnit);

newUnit("conscript",TankUnit);
newUnit("arrange",TankUnit);
newUnit("charge",TankUnit);
newUnit("capture",TankUnit);
newUnit("triumph",TankUnit);

newUnit("dust",ElevationMoveUnit);
newUnit("mist",ElevationMoveUnit);
newUnit("haze",ElevationMoveUnit);
newUnit("hurricane",ElevationMoveUnit);
newUnit("meteorology",ElevationMoveUnit);
newUnit("flocculate",ElevationMoveUnit);
newUnit("alleviate",ElevationMoveUnit);

newUnit("smarb",LegsUnit);
newUnit("mider",LegsUnit);
newUnit("velocite",LegsUnit);
newUnit("slidoid",LegsUnit);
newUnit("hovopid",LegsUnit);

newUnit("blade",MechUnit);
newUnit("hammer",MechUnit);
newUnit("ballistic",MechUnit);
newUnit("firelock",MechUnit);
newUnit("ray",MechUnit);

newUnit("stardust",MechUnit);
newUnit("vortex",MechUnit);
newUnit("nebula",MechUnit);
newUnit("galaxy",MechUnit);
newUnit("universe",MechUnit);

newUnit("aurora",UnitEntity);
newUnit("plasma",UnitEntity);
newUnit("solar",UnitEntity);
newUnit("magnetic",UnitEntity);
newUnit("corona",UnitEntity);

newUnit("point",UnitEntity);
newUnit("line",UnitEntity);
newUnit("square",PayloadUnit);
newUnit("stereo",PayloadUnit);
newUnit("meta",PayloadUnit);

newUnit("lance",MechUnit);
newUnit("hoe",MechUnit);
newUnit("astrologe",MechUnit);
newUnit("prominence",PayloadUnit);
newUnit("stellar",UnitEntity);
newUnit("string",UnitEntity);
newUnit("plain",PayloadUnit);

newUnit("assembly-drone-ve",BuildingTetherPayloadUnit);
newUnit("assembly-drone-hyper",BuildingTetherPayloadUnit);
newUnit("fly-laser-drill",UnitEntity);
newUnit("egnarra",TankUnit);
newUnit("wide-fortress",MechUnit);
newUnit("magnetic-small",UnitEntity);
newUnit("thorium-blaster",LegsUnit);
newUnit("thorium-bomber",UnitEntity);
newUnit("thorium-rocketeer",UnitWaterMove);
newUnit("duplicator",UnitEntity);
newUnit("alev",MechUnit);
newUnit("antumbright",UnitEntity);
newUnit("eclire",UnitEntity);
newUnit("toxorpion",LegsUnit);

newUnit("iota",UnitEntity);
newUnit("iota-fungikiller",UnitEntity);
newUnit("iota-stg",UnitEntity);
newUnit("oct-painted",PayloadUnit);

newUnit("zeta",LegsUnit);
newUnit("eta",LegsUnit);

newUnit("shimmer",UnitEntity);
newUnit("daybreak",UnitEntity);
newUnit("sunrise",UnitEntity);

newUnit("uprise",UnitEntity);
newUnit("soar",UnitEntity);
newUnit("hover",UnitEntity);
newUnit("ambush",UnitEntity);
newUnit("dive",UnitEntity);

newUnit("sparkle",PayloadUnit);
newUnit("plasm",PayloadUnit);
newUnit("surge",PayloadUnit);
newUnit("tide",PayloadUnit);
newUnit("thunder",PayloadUnit);

newUnit("vibrate",TankUnit);
newUnit("shake",TankUnit);
newUnit("quake",TankUnit);

newUnit("thorium-eradicator",LegsUnit);
newUnit("huge-dagger",MechUnit);
newUnit("hyper",PayloadUnit);
