package VanillaExpansion.content;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.gen.Musics;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.graphics.g3d.NoiseMesh;
import mindustry.graphics.g3d.SunMesh;
import mindustry.type.Planet;
import mindustry.world.blocks.Attributes;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.Env;

public class VEJSPlanets {

    public static Planet sol2, cyclant, phoon, maress, sitrullus, thavina;

    public static void load(){

        sol2 = new Planet("sol2", Planets.sun, 12f){{
            bloom = true;
            accessible = true;
            alwaysUnlocked = false;
            iconColor = Color.valueOf("387aff");
            rotateTime = 600;
            solarSystem = this;
            visible = true;
            orbitRadius = 300;
            drawOrbit = false;
            orbitTime = 12000;
            children = Seq.with(cyclant, maress, sitrullus);

            mesh = new MultiMesh(
                    new SunMesh(this,
                            6, 5, 0.3, 3, 1.2, 0.8, 1.1f,
                            Color.valueOf("387aff"), Color.valueOf("3896ff"),
                            Color.valueOf("4cc6ff"), Color.valueOf("4cc6ff"),
                            Color.valueOf("71e3ff"), Color.valueOf("8eeef4"))
            );
        }};

        cyclant = new Planet("cyclant", sol2, 1f, 3){{
            solarSystem = sol2;
            iconColor = Color.valueOf("90dbff");
            orbitRadius = 90f;
            orbitTime = 12000f;
            rotateTime = 1200f;
            hasAtmosphere = true;
            updateLighting = true;
            lightSrcFrom = 0f;
            lightSrcTo = 0.8f;
            lightDstFrom = 0.25f;
            lightDstTo = 0.85f;
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.4f;
            atmosphereColor = Color.valueOf("90dbff80");
            lightColor = Color.valueOf("3fc2ff80");
            landCloudColor = Color.valueOf("938c9c7f");
            drawOrbit = true;
            tidalLock = false;
            bloom = true;
            visible = true;
            minZoom = 0.001f;
            maxZoom = 10f;
            children = Seq.with(phoon);

            accessible = true;
            allowSelfSectorLaunch = false;
            defaultAttributes.set(Attribute.spores, -1f);
            ruleSetter = rules -> {
                rules.coreDestroyClear = true;
                rules.worldProcessorPlayerLink = true;
                rules.unitPayloadsExplode = true;
                rules.hideSpawns = false;
                rules.logicUnitDeconstruct = true;
                rules.randomWaveAI = false;
                rules.planet = cyclant;
            };
            defaultCore = VEJSBlocks.isoCore1;
            sectorSeed = 0;
            alwaysUnlocked = true;
            startSector = 170;
            clearSectorOnLose = true;
            allowSectorInvasion = false;
            allowLaunchToNumbered = false;
            allowCampaignRules = true;
            showRtsAIRule = false;
            enemyCoreSpawnReplace = true;
            allowLaunchSchematics = true;
            launchMusic = Musics.game4;
            allowLaunchLoadout = true;
            launchCapacityMultiplier = 0.5f;
            prebuildBase = true;
            defaultEnv = Env.terrestrial | Env.groundWater | Env.oxygen | Env.groundOil;

            cloudMesh = new MultiMesh(
                    new HexSkyMesh(this,
                            9, 2.8f, 0.07f, 5, Color.valueOf("938c9cd0"),
                            2, 0.42f, 0.9f, 0.43f),
                    new HexSkyMesh(this,
                            19, 1f, 0.15f, 5, Color.valueOf("b8c1e050"),
                            2, 0.4f, 0.5f, 0.45f),
                    new HexSkyMesh(this,
                            29, 0.6f, 0.2f, 5, Color.valueOf("c7e5ff30"),
                            2, 0.38f, 0.3f, 0.47f),
                    new HexSkyMesh(this,
                            39, 0.8f, 0.25f, 5, Color.valueOf("b8c1e020"),
                            2, 0.38f, 0.3f, 0.6f),
                    new HexSkyMesh(this,
                            49, 1.2f, 0.3f, 5, Color.valueOf("c7e5ff10"),
                            2, 0.38f, 0.3f, 0.75f)
            );
            mesh = new MultiMesh(
                    new NoiseMesh(this,
                            204, 6, 1f, 10, 0.5f, 1f, 0.5f,
                            Color.valueOf("c59a78"), Color.valueOf("909862"),
                            10, 0.5f, 0.2f, 0.5f),
                    new NoiseMesh(this,
                            3, 6, 1f, 6, 0.5f, 0.5f, 0.5f,
                            Color.valueOf("4d5ca420"), Color.valueOf("5867ac20"),
                            3, 0.5f, 0.6f, 0.5f),
                    new NoiseMesh(this,
                            2, 6, 0.982f, 8, 0.5f, 2f, 0.5f,
                            Color.valueOf("3c4448"), Color.valueOf("909862"),
                            1, 0.5f, 1f, 0.5f),
                    new SunMesh(this,
                            2, 6, 0.5, 0.01, 1.2, 1.5, 1.1f,
                            Color.valueOf("ffa665"), Color.valueOf("feb380"))
            );
        }};

        phoon = new Planet("phoon", cyclant, 0.2f, 1){{
            solarSystem = sol2;
            iconColor = Color.valueOf("989aa4");
            orbitRadius = 4.5f;
            orbitTime = 1000f;
            rotateTime = 1000f;
            hasAtmosphere = true;
            updateLighting = false;
            atmosphereRadIn = 0.01f;
            atmosphereRadOut = 0.02f;
            atmosphereColor = Color.valueOf("989aa422");
            lightColor = Color.valueOf("b0bac0dd");
            landCloudColor = Color.valueOf("989aa4f");
            drawOrbit = true;
            tidalLock = true;
            bloom = true;
            visible = true;
            camRadius = 0.25f;
            minZoom = 0.2f;
            maxZoom = 3f;

            accessible = true;
            allowSelfSectorLaunch = false;
            defaultAttributes.set(Attribute.spores, -1f);
            ruleSetter = rules -> {
                rules.coreDestroyClear = true;
                rules.worldProcessorPlayerLink = true;
                rules.unitPayloadsExplode = true;
                rules.hideSpawns = false;
                rules.logicUnitDeconstruct = true;
                rules.randomWaveAI = false;
                rules.planet = phoon;
            };
            defaultCore = VEJSBlocks.isoCore1;
            sectorSeed = 0;
            alwaysUnlocked = true;
            startSector = 0;
            clearSectorOnLose = true;
            allowSectorInvasion = false;
            allowLaunchToNumbered = false;
            allowCampaignRules = true;
            showRtsAIRule = true;
            enemyCoreSpawnReplace = true;
            allowLaunchSchematics = true;
            launchMusic = Musics.game4;
            allowLaunchLoadout = true;
            launchCapacityMultiplier = 0.5f;
            prebuildBase = false;
            defaultEnv = Env.terrestrial | Env.groundWater | Env.oxygen | Env.groundOil;

            mesh = new MultiMesh(
                    new NoiseMesh(this,
                            204, 2, 0.2f, 5, 0.5f, 1f, 0.5f,
                            Color.valueOf("b0bac0"), Color.valueOf("989aa4"),
                            4, 0.5f, 0.1f, 0.5f),
                    new NoiseMesh(this,
                            3, 2, 0.2f, 3, 0.5f, 0.5f, 0.5f,
                            Color.valueOf("989aa4"), Color.valueOf("6e7080"),
                            4, 0.5f, 0.1f, 0.5f)
            );
        }};

        maress = new Planet("maress", sol2, 0.75f, 3){{
            solarSystem = sol2;
            iconColor = Color.valueOf("de8a5b");
            orbitRadius = 120f;
            orbitTime = 18475f;
            rotateTime = 1100f;
            hasAtmosphere = true;
            updateLighting = true;
            lightSrcFrom = 0.2f;
            lightSrcTo = 1.4f;
            lightDstFrom = 0f;
            lightDstTo = 1.2f;
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.15f;
            atmosphereColor = Color.valueOf("e69663");
            lightColor = Color.valueOf("e69663dd");
            landCloudColor = Color.valueOf("b6a49911");
            drawOrbit = true;
            tidalLock = false;
            bloom = true;
            visible = true;
            minZoom = 0.001f;
            maxZoom = 10f;

            accessible = true;
            allowSelfSectorLaunch = false;
            defaultAttributes.set(Attribute.spores, -1f);
            ruleSetter = rules -> {
                rules.coreDestroyClear = true;
                rules.worldProcessorPlayerLink = true;
                rules.unitPayloadsExplode = true;
                rules.hideSpawns = false;
                rules.logicUnitDeconstruct = true;
                rules.randomWaveAI = true;
                rules.planet = maress;
            };
            defaultCore = VEJSBlocks.isoCore1;
            sectorSeed = 0;
            alwaysUnlocked = true;
            startSector = 43;
            clearSectorOnLose = true;
            allowSectorInvasion = false;
            allowLaunchToNumbered = false;
            allowCampaignRules = true;
            showRtsAIRule = false;
            enemyCoreSpawnReplace = true;
            allowLaunchSchematics = true;
            launchMusic = Musics.game8;
            allowLaunchLoadout = true;
            launchCapacityMultiplier = 0.5f;
            prebuildBase = true;
            defaultEnv = Env.terrestrial | Env.groundWater | Env.oxygen;

            cloudMesh = new MultiMesh(
                    new HexSkyMesh(this,
                            78, 1f, 0.12f, 5, Color.valueOf("b6a49933"),
                            2, 0.4f, 0.5f, 0.45f),
                    new HexSkyMesh(this,
                            77, 3f, 0.07f, 5, Color.valueOf("de8a5b22"),
                            2, 0.4f, 0.9f, 0.45f)
            );
            mesh = new MultiMesh(
                    new NoiseMesh(this,
                            177, 5, 0.733f, 10, 0.5f, 2f, 0.3f,
                            Color.valueOf("ba754e"), Color.valueOf("88624c40"),
                            1, 0.5f, 1f, 0.5f),
                    new NoiseMesh(this,
                            995, 5, 0.708f, 6, 0.5f, 0.1f, 0.5f,
                            Color.valueOf("c2c2c2"), Color.valueOf("b6a499"),
                            1, 0.5f, 1f, 0.5f),
                    new NoiseMesh(this,
                            994, 5, 0.5f, 6, 0.5f, 0.1f, 0.05f,
                            Color.valueOf("2d2f39"), Color.valueOf("00ffce"),
                            10, 0.5f, 0.05f, 0.5f)
            );
        }};

        sitrullus = new Planet("sitrullus", sol2, 0.6f, 2){{
            solarSystem = sol2;
            iconColor = Color.valueOf("88c961");
            orbitRadius = 105f;
            orbitTime = 15122f;
            rotateTime = 1800f;
            hasAtmosphere = true;
            updateLighting = true;
            lightSrcFrom = 0f;
            lightSrcTo = 1f;
            lightDstFrom = 0f;
            lightDstTo = 1f;
            atmosphereRadIn = 0.01f;
            atmosphereRadOut = 0.1f;
            atmosphereColor = Color.valueOf("88c96180");
            lightColor = Color.valueOf("ff555550");
            landCloudColor = Color.valueOf("daf9c87f");
            drawOrbit = true;
            tidalLock = false;
            bloom = true;
            visible = true;
            minZoom = 1.2f;
            maxZoom = 10f;

            accessible = true;
            allowSelfSectorLaunch = false;
            defaultAttributes.set(Attribute.spores, 0.5f);
            defaultAttributes.set(Attribute.water, 2f);
            ruleSetter = rules -> {
                rules.coreDestroyClear = true;
                rules.worldProcessorPlayerLink = true;
                rules.unitPayloadsExplode = true;
                rules.hideSpawns = false;
                rules.logicUnitDeconstruct = true;
                rules.randomWaveAI = true;
                rules.pauseDisabled = true;
                rules.planet = sitrullus;
            };
            defaultCore = VEJSBlocks.genCore;
            sectorSeed = 0;
            alwaysUnlocked = true;
            startSector = 15;
            clearSectorOnLose = true;
            allowSectorInvasion = false;
            allowLaunchToNumbered = false;
            allowCampaignRules = false;
            showRtsAIRule = false;
            enemyCoreSpawnReplace = false;
            allowLaunchSchematics = true;
            launchMusic = Musics.game8;
            allowLaunchLoadout = true;
            launchCapacityMultiplier = 0.25f;
            prebuildBase = true;
            defaultEnv = Env.terrestrial | Env.groundWater | Env.oxygen | Env.spores;

            cloudMesh = new MultiMesh(
                    new HexSkyMesh(this,
                            574, 0.3f, 0.08f, 4, Color.valueOf("daf9c840"),
                            2, 0.42f, 0.9f, 0.43f),
                    new HexSkyMesh(this,
                            73, 0.2f, 0.1f, 4, Color.valueOf("edffe220"),
                            2, 0.42f, 0.9f, 0.43f)
            );
            mesh = new MultiMesh(
                    new NoiseMesh(this,
                            73, 5, 0.609f, 10, 0.5f, 12f, 0f,
                            Color.valueOf("ff555520"), Color.valueOf("9e78dc80"),
                            1, 0.5f, 1f, 0.5f),
                    new NoiseMesh(this,
                            117, 5, 0.6f, 10, 0.5f, 2f, 0.2f,
                            Color.valueOf("ff555520"), Color.valueOf("cc343420"),
                            5, 0.5f, 0.5f, 0.5f),
                    new NoiseMesh(this,
                            128, 5, 0.585f, 10, 0.5f, 1f, 0.4f,
                            Color.valueOf("88c961"), Color.valueOf("5ba232"),
                            1, 0.5f, 1f, 0.5f),
                    new NoiseMesh(this,
                            128, 5, 0.59f, 10, 0.5f, 1f, 0.33f,
                            Color.valueOf("ddf5b0"), Color.valueOf("edf5b0"),
                            1, 0.5f, 1f, 0.5f)
            );
        }};

        thavina = new Planet("thavina", VEPlanets.neutronStar, 0.85f, 2){{
            solarSystem = VEPlanets.neutronStar;
            iconColor = Color.valueOf("ff3a73");
            orbitRadius = 40f;
            orbitTime = 12000f;
            rotateTime = 600f;
            hasAtmosphere = true;
            updateLighting = true;
            lightSrcFrom = 0.5f;
            lightSrcTo = 0.8f;
            lightDstFrom = 0.5f;
            lightDstTo = 0.8f;
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.1f;
            atmosphereColor = Color.valueOf("c46164");
            lightColor = Color.valueOf("ff3a7344");
            landCloudColor = Color.valueOf("a3838400");
            drawOrbit = true;
            tidalLock = false;
            bloom = true;
            visible = true;
            minZoom = 0.1f;
            maxZoom = 10f;

            accessible = true;
            allowSelfSectorLaunch = false;
            defaultAttributes.set(Attribute.spores, -1f);
            defaultAttributes.set(Attribute.light, -0.2f);
            ruleSetter = rules -> {
                rules.coreDestroyClear = true;
                rules.worldProcessorPlayerLink = true;
                rules.unitPayloadsExplode = true;
                rules.hideSpawns = true;
                rules.logicUnitDeconstruct = true;
                rules.randomWaveAI = true;
                rules.planet = thavina;
            };
            defaultCore = Blocks.coreShard;
            sectorSeed = 0;
            alwaysUnlocked = true;
            startSector = 0;
            clearSectorOnLose = true;
            allowSectorInvasion = false;
            allowLaunchToNumbered = false;
            allowCampaignRules = false;
            showRtsAIRule = false;
            enemyCoreSpawnReplace = true;
            allowLaunchSchematics = true;
            launchMusic = Musics.game4;
            allowLaunchLoadout = true;
            launchCapacityMultiplier = 0.5f;
            prebuildBase = true;
            defaultEnv = Env.terrestrial;

            mesh = new MultiMesh(
                    new NoiseMesh(this,
                            995, 5, 0.784f, 6, 0.5f, 0.1f, 0.5f,
                            Color.valueOf("d1efff00"), Color.valueOf("1b161b00"),
                            1, 0.5f, 1f, 0.5f),
                    new NoiseMesh(this,
                            995, 5, 0.77f, 6, 0.5f, 2f, 1f,
                            Color.valueOf("1b161b00"), Color.valueOf("392f2d00"),
                            1, 0.5f, 1f, 0.5f),
                    new NoiseMesh(this,
                            325, 5, 0.7f, 6, 0.5f, 10f, 2f,
                            Color.valueOf("1b161b00"), Color.valueOf("2d2f3900"),
                            1, 0.5f, 1f, 0.5f),
                    new SunMesh(this,
                            2, 6, 0.3, 1.5, 1.2, 1, 1.1f,
                            Color.valueOf("ff3a73"), Color.valueOf("c71f50"))
            );
        }};
    }
}
