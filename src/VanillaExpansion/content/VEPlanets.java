package VanillaExpansion.content;

import VanillaExpansion.expand.graphics.AsteroidBeltMesh;
import VanillaExpansion.expand.graphics.ZAxisSkyMesh;
import VanillaExpansion.expand.maps.ProximaPlanetGenerator;
import arc.graphics.Color;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.graphics.g3d.*;
import mindustry.type.Planet;
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
    public static Planet proxima;

    /** 加载行星定义 */
    public static void load() {
        Planet sol4b = content.planet("ve-sol4b");
        proxima = new Planet("proxima", sol4b, 1f, 3) {{
            // 设置生成器
            generator = new ProximaPlanetGenerator();

            // I GOT ANNOYED
            visible = true;
            accessible = false;
            
            // 设置网格加载器（六边形网格）
            meshLoader = () -> new HexMesh(this, 6);
            
            // 设置云层加载器（大气层效果）- 使用Z轴旋转，添加小行星带
            cloudMeshLoader = () -> new MultiMesh(
                // 小行星带 - 由大量不规则小行星组成
                new AsteroidBeltMesh(this, 2.5f, 4.5f, 150, 729, Color.valueOf("6a6a6a")),
                // 内层云层 - 白色
                new ZAxisSkyMesh(this, 2, 0.3f, 0.14f, 5, Color.valueOf("87CEEB").a(0.75f), 2, 0.42f, 1f, 0.43f),
                // 外层云层 - 白色
                new ZAxisSkyMesh(this, 3, 0.8f, 0.15f, 5, Color.valueOf("87CEEB").a(0.65f), 2, 0.42f, 1.2f, 0.45f)
            );
            
            // 直接设置轨道半径（星球到太阳的距离）
            orbitRadius = 45f;
            
            // 设置轨道偏移角度，使行星正对太阳（角度0表示向右，正对太阳）
            orbitOffset = 0f;
            
            // 设置潮汐锁定为true，行星始终面向太阳
            tidalLock = true;
            
            // 设置基本属性
            iconColor = Color.valueOf("87CEEB"); // 淡蓝色，符合冰原主题
            atmosphereColor = Color.valueOf("4A90A4"); // 天蓝色大气
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            hasAtmosphere = true;
            
            // 设置起始扇区
            startSector = 170;
            
            // 设置解锁状态
            alwaysUnlocked = true;
            
            // 设置环境属性
            defaultEnv = mindustry.world.meta.Env.terrestrial;
            
            // 设置默认核心
            
            // 设置规则
            ruleSetter = r -> {
                r.waveTeam = mindustry.game.Team.crux;
                r.placeRangeCheck = false;
                r.coreDestroyClear = true;
            };
            
            // 允许各种功能
            allowWaves = true;
            allowLegacyLaunchPads = true;
            allowSectorInvasion = true;
            allowLaunchSchematics = true;
            enemyCoreSpawnReplace = true;
            allowLaunchLoadout = true;
            
            // 设置颜色主题
            landCloudColor = Color.valueOf("87CEEB").a(0.5f);
        }};
        sol4b.children.add(proxima);
        proxima.solarSystem = sol4b;
    }
}
