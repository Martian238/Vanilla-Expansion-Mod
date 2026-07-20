//f
function newPlanet(name, parent, radius) {
	exports[name] = extend(Planet, name, parent, radius, {});
}
function newSector(name, planet, position) {
	exports[name] = extend(SectorPreset, name, planet, position, {});
}

log("ser");
newSector("classic-175", Planets.serpulo, 175);
newSector("classic-222", Planets.serpulo, 222);
newSector("classic-223", Planets.serpulo, 218);
newSector("classic-85", Planets.serpulo, 125);
newSector("classic-7", Planets.serpulo, 7);
newSector("classic-133", Planets.serpulo, 133);
newSector("classic-195", Planets.serpulo, 195);

log("cyc1");
newSector("barrier-hill", Planets.tantros, 0);
newSector("carbon-relics", Planets.tantros, 0);
newSector("wetland-miningfield", Planets.tantros, 0);
newSector("intervalley", Planets.tantros, 0);
log("cyc2");
newSector("volcanic-top", Planets.tantros, 0);
newSector("fort-port", Planets.tantros, 0);
newSector("fungus-factory", Planets.tantros, 0);
newSector("gobi", Planets.tantros, 0);
newSector("isolate-island", Planets.tantros, 0);
newSector("shale-quarry", Planets.tantros, 0);
newSector("unit-laboratory", Planets.tantros, 0);
log("cyc3");
newSector("thorium-hacienda", Planets.tantros, 0);
newSector("blast-test", Planets.tantros, 0);
newSector("ruins", Planets.tantros, 0);
newSector("silicon-facility", Planets.tantros, 0);
newSector("cross-forest", Planets.tantros, 0);
newSector("warp-tech-base", Planets.tantros, 0);
newSector("nuclear-powerplant", Planets.tantros, 0);
log("cyc4");
newSector("prime-route", Planets.tantros, 0);
newSector("planetary-cargo-center", Planets.tantros, 0);
newSector("retreat-zone", Planets.tantros, 0);

log("photutorial");
newSector("tutorial-floor-crusher", Planets.tantros, 0);
newSector("tutorial-blocking-wall", Planets.tantros, 0);
newSector("tutorial-assemble-selector", Planets.tantros, 0);
log("phohc");
newSector("hardcore-barrier-hill", Planets.tantros, 0);
newSector("hardcore-carbon-relics", Planets.tantros, 0);
newSector("hardcore-intervalley", Planets.tantros, 0);
log("phocommunity");
newSector("community-entrance", Planets.tantros, 0);
newSector("community-huoshankou", Planets.tantros, 0);
newSector("community-chiyanheliu", Planets.tantros, 0);
newSector("community-lieguqianshao", Planets.tantros, 0);
newSector("community-leimingliegu", Planets.tantros, 0);
newSector("community-xingyunpendi", Planets.tantros, 0);
newSector("community-xingyunxueshan", Planets.tantros, 0);
newSector("community-yunshikeng", Planets.tantros, 0);

log("mar0");
newSector("negotiation", Planets.tantros, 0);
newSector("temporary-frontier", Planets.tantros, 0);
newSector("industrial-hub", Planets.tantros, 0);
newSector("war-headquarters", Planets.tantros, 0);
newSector("reunion", Planets.tantros, 0);
log("mar1");
newSector("mirror-swamp", Planets.tantros, 0);
newSector("misty-valley", Planets.tantros, 0);
newSector("halo-mountains", Planets.tantros, 0);
newSector("third-facility", Planets.tantros, 0);
log("mar2");
newSector("icy-boundary", Planets.tantros, 0);
newSector("metallic-glacier", Planets.tantros, 0);
newSector("radioactive-islands", Planets.tantros, 0);
newSector("omni-monitor", Planets.tantros, 0);
log("mar3");
newSector("lower-pass", Planets.tantros, 0);
newSector("slate-guardpost", Planets.tantros, 0);
newSector("surface-cracks", Planets.tantros, 0);
newSector("thermal-powerplant", Planets.tantros, 0);

log("sit0");
newSector("melon-onset", Planets.tantros, 0);
log("sit1");
newSector("fruit-flats", Planets.tantros, 0);
log("sit2");
newSector("boss-fungitron", Planets.tantros, 0);
log("sit3");
newSector("boss-zentack", Planets.tantros, 0);
log("sit4");
newSector("boss-chiniun", Planets.tantros, 0);

log("complete");