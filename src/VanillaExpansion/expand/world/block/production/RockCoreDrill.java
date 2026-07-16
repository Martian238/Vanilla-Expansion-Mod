package VanillaExpansion.expand.world.block.production;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Tile;
import mindustry.world.blocks.production.Drill;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;

import static mindustry.Vars.*;

public class RockCoreDrill extends Drill {

    // ========== 多钻孔配置 ==========
    /** 钻孔数量 */
    public int drillCount = 1;

    /** 每个钻头相对于方块中心的X偏移坐标（像素单位） */
    public float[] drillOffsetX = {0f};
    /** 每个钻头相对于方块中心的Y偏移坐标（像素单位） */
    public float[] drillOffsetY = {0f};

    /** 每个钻孔的独立转速乘数 */
    public float[] drillSpeedMultipliers = {1.0f};

    /** 是否绘制多个钻孔的动画 */
    public boolean drawMultipleDrills = true;
    // ==================================

    // 贴图字段
    public TextureRegion rotatorRegion;
    public TextureRegion topRegion;
    public TextureRegion itemRegionDisplay;

    public RockCoreDrill(String name){
        super(name);

        // 基础配置
        update = true;
        solid = true;
        hasLiquids = true;
        hasItems = true;
        drawMineItem = true;
        drawRim = false;
        rotateSpeed = 2f;

        // 钻机属性
        tier = 3;
        drillTime = 280f;
        warmupSpeed = 0.015f;
        liquidBoostIntensity = 1.6f;

        // 效果配置
        drillEffect = Fx.mine;
        updateEffect = Fx.pulverizeSmall;
        drillEffectChance = 0.02f;
        updateEffectChance = 0.02f;

        // 默认：单个钻头在中心
        drillCount = 1;
        drillOffsetX = new float[]{0f};
        drillOffsetY = new float[]{0f};
        drillSpeedMultipliers = new float[]{1.0f};

        // 环境支持
        envEnabled |= Env.space;
    }

    @Override
    public void init(){
        super.init();

        // 验证并修正钻孔配置
        if(drillCount < 1) drillCount = 1;

        // 确保偏移数组长度正确
        if(drillOffsetX.length < drillCount){
            float[] newX = new float[drillCount];
            float[] newY = new float[drillCount];
            System.arraycopy(drillOffsetX, 0, newX, 0, drillOffsetX.length);
            System.arraycopy(drillOffsetY, 0, newY, 0, drillOffsetY.length);
            for(int i = drillOffsetX.length; i < drillCount; i++){
                newX[i] = 0f;
                newY[i] = 0f;
            }
            drillOffsetX = newX;
            drillOffsetY = newY;
        }

        // 确保速度乘数数组长度正确
        if(drillSpeedMultipliers.length < drillCount){
            float[] newMultipliers = new float[drillCount];
            System.arraycopy(drillSpeedMultipliers, 0, newMultipliers, 0, drillSpeedMultipliers.length);
            for(int i = drillSpeedMultipliers.length; i < drillCount; i++){
                newMultipliers[i] = 1.0f;
            }
            drillSpeedMultipliers = newMultipliers;
        }

        Log.info("[RockCoreDrill] Initialized: drillCount=" + drillCount);
        for(int i = 0; i < drillCount; i++){
            Log.info("[RockCoreDrill] Drill[" + i + "] offset=(" + drillOffsetX[i] + ", " + drillOffsetY[i] +
                    "), speedMulti=" + drillSpeedMultipliers[i]);
        }
    }

    @Override
    public void load(){
        super.load();

        // 手动加载贴图
        String baseName = name;

        rotatorRegion = Core.atlas.find(baseName + "-rotator");
        if(!rotatorRegion.found()){
            Log.warn("[RockCoreDrill] Rotator texture not found: " + baseName + "-rotator, using fallback");
            rotatorRegion = Core.atlas.find("drill-" + size + "-rotator", "clear");
        }

        topRegion = Core.atlas.find(baseName + "-top");
        if(!topRegion.found()){
            Log.warn("[RockCoreDrill] Top texture not found: " + baseName + "-top, using fallback");
            topRegion = Core.atlas.find("drill-" + size + "-top", "clear");
        }

        itemRegionDisplay = Core.atlas.find(baseName + "-item");
        if(!itemRegionDisplay.found()){
            Log.warn("[RockCoreDrill] Item texture not found: " + baseName + "-item, using fallback");
            itemRegionDisplay = Core.atlas.find("drill-" + size + "-item", "clear");
        }

        if(region == null || !region.found()){
            region = Core.atlas.find(baseName, "clear");
        }

        Log.info("[RockCoreDrill] Textures loaded");
    }

