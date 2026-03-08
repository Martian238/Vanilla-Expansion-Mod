const sol2 = new Planet("sol2", Planets.sun, 2, 3.75);
const cyclant = new Planet("cyclant", sol2, 1, 3.75);

const barrier-hill = new SectorPreset("barrier-hill", cyclant, 170);
exports.barrier-hill = barrier-hill;
const carbon-relics = new SectorPreset("carbon-relics", cyclant, 35);
exports.carbon-relics = carbon-relics;
const wetland-miningfield = new SectorPreset("wetland-miningfield", cyclant, 20);
exports.wetland-miningfield = wetland-miningfield;