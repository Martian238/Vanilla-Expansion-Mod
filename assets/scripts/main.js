MapResizeDialog.minSize = 5
MapResizeDialog.maxSize = 1000
Vars.maxSchematicSize = 600
require("sectorSize");
//require("base/library");
require("units");
require("items");
Vars.renderer.maxZoom = 25;
Vars.renderer.minZoom = 0.2;
Vars.appName = "Mindustry: Vanilla Expansion";
//require("xfkjqjs");
/*
Team.green.color.set(Color.valueOf("00ffce"));
Team.green.palette[0].set(Color.valueOf("7effea"));
Team.green.palette[1].set(Color.valueOf("3ad2b5"));
Team.green.palette[2].set(Color.valueOf("189886"));
Team.green.hasPalette = true;
Team.green.ignoreUnitCap = true;
Team.green.name = "lacuna";
*/
Planets.tantros.visible = true;
require("blocks");
log("endblocks");
require("sectors");
log("endsectors");
require("team2");
log("endteam2");
//require("planets");
log("endplanets");