    @Override
    public TextureRegion[] icons(){
        TextureRegion rot = rotatorRegion != null && rotatorRegion.found() ? rotatorRegion : region;
        TextureRegion top = topRegion != null && topRegion.found() ? topRegion : region;
        return new TextureRegion[]{region, rot, top};
    }

    @Override
    public void setStats(){
        super.setStats();
        if(drillCount > 1){
            stats.add(new Stat("drillcount"), Core.bundle.format("stat.drillcount", drillCount));
        }
    }

    @Override
    public void setBars(){
        super.setBars();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        // 不调用 super.drawPlace()，完全自定义显示

        Tile tile = world.tile(x, y);
        if(tile == null) return;

        countOre(tile);

        if(returnItem != null){
            float speed = 60f / getDrillTime(returnItem) * returnCount;
            String text;

            if(drillCount > 1){
                // 多钻头模式：显示 "X.XX/秒 ×N"
                text = Core.bundle.format("bar.drillspeed", Strings.fixed(speed, 2)) + " ×" + drillCount;
            }else{
                // 单钻头模式：显示 "X.XX/秒"
                text = Core.bundle.format("bar.drillspeed", Strings.fixed(speed, 2));
            }

            float width = drawPlaceText(text, x, y, valid);
            float dx = x * tilesize + offset - width/2f - 4f, dy = y * tilesize + offset + size * tilesize / 2f + 5, s = iconSmall / 4f;

            Draw.mixcol(Color.darkGray, 1f);
            Draw.rect(returnItem.fullIcon, dx, dy - 1, s, s);
            Draw.reset();
            Draw.rect(returnItem.fullIcon, dx, dy, s, s);

            if(drawMineItem && itemRegionDisplay != null && itemRegionDisplay.found()){
                Draw.color(returnItem.color);
                Draw.rect(itemRegionDisplay, tile.worldx() + offset, tile.worldy() + offset);
                Draw.color();
            }
        }else{
            // 没有可挖掘的矿石，显示等级不足提示
            Tile to = tile.getLinkedTilesAs(this, tempTiles).find(t -> t.drop() != null && (t.drop().hardness > tier || (blockedItems != null && blockedItems.contains(t.drop()))));
            Item item = to == null ? null : to.drop();
            if(item != null){
                drawPlaceText(Core.bundle.get("bar.drilltierreq"), x, y, valid);
            }
        }
    }

    /**
     * 获取指定钻头的位置
     */
    protected Vec2 getDrillPosition(float x, float y, int drillIndex){
        if(drillIndex >= 0 && drillIndex < drillCount){
            return new Vec2(x + drillOffsetX[drillIndex], y + drillOffsetY[drillIndex]);
        }
        return new Vec2(x, y);
    }

    /**
     * 添加可选液体强化（booster）
     * @param liquid 液体类型
     * @param amountPerSecond 每秒消耗量
     * @param intensity 强化倍率
     */
    public void addBooster(Liquid liquid, float amountPerSecond, float intensity){
        this.liquidBoostIntensity = intensity;
        ConsumeLiquid booster = new ConsumeLiquid(liquid, amountPerSecond / 60f);
        booster.optional = true;
        booster.booster = true;
        this.consume(booster);
    }

