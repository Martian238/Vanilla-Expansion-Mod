package VanillaExpansion.expand.maps;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.TileGen;

/**
 * 比邻星星球生成器 - 实现环形生物群系
 * 生物群系按温度从寒冷到炎热分布：
 * 冰原[淡蓝] -> 雪地[白色] -> 针叶林[灰绿] -> 蘑菇地[深绿] -> 落叶森林[黄绿] -> 常绿森林[绿色] -> 金合欢树林[黄绿] -> 沙漠[黄色] -> 恶地[红棕]
 */
public class ProximaPlanetGenerator extends RegionControlledPlanetGenerator {

    /** 生物群系类型枚举 */
    private enum BiomeType {
        ICE_PLAINS,      // 冰原 - 淡蓝色
        SNOWY,          // 雪地 - 白色
        TAIGA,          // 针叶林 - 灰绿色
        MUSHROOM,       // 蘑菇地 - 深绿色
        DECIDUOUS,      // 落叶森林 - 黄绿色
        EVERGREEN,      // 常绿森林 - 绿色
        ACACIA,         // 金合欢树林 - 黄绿色
        DESERT,         // 沙漠 - 黄色
        BADLANDS        // 恶地 - 红棕色
    }

    /** 缩放因子 */
    private float scl = 5f;
    /** 高度偏移 */
    private float heightYOffset = 42.7f;
    /** 水域偏移 */
    private float waterOffset = 0.04f;
    /** 高度缩放 */
    private float heightScl = 1.01f;

    /**
     * 计算原始高度值
     */
    private float rawHeight(Vec3 position) {
        return (Mathf.pow(Simplex.noise3d(seed, 7, 0.5f, 1f / 3f, position.x * scl, position.y * scl + heightYOffset, position.z * scl) * heightScl, 2.3f) + waterOffset) / (1f + waterOffset);
    }

    @Override
    public float getHeight(Vec3 position) {
        float height = rawHeight(position);
        float water = 0.1f; // 水域阈值
        return Math.max(height, water);
    }

    @Override
    public void getColor(Vec3 position, Color out) {
        Block block = getBlock(position, false);
        
        if (block == Blocks.water) {
            out.set(block.mapColor).a(1f - block.albedo);
            return;
        }
        
        if (block == Blocks.sand) {
            float temp = getBiomeTemperature(position);
            BiomeType biome = getBiomeByTemperature(temp);
            
            if (biome == BiomeType.MUSHROOM || biome == BiomeType.DECIDUOUS || biome == BiomeType.EVERGREEN) {
                out.set(block.mapColor).a(1f - block.albedo);
                return;
            }
        }
        
        float temp = getBiomeTemperature(position);
        
        out.set(getColorByTemperature(temp));
        out.a(0.8f);
    }

    /**
     * 带坐标的颜色获取方法（支持随机扰动限制）
     */
    public void getColor(Vec3 position, Color out, int x, int y) {
        Block block = getBlock(position, false);
        
        if (block == Blocks.water) {
            out.set(block.mapColor).a(1f - block.albedo);
            return;
        }
        
        if (block == Blocks.sand) {
            float temp = getBiomeTemperature(position);
            BiomeType biome = getBiomeByTemperature(temp);
            
            if (biome == BiomeType.MUSHROOM || biome == BiomeType.DECIDUOUS || biome == BiomeType.EVERGREEN) {
                out.set(block.mapColor).a(1f - block.albedo);
                return;
            }
        }
        
        float temp = getBiomeTemperature(position);
        
        out.set(getColorByTemperature(temp));
        out.a(0.8f);
    }

    /**
     * 根据温度值直接返回对应的颜色
     * 从左到右（从冷到热）：冰原→雪地→针叶林→蘑菇地→落叶森林→常绿森林→金合欢树林→沙漠→恶地
     * 温度被限制在[0,1]范围内
     */
    private Color getBiomeColor(int biomeIndex) {
        switch (biomeIndex) {
            case 0: return Color.valueOf("ADD8E6");
            case 1: return Color.valueOf("FFFFFF");
            case 2: return Color.valueOf("A0BEA0");
            case 3: return Color.valueOf("A5B685");
            case 4: return Color.valueOf("9ACD32");
            case 5: return Color.valueOf("32CD32");
            case 6: return Color.valueOf("FFD700");
            case 7: return Color.valueOf("F4A460");
            case 8: return Color.valueOf("CD5C5C");
            default: return Color.valueOf("9ACD32");
        }
    }

