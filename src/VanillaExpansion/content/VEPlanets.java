package VanillaExpansion.content;

import VanillaExpansion.VESounds;
import VanillaExpansion.expand.graphics.AsteroidBeltMesh;
import VanillaExpansion.expand.graphics.MagneticFluxMesh;
import VanillaExpansion.expand.graphics.NeutronJetBeamMesh;
import VanillaExpansion.expand.graphics.NeutronJetParticleMesh;
import VanillaExpansion.expand.graphics.NeutronJetSpinBeam;
import VanillaExpansion.expand.graphics.RotatingMesh;
import VanillaExpansion.expand.graphics.ZAxisSkyMesh;
import VanillaExpansion.expand.maps.NeutronStarPlanetGenerator;
import VanillaExpansion.expand.maps.ProximaPlanetGenerator;
import arc.graphics.Color;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.graphics.g3d.*;
import mindustry.graphics.g3d.PlanetGrid.Ptile;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.Env;

import static mindustry.Vars.content;
import static mindustry.gen.Musics.game4;
import static mindustry.gen.Musics.game8;

/**
 * 比邻星内容定义类
 */
public class VEPlanets {

    /** 星球实例 */
    public static Planet proxima, neutronStar;
    /** 加载行星定义 */
    public static void load() {
        neutronStar = new Planet("sol4b", Planets.sun, 0.9f, 0) {{
            generator = new NeutronStarPlanetGenerator();
            meshLoader = () -> new RotatingMesh(new MultiMesh(new GenericMesh[]{
                new SunMesh(this, 8, 5d, 0.3d, 3d, 1.2d, 0.8d, 1.1f, new Color[]{Color.valueOf("88bbff"), Color.valueOf("88ddff"), Color.valueOf("aaccff"), Color.valueOf("ccddff"), Color.valueOf("eeeeff"), Color.valueOf("ffffff")}),
                new MagneticFluxMesh(this)
            }), 720f);
            cloudMeshLoader = () -> new MultiMesh(
                new NeutronJetParticleMesh(this, 400),
                new NeutronJetBeamMesh(this, 6f, 0.4f, 0.15f){{
                    drawSector = true;
                    sectorTilt = 15f;
                    sectorAngle = 5f;
                    sectorLength = 27f;
                    sectorColor = Color.valueOf("87CEEB");
                    sectorAlpha = 0.3f;
                }},
                new NeutronJetSpinBeam(this, 27f, 0.3f, 0.15f)
            );
            visible = true;
            accessible = false;
            hasAtmosphere = true;
            bloom = true;
            alwaysUnlocked = true;
            iconColor = Color.valueOf("88bbff");
            atmosphereColor = Color.valueOf("8844ff");
            atmosphereRadIn = 0.1f;
            atmosphereRadOut = 0.8f;
            rotateTime = 0.5f;
            orbitRadius = 2000f;
            orbitOffset = 180f;
            camRadius = 1.8f;
            clipRadius = 1.5f;
            startSector = 0;
            drawOrbit = false;
            ruleSetter = r -> {
                r.waves = false;
            };
            allowWaves = false;
            allowSectorInvasion = false;
            allowLaunchToNumbered = false;
            allowLaunchSchematics = false;
            allowLaunchLoadout = false;
            allowLegacyLaunchPads = false;
        }};
        neutronStar.solarSystem = neutronStar;

        proxima = new Planet("proxima", neutronStar, 1f, 3) {{
            generator = new ProximaPlanetGenerator();
            visible = true;
            accessible = false;
            meshLoader = () -> new HexMesh(this, 6);
            cloudMeshLoader = () -> new MultiMesh(
                new AsteroidBeltMesh(this, 2.5f, 4.5f, 150, 729, Color.valueOf("6a6a6a")),
                new ZAxisSkyMesh(this, 2, 0.3f, 0.14f, 5, Color.valueOf("87CEEB").a(0.75f), 2, 0.42f, 1f, 0.43f),
                new ZAxisSkyMesh(this, 3, 0.8f, 0.15f, 5, Color.valueOf("87CEEB").a(0.65f), 2, 0.42f, 1.2f, 0.45f)
            );
            orbitRadius = 65f;
            orbitOffset = 0f;
            tidalLock = true;
            iconColor = Color.valueOf("87CEEB");
            atmosphereColor = Color.valueOf("4A90A4");
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            hasAtmosphere = true;
            startSector = 170;
            alwaysUnlocked = true;
            defaultEnv = mindustry.world.meta.Env.terrestrial;
            ruleSetter = r -> {
                r.waveTeam = mindustry.game.Team.crux;
                r.placeRangeCheck = false;
                r.coreDestroyClear = true;
            };
            allowWaves = true;
            allowLegacyLaunchPads = true;
            allowSectorInvasion = true;
            allowLaunchSchematics = true;
            enemyCoreSpawnReplace = true;
            allowLaunchLoadout = true;
            landCloudColor = Color.valueOf("87CEEB").a(0.5f);
        }};
        proxima.solarSystem = neutronStar;
    }
}
