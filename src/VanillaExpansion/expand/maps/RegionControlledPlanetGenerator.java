package VanillaExpansion.expand.maps;
import arc.math.geom.Vec3;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.type.Sector;
import mindustry.world.Tile;
import mindustry.world.TileGen;
import mindustry.world.Tiles;
import mindustry.world.WorldParams;
public abstract class RegionControlledPlanetGenerator extends PlanetGenerator {
    private boolean regionMode = false;
    private int regionStartX = 0, regionStartY = 0;
    private int regionEndX = 0, regionEndY = 0;
    private boolean noiseRestricted = false;
    private int noiseStartX = 0, noiseStartY = 0;
    private int noiseEndX = 0, noiseEndY = 0;
    private final ThreadLocal<int[]> currentCoords = ThreadLocal.withInitial(() -> new int[2]);
    public void setRegionMode(boolean enabled, int startX, int startY, int endX, int endY) {
        this.regionMode = enabled;
        this.regionStartX = startX;
        this.regionStartY = startY;
        this.regionEndX = endX;
        this.regionEndY = endY;
    }
    public void setNoiseRestriction(boolean enabled, int startX, int startY, int endX, int endY) {
        this.noiseRestricted = enabled;
        this.noiseStartX = startX;
        this.noiseStartY = startY;
        this.noiseEndX = endX;
        this.noiseEndY = endY;
    }
    protected boolean isInNoiseRegion(int x, int y) {
        if (!noiseRestricted) return true;
        return x >= noiseStartX && x <= noiseEndX && y >= noiseStartY && y <= noiseEndY;
    }
    protected void setCurrentCoords(int x, int y) {
        int[] coords = currentCoords.get();
        coords[0] = x;
        coords[1] = y;
    }
    protected int[] getCurrentCoords() {
        return currentCoords.get();
    }
    public void generateRegion(Tiles tiles, Sector sec, WorldParams params, 
                               int startX, int startY, int endX, int endY) {
        boolean oldRegionMode = this.regionMode;
        int oldStartX = this.regionStartX, oldStartY = this.regionStartY;
        int oldEndX = this.regionEndX, oldEndY = this.regionEndY;
        setRegionMode(true, startX, startY, endX, endY);
        generate(tiles, sec, params);
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
        int startX = regionMode ? Math.max(0, regionStartX) : 0;
        int startY = regionMode ? Math.max(0, regionStartY) : 0;
        int endX = regionMode ? Math.min(tiles.width - 1, regionEndX) : tiles.width - 1;
        int endY = regionMode ? Math.min(tiles.height - 1, regionEndY) : tiles.height - 1;
        for(int y = startY; y <= endY; y++){
            for(int x = startX; x <= endX; x++){
                gen.reset();
                setCurrentCoords(x, y);
                Vec3 position = sector.rect.project(x / (float)tiles.width, y / (float)tiles.height);
                genTile(position, gen, x, y);
                tiles.set(x, y, new Tile(x, y, gen.floor, gen.overlay, gen.block));
            }
        }
        if (!regionMode) {
            super.generate(tiles, params);
        }
    }
    public abstract void genTile(Vec3 position, TileGen tile, int x, int y);
    @Override
    protected void genTile(Vec3 position, TileGen tile) {
        genTile(position, tile, 0, 0);
    }
    protected boolean isRegionMode() {
        return regionMode;
    }
    protected boolean isNoiseRestricted() {
        return noiseRestricted;
    }
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag, int tileX, int tileY) {
        if (!isInNoiseRegion(tileX, tileY)) {
            return 0f;
        }
        return super.noise(x, y, octaves, falloff, scl, mag);
    }
    protected float noise3d(float x, float y, float z, double octaves, double falloff, double scl, double mag, int tileX, int tileY) {
        if (!isInNoiseRegion(tileX, tileY)) {
            return 0f;
        }
        return arc.util.noise.Simplex.noise3d(seed, octaves, falloff, 1f / (float)scl, x, y, z) * (float)mag;
    }
}