    private Color getColorByTemperature(float temp) {
        int numBiomes = BiomeType.values().length;
        float clampedTemp = Mathf.clamp(temp);
        float biomeF = clampedTemp * numBiomes;
        int biomeIndex1 = (int)Math.floor(biomeF);
        int biomeIndex2 = (int)Math.ceil(biomeF);
        float t = biomeF - biomeIndex1;
        biomeIndex1 = Mathf.clamp(biomeIndex1, 0, numBiomes - 1);
        biomeIndex2 = Mathf.clamp(biomeIndex2, 0, numBiomes - 1);
        Color color1 = getBiomeColor(biomeIndex1);
        Color color2 = getBiomeColor(biomeIndex2);
        return new Color(color1).lerp(color2, t);
    }

    /**
     * 根据三维位置获取对应的方块（生物群系）
     * @param position 三维球面位置
     * @param applyNoise 是否应用随机扰动
     * @param tileX 瓦片X坐标（用于区域限制检查）
     * @param tileY 瓦片Y坐标（用于区域限制检查）
     */
    private Block getBlock(Vec3 position, boolean applyNoise, int tileX, int tileY) {
        float height = rawHeight(position);
        float temp = getBiomeTemperature(position);
        BiomeType biome = getBiomeByTemperature(temp);
        return getBlockForBiome(biome, height);
    }

    /**
     * 根据三维位置获取对应的方块（生物群系）- 默认应用噪声
     */
    private Block getBlock(Vec3 position) {
        int[] coords = getCurrentCoords();
        return getBlock(position, true, coords[0], coords[1]);
    }

    /**
     * 根据三维位置获取对应的方块（生物群系）- 指定是否应用噪声
     */
    private Block getBlock(Vec3 position, boolean applyNoise) {
        int[] coords = getCurrentCoords();
        return getBlock(position, applyNoise, coords[0], coords[1]);
    }

    /**
     * 根据三维位置计算生物群系温度 - 使用角度确保群系均匀分布
     * 在球体上，z坐标对应cos(纬度)，直接使用z会导致群系不均匀
     * 使用asin(z)将角度映射到[0,1]范围，确保每个群系占据相等的纬度跨度
     */
    private float getBiomeTemperature(Vec3 position) {
        float z = position.z;
        float angle = Mathf.sin(Mathf.clamp(z, -1f, 1f));
        float normalizedAngle = (angle + Mathf.PI / 2f) / Mathf.PI;
        float baseTemp = 1f - normalizedAngle;
        float noise = noise3d(position.x * scl, position.y * scl, position.z * scl, 4, 0.5f, 1f / 8f, 0.5f, 0, 0);
        float blendedTemp = baseTemp + noise * 0.08f;
        return Mathf.clamp(blendedTemp);
    }

    /**
     * 根据温度值获取生物群系类型 - 使用周期性函数确保每个生物群系大小相等
     * 温度被限制在[0,1]范围内，确保所有群系都能完整显示
     */
    private BiomeType getBiomeByTemperature(float temp) {
        int numBiomes = BiomeType.values().length;
        float clampedTemp = Mathf.clamp(temp);
        int biomeIndex = (int)(clampedTemp * numBiomes);
        biomeIndex = Math.min(biomeIndex, numBiomes - 1);
        return BiomeType.values()[biomeIndex];
    }

    /**
     * 周期性锯齿波函数 - 将[0,1]范围的值映射到锯齿波形
     * 用于确保生物群系大小完全相等
     */
    private float sawtoothWave(float t) {
        return 2f * (t - (float)Math.floor(t + 0.5f));
    }

    /**
     * 获取基于周期性函数的温度值 - 每个生物群系占据相等的温度跨度
     * @param position 三维位置
     * @return 周期性温度值 [0, 1]
     */
    private float getPeriodicTemperature(Vec3 position) {
        float pz = position.z * scl;
        float rad = scl * 3f;
        float temp = Mathf.clamp((-pz * 1.5f + rad) / (rad * 2f));
        int numBiomes = BiomeType.values().length;
        float biomeSize = 1f / numBiomes;
        float biomeProgress = (temp % 1f) * numBiomes;
        return biomeProgress / numBiomes;
    }