    /**
     * 简单的二维向量类
     */
    public static class Vec2 {
        public float x, y;
        public Vec2(float x, float y){
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 钻机建筑类
     */
    public class RockCoreDrillBuild extends DrillBuild {

        // 每个钻孔的独立进度
        public float[] drillProgress;

        // 每个钻孔的独立计时（用于旋转动画）
        public float[] drillTimeDrilled;

        @Override
        public void created(){
            super.created();

            // 初始化数组
            drillProgress = new float[drillCount];
            drillTimeDrilled = new float[drillCount];

            for(int i = 0; i < drillCount; i++){
                drillProgress[i] = 0f;
                drillTimeDrilled[i] = 0f;
            }
        }

        @Override
        public void draw(){
            // 绘制底座
            Draw.rect(region, x, y);

            // 绘制裂纹层
            Draw.z(Layer.blockCracks);
            drawDefaultCracks();

            // 绘制钻机主体
            Draw.z(Layer.blockAfterCracks);

            if(drawMultipleDrills && drillCount > 1){
                // 绘制多个钻孔
                for(int i = 0; i < drillCount; i++){
                    Vec2 pos = getDrillPosition(x, y, i);
                    float speedMulti = (i < drillSpeedMultipliers.length) ? drillSpeedMultipliers[i] : 1f;
                    float rotSpeed = rotateSpeed * speedMulti;
                    float timeForThisDrill = drillTimeDrilled != null && i < drillTimeDrilled.length ?
                            drillTimeDrilled[i] : timeDrilled;

                    // 绘制旋转钻头
                    if(drawSpinSprite){
                        Drawf.spinSprite(rotatorRegion, pos.x, pos.y, timeForThisDrill * rotSpeed);
                    }else{
                        Draw.rect(rotatorRegion, pos.x, pos.y, timeForThisDrill * rotSpeed);
                    }

                    // 绘制顶部覆盖层
                    Draw.rect(topRegion, pos.x, pos.y);
                }
            }else{
                // 单钻孔模式
                if(drawSpinSprite){
                    Drawf.spinSprite(rotatorRegion, x, y, timeDrilled * rotateSpeed);
                }else{
                    Draw.rect(rotatorRegion, x, y, timeDrilled * rotateSpeed);
                }
                Draw.rect(topRegion, x, y);
            }

            // 绘制正在开采的物品图标（在中心显示）
            if(dominantItem != null && drawMineItem && itemRegionDisplay != null && itemRegionDisplay.found()){
                Draw.color(dominantItem.color);
                Draw.rect(itemRegionDisplay, x, y);
                Draw.color();
            }
        }

        @Override
        public void updateTile(){
            // 物品输出
            if(timer(timerDump, dumpTime / timeScale)){
                dump(dominantItem != null && items.has(dominantItem) ? dominantItem : null);
            }

            if(dominantItem == null){
                // 即使没有矿物，也要更新 lastDrillSpeed 为 0，确保进度条每帧都有有效值
                lastDrillSpeed = 0f;
                return;
            }

            timeDrilled += warmup * delta();
            float delay = getDrillTime(dominantItem);

            if(items.total() < itemCapacity && dominantItems > 0 && efficiency > 0){
                float speed = Mathf.lerp(1f, liquidBoostIntensity, optionalEfficiency) * efficiency;

                // 完全使用原版公式计算 lastDrillSpeed
                lastDrillSpeed = (speed * dominantItems * warmup) / delay;

                float totalProgressPerSecond = 0f;
                int itemsProduced = 0;

                for(int i = 0; i < drillCount; i++){
                    drillTimeDrilled[i] += warmup * delta();
                    float drillMultiplier = (i < drillSpeedMultipliers.length) ? drillSpeedMultipliers[i] : 1f;
                    float drillProgressPerSecond = speed * dominantItems * warmup * drillMultiplier;
                    totalProgressPerSecond += drillProgressPerSecond;
                    drillProgress[i] += delta() * drillProgressPerSecond;

                    while(drillProgress[i] >= delay && items.total() < itemCapacity){
                        drillProgress[i] -= delay;
                        itemsProduced++;
                        if(wasVisible && Mathf.chanceDelta(drillEffectChance * warmup)){
                            Vec2 pos = getDrillPosition(x, y, i);
                            drillEffect.at(pos.x + Mathf.range(drillEffectRnd),
                                    pos.y + Mathf.range(drillEffectRnd),
                                    dominantItem.color);
                        }
                    }
                }

                warmup = Mathf.approachDelta(warmup, speed, warmupSpeed);
                progress += delta() * (totalProgressPerSecond / drillCount);

                for(int i = 0; i < itemsProduced && items.total() < itemCapacity; i++){
                    offload(dominantItem);
                }

                if(Mathf.chanceDelta(updateEffectChance * warmup)){
                    int randomDrill = Mathf.random(0, drillCount - 1);
                    Vec2 pos = getDrillPosition(x, y, randomDrill);
                    updateEffect.at(pos.x + Mathf.range(size * 2f),
                            pos.y + Mathf.range(size * 2f));
                }
            }else{
                // 确保每帧都更新 lastDrillSpeed 为 0
                lastDrillSpeed = 0f;
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
                for(int i = 0; i < drillCount; i++){
                    if(drillProgress != null){
                        drillProgress[i] = 0f;
                    }
                }
                progress = 0f;
            }
        }

        @Override
        public float progress(){
            if(dominantItem == null) return 0f;
            // 返回平均进度（用于显示进度条）
            return Mathf.clamp(progress / getDrillTime(dominantItem));
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            if(drillProgress != null){
                for(int i = 0; i < drillCount; i++){
                    write.f(drillProgress[i]);
                }
            }
            if(drillTimeDrilled != null){
                for(int i = 0; i < drillCount; i++){
                    write.f(drillTimeDrilled[i]);
                }
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 3 && drillProgress != null){
                for(int i = 0; i < drillCount; i++){
                    try{
                        drillProgress[i] = read.f();
                    }catch(Exception e){
                        drillProgress[i] = 0f;
                    }
                }
                for(int i = 0; i < drillCount; i++){
                    try{
                        drillTimeDrilled[i] = read.f();
                    }catch(Exception e){
                        drillTimeDrilled[i] = 0f;
                    }
                }
            }
        }
    }
}