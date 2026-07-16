package VanillaExpansion.expand.maps;

import arc.math.geom.Vec3;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.type.Sector;
import mindustry.world.Tile;
import mindustry.world.TileGen;
import mindustry.world.Tiles;
import mindustry.world.WorldParams;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支持区域控制的行星生成器父类
 * 提供以下功能：
 * 1. 区域绘制模式 - 只在指定区域内生成地形
 * 2. 随机扰动限制 - 只在指定区域内应用随机噪声扰动
 */
public abstract class RegionControlledPlanetGenerator extends PlanetGenerator {

    /** 区域绘制模式 */
    private boolean regionMode = false;
    private int regionStartX = 0, regionStartY = 0;
    private int regionEndX = 0, regionEndY = 0;

    /** 随机扰动限制模式 */
    private boolean noiseRestricted = false;
    private int noiseStartX = 0, noiseStartY = 0;
    private int noiseEndX = 0, noiseEndY = 0;

    /** ThreadLocal 存储当前生成坐标（用于 getColor 方法） */
    private final ThreadLocal<int[]> currentCoords = ThreadLocal.withInitial(() -> new int[2]);

    /**
     * 设置区域绘制模式
     * @param enabled 是否启用区域绘制
     * @param startX 区域起始X
     * @param startY 区域起始Y
     * @param endX 区域结束X
     * @param endY 区域结束Y
     */
    public void setRegionMode(boolean enabled, int startX, int startY, int endX, int endY) {
        this.regionMode = enabled;
        this.regionStartX = startX;
        this.regionStartY = startY;
        this.regionEndX = endX;
        this.regionEndY = endY;
    }

    /**
     * 设置随机扰动限制区域
     * @param enabled 是否启用限制
     * @param startX 限制区域起始X
     * @param startY 限制区域起始Y
     * @param endX 限制区域结束X
     * @param endY 限制区域结束Y
     */
    public void setNoiseRestriction(boolean enabled, int startX, int startY, int endX, int endY) {
        this.noiseRestricted = enabled;
        this.noiseStartX = startX;
        this.noiseStartY = startY;
        this.noiseEndX = endX;
        this.noiseEndY = endY;
    }

    /**
     * 检查指定坐标是否在随机扰动限制区域内
     * @param x X坐标
     * @param y Y坐标
     * @return 是否在限制区域内
     */
    protected boolean isInNoiseRegion(int x, int y) {
        if (!noiseRestricted) return true;
        return x >= noiseStartX && x <= noiseEndX && y >= noiseStartY && y <= noiseEndY;
    }

    /**
     * 设置当前生成坐标（供 getColor 方法使用）
     */
    protected void setCurrentCoords(int x, int y) {
        int[] coords = currentCoords.get();
        coords[0] = x;
        coords[1] = y;
    }

    /**
     * 获取当前生成坐标
     */
    protected int[] getCurrentCoords() {
        return currentCoords.get();
    }

    /**
     * 在指定区域内生成地形（便捷方法）
     */
    public void generateRegion(Tiles tiles, Sector sec, WorldParams params, 
                               int startX, int startY, int endX, int endY) {
        // 保存当前状态
        boolean oldRegionMode = this.regionMode;
        int oldStartX = this.regionStartX, oldStartY = this.regionStartY;
        int oldEndX = this.regionEndX, oldEndY = this.regionEndY;

        // 设置区域模式
        setRegionMode(true, startX, startY, endX, endY);
        
        // 调用主生成方法
        generate(tiles, sec, params);

        // 恢复状态
        setRegionMode(oldRegionMode, oldStartX, oldStartY, oldEndX, oldEndY);
    }

    @Override
    public void generate(Tiles tiles, Sector sec, WorldParams params) {
        this.tiles = tiles;
        this.seed = params.seedOffset + baseSeed;
        this.sector = sec;
        this.width = tiles.width;
        this.height = tiles.height;
        this.rand.setSeed(sec.id + params.seedOffset + baseSeed);

        TileGen gen = new TileGen();
        
        // 确定遍历范围
        int startX = regionMode ? Math.max(0, regionStartX) : 0;
        int startY = regionMode ? Math.max(0, regionStartY) : 0;
        int endX = regionMode ? Math.min(tiles.width - 1, regionEndX) : tiles.width - 1;
        int endY = regionMode ? Math.min(tiles.height - 1, regionEndY) : tiles.height - 1;

        // 在指定范围内生成
        for(int y = startY; y <= endY; y++){
            for(int x = startX; x <= endX; x++){
                gen.reset();
                // 设置当前坐标（供 getColor 方法使用）
                setCurrentCoords(x, y);
                Vec3 position = sector.rect.project(x / (float)tiles.width, y / (float)tiles.height);
                // 使用带坐标的genTile方法，支持随机扰动限制
                genTile(position, gen, x, y);
                tiles.set(x, y, new Tile(x, y, gen.floor, gen.overlay, gen.block));
            }
        }

        // 只在非区域模式下执行后续生成步骤（调用父类方法）
        if (!regionMode) {
            super.generate(tiles, params);
        }
    }

    /**
     * 带坐标的瓦片生成方法（子类必须实现）
     * @param position 三维球面位置
     * @param tile 瓦片生成器
     * @param x 瓦片X坐标
     * @param y 瓦片Y坐标
     */
    public abstract void genTile(Vec3 position, TileGen tile, int x, int y);

    /**
     * 不带坐标的瓦片生成方法（保持兼容性）
     */
    @Override
    protected void genTile(Vec3 position, TileGen tile) {
        // 默认实现，不传递坐标信息（随机扰动限制不生效）
        genTile(position, tile, 0, 0);
    }

    /**
     * 获取是否启用区域绘制模式
     */
    protected boolean isRegionMode() {
        return regionMode;
    }

    /**
     * 获取是否启用随机扰动限制
     */
    protected boolean isNoiseRestricted() {
        return noiseRestricted;
    }

    /**
     * 支持区域限制的噪音方法
     * @param x X坐标
     * @param y Y坐标  
     * @param octaves 八度
     * @param falloff 衰减
     * @param scl 缩放
     * @param mag 幅度
     * @param tileX 瓦片X坐标（用于区域限制检查）
     * @param tileY 瓦片Y坐标（用于区域限制检查）
     * @return 噪音值，若不在限制区域内返回0
     */
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag, int tileX, int tileY) {
        // 检查是否在允许噪音的区域内
        if (!isInNoiseRegion(tileX, tileY)) {
            return 0f; // 不在限制区域内，返回0（无扰动）
        }
        // 调用父类的噪音方法
        return super.noise(x, y, octaves, falloff, scl, mag);
    }

    /**
     * 支持区域限制的3D噪音方法
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param octaves 八度
     * @param falloff 衰减
     * @param scl 缩放
     * @param mag 幅度
     * @param tileX 瓦片X坐标（用于区域限制检查）
     * @param tileY 瓦片Y坐标（用于区域限制检查）
     * @return 噪音值，若不在限制区域内返回0
     */
    protected float noise3d(float x, float y, float z, double octaves, double falloff, double scl, double mag, int tileX, int tileY) {
        // 检查是否在允许噪音的区域内
        if (!isInNoiseRegion(tileX, tileY)) {
            return 0f; // 不在限制区域内，返回0（无扰动）
        }
        // 直接使用 Simplex 噪音
        return arc.util.noise.Simplex.noise3d(seed, octaves, falloff, 1f / (float)scl, x, y, z) * (float)mag;
    }
}
