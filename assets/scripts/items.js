function newItem(name) {
	exports[name] = extend(Item, name, {});
}
function newLiquid(name) {
	exports[name] = extend(Liquid, name, {});
}
function newCellLiquid(name) {
	exports[name] = extend(CellLiquid, name, {});
}

newItem("aluminium");
newItem("quartz");
newItem("catalyzon");
newItem("silicide");
newItem("salt");
newItem("plant-matter");
newItem("chromium");
newItem("sodium");
newItem("carbon-shale-cobble");
newItem("shale-cobble");
newItem("nitroalkoss");
newItem("cobalt");
newItem("fibralt");
newItem("fusion-fuel");
newItem("capacitor");
newItem("warp-nucleus");

newItem("red-soil");
newItem("ferrum");
newItem("reflector-matter");
newItem("ferric-shale-cobble");
newItem("silver");
newItem("tantalum");
newItem("astro-plate");
newItem("phecteel");

newItem("melon-dirt");
newItem("sugar");
newItem("dense-melon-dirt");
newItem("crystallon");
newItem("mect-complex");

newItem("motiphite");
newItem("technolite");
newItem("chrysopite");
newItem("finallite");

newLiquid("lava");
newLiquid("chlorine");
newLiquid("melon-water");
newCellLiquid("melon-water-corrupted");
newCellLiquid("dysharmony-fluid");