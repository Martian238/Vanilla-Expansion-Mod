package VanillaExpansion.expand.maps;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.TileGen;
public class ProximaPlanetGenerator extends RegionControlledPlanetGenerator {
    private enum BiomeType {
        ICE_PLAINS,
        SNOWY,
        TAIGA,
        MUSHROOM,
        DECIDUOUS,
        EVERGREEN,
        ACACIA,
        DESERT,
        BADLANDS
    }
    private float scl = 5f;
    private float heightYOffset = 42.7f;
    private float waterOffset = 0.04f;
    private float heightScl = 1.01f;
    private float rawHeight(Vec3 position) {
        return (Mathf.pow(Simplex.noise3d(seed, 7, 0.5f, 1f / 3f, position.x * scl, position.y * scl + heightYOffset, position.z * scl) * heightScl, 2.3f) + waterOffset) / (1f + waterOffset);
    }
    @Override
    public float getHeight(Vec3 position) {
        float height = rawHeight(position);
        float water = 0.1f; // 水域
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
    private Block getBlock(Vec3 position, boolean applyNoise, int tileX, int tileY) {
        float height = rawHeight(position);
        float temp = getBiomeTemperature(position);
        BiomeType biome = getBiomeByTemperature(temp);
        return getBlockForBiome(biome, height);
    }
    private Block getBlock(Vec3 position) {
        int[] coords = getCurrentCoords();
        return getBlock(position, true, coords[0], coords[1]);
    }
    private Block getBlock(Vec3 position, boolean applyNoise) {
        int[] coords = getCurrentCoords();
        return getBlock(position, applyNoise, coords[0], coords[1]);
    }
    private float getBiomeTemperature(Vec3 position) {
        float z = position.z;
        float angle = Mathf.sin(Mathf.clamp(z, -1f, 1f));
        float normalizedAngle = (angle + Mathf.PI / 2f) / Mathf.PI;
        float baseTemp = 1f - normalizedAngle;
        float noise = noise3d(position.x * scl, position.y * scl, position.z * scl, 4, 0.5f, 1f / 8f, 0.5f, 0, 0);
        float blendedTemp = baseTemp + noise * 0.08f;
        return Mathf.clamp(blendedTemp);
    }
    private BiomeType getBiomeByTemperature(float temp) {
        int numBiomes = BiomeType.values().length;
        float clampedTemp = Mathf.clamp(temp);
        int biomeIndex = (int)(clampedTemp * numBiomes);
        biomeIndex = Math.min(biomeIndex, numBiomes - 1);
        return BiomeType.values()[biomeIndex];
    }
    private float sawtoothWave(float t) {
        return 2f * (t - (float)Math.floor(t + 0.5f));
    }
    private float getPeriodicTemperature(Vec3 position) {
        float pz = position.z * scl;
        float rad = scl * 3f;
        float temp = Mathf.clamp((-pz * 1.5f + rad) / (rad * 2f));
        int numBiomes = BiomeType.values().length;
        float biomeSize = 1f / numBiomes;
        float biomeProgress = (temp % 1f) * numBiomes;
        return biomeProgress / numBiomes;
    }
    private Block getBlockForBiome(BiomeType biome, float height) {
        if (height < 0.1f) {
            switch (biome) {
                case MUSHROOM:
                case DECIDUOUS:
                case EVERGREEN:
                    return Blocks.water;
                default:
                    return getSurfaceBlock(biome);
            }
        }
        if (height < 0.15f) {
            switch (biome) {
                case MUSHROOM:
                case DECIDUOUS:
                case EVERGREEN:
                    return Blocks.sand;
                default:
                    return getSurfaceBlock(biome);
            }
        }
        return getSurfaceBlock(biome);
    }
    private Block getSurfaceBlock(BiomeType biome) {
        switch (biome) {
            case ICE_PLAINS:
                return Blocks.ice;
            case SNOWY:
                return Blocks.snow;
            case TAIGA:
                return Blocks.moss;
            case MUSHROOM:
                return Blocks.sporeMoss;
            case DECIDUOUS:
                return Blocks.grass;
            case EVERGREEN:
                return Blocks.grass;
            case ACACIA:
                return Blocks.sand;
            case DESERT:
                return Blocks.sand;
            case BADLANDS:
                return Blocks.dirt;
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
        //添加地形块
        // tile.block = tile.floor.asFloor().wall;
        
        // 添加随机地形变化（使用父类的区域限制噪声）
        if (noise3d(position.x, position.y, position.z, 2, 2, 0.03f, 1f, x, y) > 0.3) {
            //添加地形块
            // tile.block = Blocks.air;
        }
    }
    @Override
    public void genTile(Vec3 position, TileGen tile, int x, int y) {
        tile.floor = getBlock(position, true, x, y);
    }
    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag) {
        Vec3 v = sector.rect.project(x, y).scl(5f);
        return Simplex.noise3d(seed, octaves, falloff, 1f / (float)scl, v.x, v.y, v.z) * (float)mag;
    }
    @Override
    protected void generate() {
        //饰性元素
        decorate();
        //矿石
        addOres();
    }
    private void decorate() {
        pass((x, y) -> {
            if (rand.chance(0.02)) {
                //装饰
            }
        });
    }
    private void addOres() {
        pass((x, y) -> {
            if (!floor.asFloor().hasSurface()) return;
            int offsetX = x - 4, offsetY = y + 23;
            if (Math.abs(0.5f - noise(offsetX, offsetY, 2, 0.7, 40, 1f, x, y)) > 0.26f &&
                Math.abs(0.5f - noise(offsetX, offsetY - 999, 1, 1, 30, 1f, x, y)) > 0.37f) {
                ore = Blocks.oreCopper;
                return;
            }
            if (Math.abs(0.5f - noise(offsetX, offsetY + 999, 2, 0.7, 42, 1f, x, y)) > 0.26f &&
                Math.abs(0.5f - noise(offsetX, offsetY - 1998, 1, 1, 34, 1f, x, y)) > 0.37f) {
                ore = Blocks.oreLead;
                return;
            }
        });
    }
}