    /**
     * 根据生物群系和高度获取对应的方块
     */
    private Block getBlockForBiome(BiomeType biome, float height) {
        // 低高度区域（水域）- 只在特定生物群系生成水
        if (height < 0.1f) {
            // 水只在蘑菇地、落叶森林、常绿森林生物群系生成
            switch (biome) {
                case MUSHROOM:
                case DECIDUOUS:
                case EVERGREEN:
                    return Blocks.water;
                default:
                    // 其他生物群系低高度区域不生成水，使用对应地表方块
                    return getSurfaceBlock(biome);
            }
        }

        // 中低高度区域（浅水区/沙滩）- 只在特定生物群系生成沙滩
        if (height < 0.15f) {
            // 沙滩只在蘑菇地、落叶森林、常绿森林生物群系生成
            switch (biome) {
                case MUSHROOM:
                case DECIDUOUS:
                case EVERGREEN:
                    return Blocks.sand;
                default:
                    // 其他生物群系中低高度区域使用对应地表方块
                    return getSurfaceBlock(biome);
            }
        }

        // 根据生物群系返回对应的地表方块
        return getSurfaceBlock(biome);
    }

    /**
     * 获取生物群系对应的地表方块
     */
    private Block getSurfaceBlock(BiomeType biome) {
        switch (biome) {
            case ICE_PLAINS:
                return Blocks.ice;           // 冰原 - 淡蓝色
            case SNOWY:
                return Blocks.snow;          // 雪地 - 白色
            case TAIGA:
                return Blocks.moss;          // 针叶林 - 灰绿色苔藓
            case MUSHROOM:
                return Blocks.sporeMoss;     // 蘑菇地 - 深绿色孢子苔藓
            case DECIDUOUS:
                return Blocks.grass;         // 落叶森林 - 黄绿色草地
            case EVERGREEN:
                return Blocks.grass;         // 常绿森林 - 绿色草地
            case ACACIA:
                return Blocks.sand;          // 金合欢树林 - 沙质地面
            case DESERT:
                return Blocks.sand;          // 沙漠 - 黄色沙地
            case BADLANDS:
                return Blocks.dirt;          // 恶地 - 红棕色泥土
            default:
                return Blocks.grass;
        }
    }

    @Override
    public void genTile(Vec3 position, TileGen tile) {
        int[] coords = getCurrentCoords();
        int x = coords[0];
        int y = coords[1];
        tile.floor = getBlock(position, true, x, y);
        // TODO: 添加地形块
        // tile.block = tile.floor.asFloor().wall;
        
        // 添加随机地形变化（使用父类的区域限制噪声）
        if (noise3d(position.x, position.y, position.z, 2, 2, 0.03f, 1f, x, y) > 0.3) {
            // TODO: 添加地形块
            // tile.block = Blocks.air;
        }
    }

    /**
     * 带坐标的瓦片生成方法
     */
    @Override
    public void genTile(Vec3 position, TileGen tile, int x, int y) {
        // 启用极小噪声扰动，边界清晰且带有细微变化
        tile.floor = getBlock(position, true, x, y);
        // TODO: 添加地形块
        // tile.block = tile.floor.asFloor().wall;
    }

    

    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag) {
        Vec3 v = sector.rect.project(x, y).scl(5f);
        return Simplex.noise3d(seed, octaves, falloff, 1f / (float)scl, v.x, v.y, v.z) * (float)mag;
    }

    @Override
    protected void generate() {
        // TODO: 添加地形生成逻辑
        // 目前只生成基础地表，不添加地形块
        
        // 添加装饰性元素
        decorate();
        
        // 添加矿石
        addOres();
    }

    /**
     * 添加装饰性元素
     */
    private void decorate() {
        pass((x, y) -> {
            // TODO: 根据生物群系添加装饰
            if (rand.chance(0.02)) {
                // 添加树木或其他装饰
            }
        });
    }

    /**
     * 添加矿石生成
     */
    private void addOres() {
        pass((x, y) -> {
            if (!floor.asFloor().hasSurface()) return;

            int offsetX = x - 4, offsetY = y + 23;
            
            // 铜矿石（使用父类的区域限制噪声）
            if (Math.abs(0.5f - noise(offsetX, offsetY, 2, 0.7, 40, 1f, x, y)) > 0.26f &&
                Math.abs(0.5f - noise(offsetX, offsetY - 999, 1, 1, 30, 1f, x, y)) > 0.37f) {
                ore = Blocks.oreCopper;
                return;
            }
            
            // 铅矿石（使用父类的区域限制噪声）
            if (Math.abs(0.5f - noise(offsetX, offsetY + 999, 2, 0.7, 42, 1f, x, y)) > 0.26f &&
                Math.abs(0.5f - noise(offsetX, offsetY - 1998, 1, 1, 34, 1f, x, y)) > 0.37f) {
                ore = Blocks.oreLead;
                return;
            }
        });
    }
}
