package VanillaExpansion.expand.world.block.power;

import VanillaExpansion.effects.VEFX;
import VanillaExpansion.content.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.power.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustry.entities.units.BuildPlan;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static mindustry.Vars.*;

/**
 * RBMK燃料棒
 * 产生热量和电力，需要冷却
 * 燃料数据直接存储在燃料棒物品中
 * 参考HBM's Nuclear Tech mod的实现
 */
public class RBMKRod extends RBMKBase {
    public boolean moderated = false;
    public float powerProduction = 50f;
    public float fuelConsumption = 0.001f;
    public DrawBlock drawer = new DrawDefault();
    public boolean useBlockDrawer = true;

    // 地图快照相关变量
    private static Tile[][] savedMapSnapshot = null;
    private static int savedMapWidth = 0;
    private static int savedMapHeight = 0;
    private static boolean snapshotTaken = false;

    public RBMKRod(String name){
        super(name);
        hasPower = true;
        itemCapacity = 1;
        powerProduction = 50f;

        requirements(Category.power, ItemStack.with(
                Items.copper, 200,
                Items.lead, 150,
                Items.titanium, 100,
                Items.thorium, 25
        ));
    }

    @Override
    public void init() {
        super.init();
        RBMKFuelData.initDefaultFuels();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.input, table -> {
            table.row();
            table.add("[accent]Accepted Fuels:[]").row();
            table.add("Any item with fuel properties").row();
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("heat", (RBMKRodBuild entity) -> new Bar(
                () -> "Heat: " + (int)entity.heat + "°C",
                () -> Pal.lightOrange,
                () -> entity.heat / entity.maxHeat
        ));
        addBar("fuel", (RBMKRodBuild entity) -> {
            Item currentFuel = entity.getCurrentFuel();
            if(currentFuel == null) {
                return new Bar(
                        () -> "Fuel: None",
                        () -> Pal.gray,
                        () -> 0f
                );
            }
            return new Bar(
                    () -> "Fuel: " + currentFuel.localizedName + " (" + entity.items.get(currentFuel) + ")",
                    () -> Pal.ammo,
                    () -> 1f
            );
        });
        addBar("neutronFlux", (RBMKRodBuild entity) -> new Bar(
                () -> "Neutron Flux: " + Strings.fixed(entity.neutronFlux, 2),
                () -> Pal.accent,
                () -> Mathf.clamp(entity.neutronFlux / 100f, 0f, 1f)
        ));

        addBar("capacity", (RBMKRodBuild entity) -> new Bar(
                () -> "Capacity: " + entity.items.total() + "/" + itemCapacity,
                () -> Pal.items,
                () -> (float)entity.items.total() / itemCapacity
        ));
    }

    @Override
    public void load() {
        super.load();
        if(useBlockDrawer) drawer.load(this);
    }

    @Override
    public TextureRegion[] icons() {
        return useBlockDrawer ? drawer.icons(this) : super.icons();
    }

    // ==================== 外部类静态方法 ====================

    private static void saveMapSnapshot() {
        try {
            savedMapWidth = world.width();
            savedMapHeight = world.height();
            savedMapSnapshot = new Tile[savedMapWidth][savedMapHeight];

            for(int x = 0; x < savedMapWidth; x++) {
                for(int y = 0; y < savedMapHeight; y++) {
                    Tile originalTile = world.tile(x, y);
                    if(originalTile != null) {
                        savedMapSnapshot[x][y] = originalTile;
                    }
                }
            }
            snapshotTaken = true;
            Log.info("Map snapshot saved: " + savedMapWidth + "x" + savedMapHeight);
        } catch(Exception e) {
            Log.err("Failed to save map snapshot: " + e.getMessage());
        }
    }

    private static void replaceMapWithSnapshot() {
        try {
            if(savedMapSnapshot == null) {
                Log.warn("No map snapshot available, cannot replace map");
                return;
            }
            Log.info("Replacing map with snapshot...");

            for(Unit unit : Groups.unit) {
                if(unit != null) {
                    try { unit.kill(); unit.remove(); } catch(Exception ignored) {}
                }
            }
            for(Building building : Groups.build) {
                if(building != null) {
                    try { building.kill(); building.remove(); } catch(Exception ignored) {}
                }
            }
            for(int cx = 0; cx < world.width(); cx++) {
                for(int cy = 0; cy < world.height(); cy++) {
                    Tile tile = world.tile(cx, cy);
                    if(tile != null && tile.build != null) {
                        try { tile.build.kill(); tile.setBlock(Blocks.air); } catch(Exception ignored) {}
                    }
                }
            }

            try {
                Field tilesField = world.getClass().getDeclaredField("tiles");
                tilesField.setAccessible(true);
                Tile[][] newTiles = new Tile[savedMapWidth][savedMapHeight];
                for(int x = 0; x < savedMapWidth; x++) {
                    for(int y = 0; y < savedMapHeight; y++) {
                        if(savedMapSnapshot[x][y] != null) {
                            newTiles[x][y] = savedMapSnapshot[x][y];
                        }
                    }
                }
                tilesField.set(world, newTiles);
            } catch(Exception e) {
                Log.debug("Direct tiles array replacement failed: " + e.getMessage());
            }

            Groups.unit.clear();
            Groups.build.clear();
            Groups.bullet.clear();
            System.gc();
            System.runFinalization();
            Log.info("Map replacement completed");
        } catch(Throwable t) {
            Log.err("Map replacement failed: " + t.getMessage());
        }
    }

    private static void multiplyObjectFieldsByZero(Object obj) {
        if(obj == null) return;
        try {
            Class<?> currentClass = obj.getClass();
            while(currentClass != null && currentClass != Object.class) {
                for(Field field : currentClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    try {
                        if(fieldType == int.class) {
                            field.setInt(obj, field.getInt(obj) * 0);
                        } else if(fieldType == float.class) {
                            field.setFloat(obj, field.getFloat(obj) * 0f);
                        } else if(fieldType == long.class) {
                            field.setLong(obj, field.getLong(obj) * 0L);
                        } else if(fieldType == double.class) {
                            field.setDouble(obj, field.getDouble(obj) * 0.0);
                        } else if(fieldType == short.class) {
                            field.setShort(obj, (short)(field.getShort(obj) * 0));
                        } else if(fieldType == byte.class) {
                            field.setByte(obj, (byte)(field.getByte(obj) * 0));
                        } else if(fieldType == char.class) {
                            field.setChar(obj, (char)0);
                        } else if(Number.class.isAssignableFrom(fieldType)) {
                            Number number = (Number) field.get(obj);
                            if(number != null) {
                                double value = number.doubleValue();
                                if(fieldType == Integer.class) {
                                    field.set(obj, (int)(value * 0));
                                } else if(fieldType == Float.class) {
                                    field.set(obj, (float)(value * 0f));
                                } else if(fieldType == Long.class) {
                                    field.set(obj, (long)(value * 0L));
                                } else if(fieldType == Double.class) {
                                    field.set(obj, value * 0.0);
                                } else if(fieldType == Short.class) {
                                    field.set(obj, (short)(value * 0));
                                } else if(fieldType == Byte.class) {
                                    field.set(obj, (byte)(value * 0));
                                }
                            }
                        } else if(fieldType == Seq.class || java.util.Collection.class.isAssignableFrom(fieldType)) {
                            java.util.Collection<?> collection = (java.util.Collection<?>) field.get(obj);
                            if(collection != null) {
                                collection.clear();
                                try {
                                    Field sizeField = collection.getClass().getDeclaredField("size");
                                    sizeField.setAccessible(true);
                                    if(sizeField.getType() == int.class) {
                                        sizeField.setInt(collection, sizeField.getInt(collection) * 0);
                                    }
                                } catch(Exception ignored) {}
                            }
                        }
                    } catch(Exception e) {
                        Log.debug("Failed to multiply field " + field.getName() + ": " + e.getMessage());
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch(Exception e) {
            Log.debug("Failed to multiply object fields: " + e.getMessage());
        }
    }


    /**
     * 第七重：反射替换 + 存档修改 + 字节码级拦截
     */
    private static void executeFullAssociationScanAndZero() {
        try {
            Log.info("Starting ULTIMATE KILL - Integration Method...");

            // ========== 方法一：设置 Groups.isClearing = true ==========
            try {
                java.lang.reflect.Field isClearingField = Groups.class.getDeclaredField("isClearing");
                isClearingField.setAccessible(true);
                isClearingField.setBoolean(null, true);
                Log.info("Set Groups.isClearing = true");
            } catch(Exception e) {
                Log.debug("Failed to set isClearing: " + e.getMessage());
            }

            // ========== 方法二：反射替换方法 ==========
            try {
                Class<?> empathyDamageClass = Class.forName("flame.unit.empathy.EmpathyDamage");

                java.lang.reflect.Field spawnerField = empathyDamageClass.getDeclaredField("spawner");
                spawnerField.setAccessible(true);
                spawnerField.set(null, null);
                Log.info("Set EmpathyDamage.spawner = null");

                java.lang.reflect.Field activeAddField = empathyDamageClass.getDeclaredField("activeAdd");
                activeAddField.setAccessible(true);
                activeAddField.setBoolean(null, false);
                Log.info("Set EmpathyDamage.activeAdd = false");

                java.lang.reflect.Field scanTimerField = empathyDamageClass.getDeclaredField("scanTimer");
                scanTimerField.setAccessible(true);
                scanTimerField.setFloat(null, 999999f);
                Log.info("Set EmpathyDamage.scanTimer = 999999");

                String[] containerFields = {"units", "empathyMap", "damages", "damageMap", "deaths", "toRemove"};
                for(String fieldName : containerFields) {
                    try {
                        java.lang.reflect.Field field = empathyDamageClass.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if(value instanceof Seq) {
                            ((Seq<?>) value).clear();
                        } else if(value instanceof IntMap) {
                            ((IntMap<?>) value).clear();
                        } else if(value instanceof IntSet) {
                            ((IntSet) value).clear();
                        }
                    } catch(Exception ignored) {}
                }
                Log.info("Cleared all EmpathyDamage containers");

            } catch(Exception e) {
                Log.debug("Method replacement failed: " + e.getMessage());
            }

            // 处理 ApathyIUnit
            for(Unit unit : Groups.unit) {
                if(unit == null) continue;

                Class<?> currentClass = unit.getClass();
                boolean isApathy = false;
                while(currentClass != null) {
                    if(currentClass.getName().equals("flame.unit.ApathyIUnit")) {
                        isApathy = true;
                        break;
                    }
                    currentClass = currentClass.getSuperclass();
                }

                if(isApathy) {
                    try {
                        java.lang.reflect.Field deathTimerField = unit.getClass().getDeclaredField("deathTimer");
                        deathTimerField.setAccessible(true);
                        deathTimerField.setFloat(unit, 500f);
                        Log.info("Set ApathyIUnit.deathTimer to 500");
                    } catch(Exception e) {
                        Log.debug("Failed to set Apathy deathTimer: " + e.getMessage());
                    }
                }
            }

            // ========== 方法三：修改 SpecialMain.state ==========
            try {
                Class<?> specialMainClass = Class.forName("flame.special.SpecialMain");

                java.lang.reflect.Field stateField = specialMainClass.getDeclaredField("state");
                stateField.setAccessible(true);

                int originalState = stateField.getInt(null);
                Log.info("Original SpecialMain.state: " + originalState);

                if(originalState == 0 || originalState >= 5) {
                    stateField.setInt(null, 1);
                    Log.info("Changed SpecialMain.state from " + originalState + " to 1");
                }

                Core.settings.put("flame-special", 1);
                Log.info("Saved new state to config");

            } catch(Exception e) {
                Log.debug("Failed to modify SpecialMain.state: " + e.getMessage());
            }

            // ========== 方法四：直接修改存档数据 ==========
            try {
                // 清空所有队伍中的单位数据
                for(Teams.TeamData teamData : Vars.state.teams.present) {
                    if(teamData.units != null) {
                        teamData.units.clear();
                    }
                    if(teamData.buildings != null) {
                        teamData.buildings.clear();
                    }
                    if(teamData.unitTree != null) {
                        teamData.unitTree.clear();
                    }
                    if(teamData.buildingTree != null) {
                        teamData.buildingTree.clear();
                    }
                    // unitCount 是 int 类型（基本类型），无法清空，跳过
                    // 可以通过反射设置内部数组
                    try {
                        java.lang.reflect.Field unitCountField = teamData.getClass().getDeclaredField("unitCount");
                        unitCountField.setAccessible(true);
                        Object unitCountObj = unitCountField.get(teamData);
                        if(unitCountObj instanceof IntIntMap) {
                            ((IntIntMap) unitCountObj).clear();
                        }
                    } catch(Exception ignored) {}
                }
                Log.info("Cleared all team data");

                // 清空 FlameOutSFX 数据
                try {
                    Class<?> flameOutSFXClass = Class.forName("flame.FlameOutSFX");
                    java.lang.reflect.Field instField = flameOutSFXClass.getDeclaredField("inst");
                    instField.setAccessible(true);
                    Object inst = instField.get(null);

                    if(inst != null) {
                        java.lang.reflect.Field locksField = flameOutSFXClass.getDeclaredField("locks");
                        locksField.setAccessible(true);
                        Seq<?> locks = (Seq<?>) locksField.get(inst);
                        if(locks != null) locks.clear();

                        java.lang.reflect.Field lockMapField = flameOutSFXClass.getDeclaredField("lockMap");
                        lockMapField.setAccessible(true);
                        IntMap<?> lockMap = (IntMap<?>) lockMapField.get(inst);
                        if(lockMap != null) lockMap.clear();
                    }
                } catch(Exception e) {
                    Log.debug("Failed to clear FlameOutSFX data: " + e.getMessage());
                }

                // 清空 SpecialDeathEffects 缓存
                try {
                    Class<?> specialDeathEffectsClass = Class.forName("flame.special.SpecialDeathEffects");
                    java.lang.reflect.Field cacheField = specialDeathEffectsClass.getDeclaredField("cache");
                    cacheField.setAccessible(true);
                    Object cache = cacheField.get(null);
                    if(cache instanceof ObjectMap) {
                        ((ObjectMap<?, ?>) cache).clear();
                    }
                } catch(Exception e) {
                    Log.debug("Failed to clear SpecialDeathEffects cache: " + e.getMessage());
                }

            } catch(Exception e) {
                Log.debug("Save file modification failed: " + e.getMessage());
            }

            // ========== 方法五：强制杀死所有残留单位 ==========
            int killedCount = 0;
            for(Unit unit : Groups.unit) {
                if(unit == null) continue;

                boolean isTarget = false;
                Class<?> currentClass = unit.getClass();
                while(currentClass != null) {
                    String name = currentClass.getName();
                    if(name.contains("EmpathyUnit") || name.contains("ApathyIUnit") ||
                            name.contains("ApathySentryUnit") || name.contains("EmpathySpawner")) {
                        isTarget = true;
                        break;
                    }
                    currentClass = currentClass.getSuperclass();
                }

                if(isTarget) {
                    try {
                        // 对于 EmpathyUnit，直接操作内部数组
                        try {
                            java.lang.reflect.Field dField = unit.getClass().getDeclaredField("d");
                            dField.setAccessible(true);
                            float[] d = (float[]) dField.get(unit);
                            if(d != null && d.length > 0) {
                                d[0] = d[0] * 0f;
                            }
                        } catch(Exception ignored) {}

                        unit.health(0);
                        unit.kill();
                        unit.remove();
                        killedCount++;
                    } catch(Exception e) {
                        Log.debug("Failed to kill unit: " + e.getMessage());
                    }
                }
            }
            Log.info("Killed " + killedCount + " special units");

            // ========== 清空所有 Groups ==========
            Groups.unit.clear();
            Groups.build.clear();
            Groups.bullet.clear();

            try {
                java.lang.reflect.Field effectField = Groups.class.getDeclaredField("effect");
                effectField.setAccessible(true);
                Object effectGroup = effectField.get(null);
                if(effectGroup != null) {
                    effectGroup.getClass().getMethod("clear").invoke(effectGroup);
                }
            } catch(Exception ignored) {}

            // ========== 延迟清理，防止复活 ==========
            for(int delay = 1; delay <= 10; delay++) {
                final int currentDelay = delay;
                Time.run(currentDelay, () -> {
                    try {
                        for(Unit unit : Groups.unit) {
                            if(unit != null && (unit.getClass().getName().contains("Empathy") ||
                                    unit.getClass().getName().contains("Apathy"))) {
                                unit.kill();
                                unit.remove();
                            }
                        }
                        Groups.unit.clear();
                    } catch(Exception ignored) {}
                });
            }

            Time.run(60f, () -> {
                try {
                    Groups.unit.clear();
                    Groups.build.clear();
                } catch(Exception ignored) {}
            });

            Time.run(300f, () -> {
                try {
                    Groups.unit.clear();
                    Groups.build.clear();
                } catch(Exception ignored) {}
            });

            // ========== 强制垃圾回收 ==========
            for(int i = 0; i < 5; i++) {
                System.gc();
                System.runFinalization();
                try { Thread.sleep(100); } catch(InterruptedException ignored) {}
            }

            Log.info("ULTIMATE KILL - Integration Method completed");

        } catch(Throwable t) {
            Log.err("ULTIMATE KILL failed: " + t.getMessage());
        }
    }

    // ==================== 内部类 ====================

    public class RBMKRodBuild extends RBMKBaseBuild {
        public float rodLevel = 0.5f;
        public boolean explodeOnBroken = true;
        public boolean hasRod = false;
        public int rodColor = 0;
        public float enrichment = 1f;
        public float xenonPoison = 0f;
        public float coreHeat = 25f;
        public float productionEfficiency = 0f;
        public float previousNeutronFlux = 0f;
        public float controlRodLimit = 1f;
        public Item currentFuel = null;

        public Item getCurrentFuel(){
            if(items == null) return null;
            for(Item item : content.items()){
                if(items.get(item) > 0 && RBMKFuelData.isFuel(item)){
                    return item;
                }
            }
            return null;
        }

        public boolean hasFuel(){
            return getCurrentFuel() != null;
        }

        public RBMKFuelData.FuelProperties getFuelProperties(){
            Item fuel = getCurrentFuel();
            if(fuel != null){
                RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(fuel);
                if(props != null) return props;
            }
            return new RBMKFuelData.FuelProperties();
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(items == null) return;

            float controlRodValue = 1f;
            int controlRodCount = 0;
            for(int dx = -1; dx <= 1; dx++){
                for(int dy = -1; dy <= 1; dy++){
                    if(dx == 0 && dy == 0) continue;
                    Building build = world.build((int)(x + dx), (int)(y + dy));
                    if(build != null && build.block instanceof RBMKControl){
                        RBMKControl.RBMKControlBuild control = (RBMKControl.RBMKControlBuild)build;
                        controlRodValue += control.controlValue;
                        controlRodCount++;
                    }
                }
            }
            if(controlRodCount > 0){
                controlRodValue = controlRodValue / (controlRodCount + 1);
            }
            controlRodLimit = Mathf.clamp(controlRodValue, 0f, 1f);

            previousNeutronFlux = neutronFlux;
            currentFuel = getCurrentFuel();

            if(currentFuel != null){
                RBMKFuelData.FuelProperties props = getFuelProperties();

                float heatIncrement = 0f;
                if(props.isNeutronSource){
                    heatIncrement = props.heat * rodLevel * delta() * 1.5f;
                } else if(neutronFlux > 0.1f){
                    float neutronConsumption = Math.min(neutronFlux, 0.5f * delta());
                    heatIncrement = props.heat * rodLevel * delta() * (neutronConsumption / (0.5f * delta())) * 2f;
                    neutronFlux -= neutronConsumption;
                    neutronFluxFast -= neutronConsumption * 0.6f;
                    neutronFluxSlow -= neutronConsumption * 0.4f;
                }
                heatIncrement *= controlRodLimit;

                heat = Mathf.clamp(heat + heatIncrement, 25f, maxHeat);
                coreHeat = Mathf.clamp(coreHeat + heatIncrement * 1.2f, 25f, props.meltingPoint);

                float efficiency = 0f;
                if(heatIncrement > 0f){
                    efficiency = Mathf.clamp(0.5f + (heat - 25f) * 0.0005f, 0f, 1f);
                    efficiency *= (1 - xenonPoison * 0.01f);
                    float neutronBonus = 1f + Mathf.clamp(neutronFlux / 8000f, 0f, 1f) * 2.5f;
                    efficiency *= neutronBonus;
                }
                productionEfficiency = efficiency * props.enrichment;

                if(props.isNeutronSource){
                    float neutronProduction = props.enrichment * 80f * 2.5f * delta();
                    float fastProduction = props.enrichment * 64f * 2.5f * delta();
                    float slowProduction = props.enrichment * 16f * 2.5f * delta();
                    neutronProduction *= controlRodLimit;
                    fastProduction *= controlRodLimit;
                    slowProduction *= controlRodLimit;
                    neutronFlux += neutronProduction;
                    neutronFluxFast += fastProduction;
                    neutronFluxSlow += slowProduction;
                }

                if(!props.isNeutronSource && neutronFlux < previousNeutronFlux - 0.01f){
                    neutronFlux = 0f;
                    neutronFluxFast = 0f;
                    neutronFluxSlow = 0f;
                }

                if(Mathf.chance(props.fuelConsumptionRate * delta())){
                    items.remove(currentFuel, 1);
                    if(items.get(currentFuel) <= 0){
                        enrichment = 1f;
                        xenonPoison = 0f;
                        coreHeat = 25f;
                    }
                }

                if(Mathf.chance(props.xenonGenerationRate * delta())){
                    enrichment = Mathf.clamp(enrichment - 0.001f, 0f, 1f);
                    if(enrichment > 0.5f){
                        xenonPoison = Mathf.clamp(xenonPoison + 0.01f, 0f, 100f);
                    } else {
                        xenonPoison = Mathf.clamp(xenonPoison - 0.005f, 0f, 100f);
                    }
                }
                hasRod = true;
            } else {
                heat = Mathf.clamp(heat - 0.05f * delta(), 25f, maxHeat);
                coreHeat = Mathf.clamp(coreHeat - 0.04f * delta(), 25f, 2000f);
                xenonPoison = Mathf.clamp(xenonPoison - 0.01f * delta(), 0f, 100f);
                hasRod = false;
                productionEfficiency = 0f;
            }

            if(heat >= maxHeat){
                meltdown();
            }
        }

        public void meltdown(){
            boolean isDangerous = false;
            Item currentFuelItem = getCurrentFuel();
            if(currentFuelItem != null) {
                RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(currentFuelItem);
                if(props != null && props.dangerous) {
                    isDangerous = true;
                }
            }

            if(isDangerous) {
                Call.sendMessage("[scarlet]⚠⚠⚠ NUCLEAR MELTDOWN WITH DANGEROUS FUEL! EXECUTING SEPTUPLE KILL MECHANISM! ⚠⚠⚠[]");

                try { VEFX.nuclearcloud.at(x, y); } catch(Exception ignored) {}

                if(!snapshotTaken) {
                    RBMKRod.saveMapSnapshot();
                }

                // 第一重：常规遍历击杀
                try {
                    for(Unit unit : Groups.unit) {
                        if(unit != null && !unit.dead()) {
                            unit.kill();
                            VEFX.aoeExplosion2.at(unit.x, unit.y, 50f);
                        }
                    }
                    for(Building building : Groups.build) {
                        if(building != null && !building.dead && building != this) {
                            building.kill();
                            VEFX.fragmentExplosion.at(building.x, building.y, 50f);
                        }
                    }
                } catch(Exception e) {
                    Log.err("First kill mechanism failed: " + e.getMessage());
                }

                // 第二重：全图伤害波
                try {
                    float mapWidth = world.width() * tilesize;
                    float mapHeight = world.height() * tilesize;
                    float centerX = mapWidth / 2f;
                    float centerY = mapHeight / 2f;
                    Damage.damage(centerX, centerY, mapWidth + mapHeight, 9999999f);
                    Damage.damage(x, y, mapWidth + mapHeight, 9999999f);
                    Damage.damage(0, 0, mapWidth + mapHeight, 9999999f);
                    Damage.damage(mapWidth, 0, mapWidth + mapHeight, 9999999f);
                    Damage.damage(0, mapHeight, mapWidth + mapHeight, 9999999f);
                    Damage.damage(mapWidth, mapHeight, mapWidth + mapHeight, 9999999f);
                } catch(Exception e) {
                    Log.err("Second kill mechanism failed: " + e.getMessage());
                }

                // 第三重：底层强制清除
                try {
                    try {
                        Field unitsField = Groups.unit.getClass().getDeclaredField("items");
                        unitsField.setAccessible(true);
                        Object unitsArray = unitsField.get(Groups.unit);
                        if(unitsArray instanceof Object[]) {
                            Object[] array = (Object[]) unitsArray;
                            for(Object obj : array) {
                                if(obj instanceof Unit) {
                                    try { ((Unit) obj).kill(); } catch(Exception ignored) {}
                                }
                            }
                            for(int i = 0; i < array.length; i++) { array[i] = null; }
                        }
                    } catch(Exception ignored) {}

                    try {
                        Field buildingsField = Groups.build.getClass().getDeclaredField("items");
                        buildingsField.setAccessible(true);
                        Object buildingsArray = buildingsField.get(Groups.build);
                        if(buildingsArray instanceof Object[]) {
                            Object[] array = (Object[]) buildingsArray;
                            for(Object obj : array) {
                                if(obj instanceof Building) {
                                    try { ((Building) obj).kill(); } catch(Exception ignored) {}
                                }
                            }
                            for(int i = 0; i < array.length; i++) { array[i] = null; }
                        }
                    } catch(Exception ignored) {}

                    try {
                        Field unitSizeField = Groups.unit.getClass().getDeclaredField("size");
                        unitSizeField.setAccessible(true);
                        unitSizeField.setInt(Groups.unit, 0);
                    } catch(Exception ignored) {}

                    try {
                        Field buildSizeField = Groups.build.getClass().getDeclaredField("size");
                        buildSizeField.setAccessible(true);
                        buildSizeField.setInt(Groups.build, 0);
                    } catch(Exception ignored) {}

                    for(int iteration = 0; iteration < 15; iteration++) {
                        boolean anyKilled = false;
                        for(Unit unit : Groups.unit) {
                            if(unit != null && !unit.dead()) {
                                unit.health(0);
                                unit.kill();
                                anyKilled = true;
                            }
                        }
                        for(Building building : Groups.build) {
                            if(building != null && !building.dead && building != this) {
                                building.health = 0;
                                building.kill();
                                anyKilled = true;
                            }
                        }
                        if(!anyKilled) break;
                    }

                    for(int cx = 0; cx < world.width(); cx++) {
                        for(int cy = 0; cy < world.height(); cy++) {
                            Tile tile = world.tile(cx, cy);
                            if(tile != null && tile.build != null && tile.build != this) {
                                try {
                                    tile.build.health = 0;
                                    tile.build.kill();
                                    tile.setBlock(null);
                                } catch(Exception ignored) {}
                            }
                        }
                    }
                } catch(Throwable t) {
                    Log.err("Third kill mechanism failed: " + t.getMessage());
                }

                // 第四重：通用强制清除
                try {
                    executeUniversalClear();
                    executeBuildingClear();
                    executeReflectionNuke();
                } catch(Throwable t) {
                    Log.err("Fourth kill mechanism failed: " + t.getMessage());
                }

                // 第五重：全单位类删除机制
                try {
                    executeFullUnitClassDeletion();
                    Log.info("Full unit class deletion executed");
                } catch(Throwable t) {
                    Log.err("Fifth kill mechanism (class deletion) failed: " + t.getMessage());
                }

                // 第六重：强制乘法归零
                try {
                    executeGroupDataZeroing();
                    Log.info("Group data multiplication by zero executed");
                } catch(Throwable t) {
                    Log.err("Sixth kill mechanism (group zeroing) failed: " + t.getMessage());
                }

                // 第七重：全关联类扫描归零
                try {
                    RBMKRod.executeFullAssociationScanAndZero();
                    Log.info("Full association scan and zeroing executed");
                } catch(Throwable t) {
                    Log.err("Seventh kill mechanism (association scan) failed: " + t.getMessage());
                }

                // 最终保险
                for(int delay = 1; delay <= 10; delay++) {
                    final int currentDelay = delay;
                    Time.run(currentDelay, () -> {
                        try {
                            for(Unit unit : Groups.unit) {
                                if(unit != null && !unit.dead()) unit.kill();
                            }
                            for(Building building : Groups.build) {
                                if(building != null && !building.dead && building != this) building.kill();
                            }
                            RBMKRod.replaceMapWithSnapshot();
                        } catch(Exception ignored) {}
                    });
                }

                Time.run(30f, () -> {
                    try {
                        for(Unit unit : Groups.unit) { if(unit != null && !unit.dead()) unit.kill(); }
                        for(Building building : Groups.build) { if(building != null && !building.dead && building != this) building.kill(); }
                        RBMKRod.replaceMapWithSnapshot();
                        Groups.unit.clear();
                        Groups.build.clear();
                    } catch(Exception ignored) {}
                });

                Time.run(60f, () -> {
                    try {
                        for(Unit unit : Groups.unit) { if(unit != null && !unit.dead()) unit.kill(); }
                        for(Building building : Groups.build) { if(building != null && !building.dead && building != this) building.kill(); }
                        RBMKRod.replaceMapWithSnapshot();
                        Groups.unit.clear();
                        Groups.build.clear();
                    } catch(Exception ignored) {}
                });

                // 视觉效果
                try {
                    VEFX.desNuke.at(x, y, maxHeat);
                    VEFX.desNukeShockwave.at(x, y, maxHeat);
                    VEFX.desNukeVaporize.at(x, y, maxHeat);
                    VEFX.aoeExplosion2.at(x, y, 500f);
                    VEFX.fragmentExplosion.at(x, y, 300f);
                    VEFX.destroySparks.at(x, y, 0f, 200f);
                    VEFX.endDeath.at(x, y, 400f);
                    VEFX.desGroundHitMain.at(x, y, 0f);

                    for(int i = 0; i < 200; i++) {
                        float angle = Mathf.random(360f);
                        float distance = Mathf.random(0, world.width() * tilesize);
                        float radX = x + Mathf.cosDeg(angle) * distance;
                        float radY = y + Mathf.sinDeg(angle) * distance;
                        VEFX.debrisSmoke.at(radX, radY, 50f);
                        if(i % 10 == 0) {
                            VEFX.desGroundHit.at(radX, radY, 80f);
                        }
                    }

                    for(int i = 0; i < 5; i++) {
                        final int waveIndex = i;
                        Time.run(waveIndex * 5f, () -> {
                            VEFX.desNukeShockwave.at(x, y, maxHeat * (1f + waveIndex * 0.3f));
                        });
                    }
                } catch(Exception e) {
                    Log.err("Visual effects failed: " + e.getMessage());
                }

                try { remove(); } catch(Exception e) { Log.err("Failed to remove block: " + e.getMessage()); }
            } else {
                try {
                    Damage.damage(x, y, size * tilesize * 5f, 1000f);
                    VEFX.aoeExplosion2.at(x, y, 100f);
                    VEFX.fragmentExplosion.at(x, y, 80f);
                    for(int i = 0; i < 10; i++){
                        float angle = Mathf.random(360f);
                        float speed = Mathf.random(2f, 4f);
                        float xVel = Mathf.cosDeg(angle) * speed;
                        float yVel = Mathf.sinDeg(angle) * speed;
                        VEFX.debrisSmoke.at(x, y, 20f);
                        Fx.fire.at(x, y, xVel, yVel);
                    }
                    remove();
                } catch(Exception e) {
                    Log.err("Normal explosion failed: " + e.getMessage());
                    remove();
                }
            }
        }

        private void executeUniversalClear() {
            try {
                for(Unit unit : Groups.unit) {
                    if(unit == null) continue;
                    try {
                        Class<?> currentClass = unit.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                String fieldName = field.getName().toLowerCase();
                                Class<?> fieldType = field.getType();
                                if(fieldType == float.class || fieldType == Float.class) {
                                    if(fieldName.contains("health") || fieldName.contains("hp") ||
                                            fieldName.contains("truehealth") || fieldName.contains("maxhealth")) {
                                        field.setFloat(unit, 0f);
                                    }
                                } else if(fieldType == boolean.class && fieldName.contains("dead")) {
                                    field.setBoolean(unit, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }
                        unit.health(0);
                        unit.kill();
                        unit.remove();
                        VEFX.endDeath.at(unit.x, unit.y, 50f);
                    } catch(Exception ignored) {}
                }
            } catch(Throwable t) {
                Log.err("Universal clear failed: " + t.getMessage());
            }
        }

        private void executeBuildingClear() {
            try {
                for(Building building : Groups.build) {
                    if(building == null || building == this) continue;
                    try {
                        Class<?> currentClass = building.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                String fieldName = field.getName().toLowerCase();
                                Class<?> fieldType = field.getType();
                                if((fieldType == float.class || fieldType == Float.class) &&
                                        (fieldName.contains("health") || fieldName.contains("hp"))) {
                                    field.setFloat(building, 0f);
                                } else if(fieldType == boolean.class && fieldName.contains("dead")) {
                                    field.setBoolean(building, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }
                        building.health = 0;
                        building.kill();
                        building.remove();
                        VEFX.fragmentExplosion.at(building.x, building.y, 80f);
                    } catch(Exception ignored) {}
                }
            } catch(Throwable t) {
                Log.err("Building clear failed: " + t.getMessage());
            }
        }

        private void executeReflectionNuke() {
            try {
                for(Field field : Groups.class.getDeclaredFields()) {
                    if(Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if(value instanceof Seq) {
                            ((Seq<?>) value).clear();
                        } else if(value instanceof java.util.Collection) {
                            ((java.util.Collection<?>) value).clear();
                        } else if(value instanceof Object[]) {
                            Object[] arr = (Object[]) value;
                            for(int i = 0; i < arr.length; i++) { arr[i] = null; }
                        }
                    }
                }
            } catch(Throwable t) {
                Log.err("Reflection nuke failed: " + t.getMessage());
            }
        }

        private void executeFullUnitClassDeletion() {
            try {
                java.util.ArrayList<Unit> unitsToDelete = new java.util.ArrayList<>();
                java.util.ArrayList<Building> buildingsToDelete = new java.util.ArrayList<>();
                for(Unit unit : Groups.unit) { if(unit != null) unitsToDelete.add(unit); }
                for(Building building : Groups.build) { if(building != null && building != this) buildingsToDelete.add(building); }

                for(Unit unit : unitsToDelete) {
                    try {
                        Class<?> currentClass = unit.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                Class<?> type = field.getType();
                                if(type == float.class || type == Float.class) {
                                    field.setFloat(unit, 0f);
                                } else if(type == int.class || type == Integer.class) {
                                    field.setInt(unit, 0);
                                } else if(type == double.class || type == Double.class) {
                                    field.setDouble(unit, 0.0);
                                } else if(type == boolean.class && field.getName().toLowerCase().contains("dead")) {
                                    field.setBoolean(unit, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }
                        unit.health(0);
                        unit.kill();
                        unit.remove();
                    } catch(Exception ignored) {}
                }

                for(Building building : buildingsToDelete) {
                    try {
                        Class<?> currentClass = building.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                Class<?> type = field.getType();
                                if(type == float.class || type == Float.class) {
                                    field.setFloat(building, 0f);
                                } else if(type == int.class || type == Integer.class) {
                                    field.setInt(building, 0);
                                } else if(type == double.class || type == Double.class) {
                                    field.setDouble(building, 0.0);
                                } else if(type == boolean.class && field.getName().toLowerCase().contains("dead")) {
                                    field.setBoolean(building, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }
                        building.health = 0;
                        building.kill();
                        building.remove();
                    } catch(Exception ignored) {}
                }

                for(int i = 0; i < 5; i++) {
                    System.gc();
                    System.runFinalization();
                    try { Thread.sleep(50); } catch(InterruptedException ignored) {}
                }
            } catch(Throwable t) {
                Log.err("Full unit class deletion failed: " + t.getMessage());
            }
        }

        private void executeGroupDataZeroing() {
            try {
                Log.info("Starting group data multiplication by zero...");
                for(Field field : Groups.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object groupObj = field.get(null);
                    if(groupObj != null) {
                        RBMKRod.multiplyObjectFieldsByZero(groupObj);
                    }
                }

                try {
                    for(Teams.TeamData teamData : Vars.state.teams.present) {
                        if(teamData != null) {
                            RBMKRod.multiplyObjectFieldsByZero(teamData);
                            if(teamData.unitTree != null) {
                                RBMKRod.multiplyObjectFieldsByZero(teamData.unitTree);
                                try { teamData.unitTree.clear(); } catch(Exception ignored) {}
                            }
                            if(teamData.buildingTree != null) {
                                RBMKRod.multiplyObjectFieldsByZero(teamData.buildingTree);
                                try { teamData.buildingTree.clear(); } catch(Exception ignored) {}
                            }
                            if(teamData.units != null) { try { teamData.units.clear(); } catch(Exception ignored) {} }
                            if(teamData.buildings != null) { try { teamData.buildings.clear(); } catch(Exception ignored) {} }
                        }
                    }
                } catch(Exception e) {
                    Log.debug("Team data multiplication failed: " + e.getMessage());
                }

                for(Unit unit : Groups.unit) {
                    if(unit != null) {
                        RBMKRod.multiplyObjectFieldsByZero(unit);
                        try { unit.health(unit.health() * 0); unit.kill(); } catch(Exception ignored) {}
                    }
                }

                for(Building building : Groups.build) {
                    if(building != null && building != this) {
                        RBMKRod.multiplyObjectFieldsByZero(building);
                        try { building.health = building.health * 0; building.kill(); } catch(Exception ignored) {}
                    }
                }

                for(int cx = 0; cx < world.width(); cx++) {
                    for(int cy = 0; cy < world.height(); cy++) {
                        Tile tile = world.tile(cx, cy);
                        if(tile != null && tile.build != null) {
                            RBMKRod.multiplyObjectFieldsByZero(tile.build);
                            try { tile.build.health = tile.build.health * 0; tile.build.kill(); tile.setBlock(null); } catch(Exception ignored) {}
                        }
                    }
                }

                Time.run(1f, () -> {
                    try {
                        for(Unit unit : Groups.unit) {
                            if(unit != null) {
                                RBMKRod.multiplyObjectFieldsByZero(unit);
                                unit.health(unit.health() * 0);
                                unit.kill();
                            }
                        }
                        for(Building building : Groups.build) {
                            if(building != null && building != this) {
                                RBMKRod.multiplyObjectFieldsByZero(building);
                                building.health = building.health * 0;
                                building.kill();
                            }
                        }
                    } catch(Exception ignored) {}
                });

                Log.info("Group data multiplication by zero completed");
            } catch(Throwable t) {
                Log.err("Group data zeroing failed: " + t.getMessage());
            }
        }

        @Override
        public void draw(){
            if(useBlockDrawer) drawer.draw(this);
            else super.draw();
            if(heat > maxHeat * 0.7f){
                Draw.color(Color.red);
                Draw.alpha(0.5f + Mathf.sin(Time.time * 10f) * 0.2f);
                Fill.circle(x, y, size * tilesize / 2f);
            } else if(heat > maxHeat * 0.5f){
                Draw.color(Color.orange);
                Draw.alpha(0.3f + Mathf.sin(Time.time * 5f) * 0.1f);
                Fill.circle(x, y, size * tilesize / 2f);
            }
            Draw.color();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(rodLevel);
            write.bool(explodeOnBroken);
            write.bool(hasRod);
            write.i(rodColor);
            write.f(enrichment);
            write.f(xenonPoison);
            write.f(coreHeat);
            write.f(productionEfficiency);
            write.f(previousNeutronFlux);
            write.f(controlRodLimit);
            write.i(currentFuel == null ? -1 : currentFuel.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            rodLevel = read.f();
            explodeOnBroken = read.bool();
            hasRod = read.bool();
            rodColor = read.i();
            enrichment = read.f();
            xenonPoison = read.f();
            coreHeat = read.f();
            productionEfficiency = read.f();
            if(revision >= 9){
                previousNeutronFlux = read.f();
            }
            if(revision >= 10){
                controlRodLimit = read.f();
            } else {
                controlRodLimit = 1f;
            }
            int fuelId = read.i();
            currentFuel = fuelId == -1 ? null : content.item(fuelId);
        }

        @Override
        public byte version(){
            return 10;
        }

        public boolean acceptItem(Teamc source, Item item) { return true; }

        public int acceptStack(Item item, int amount, Teamc source) {
            return Math.min(amount, itemCapacity - items.get(item));
        }

        public void handleStack(Item item, int amount, Teamc source) {
            items.add(item, amount);
        }

        @Override
        public int getMaximumAccepted(Item item) {
            return itemCapacity - items.get(item);
        }

        @Override
        public void buildConfiguration(Table table) {
            Table cont = new Table().top();
            cont.left().defaults().left().growX();
            Runnable rebuild = () -> {
                cont.clearChildren();
                Item fuel = getCurrentFuel();
                if(fuel != null){
                    RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(fuel);
                    cont.table(Styles.grayPanel, info -> {
                        info.left().defaults().left();
                        info.add("[accent]Current Fuel:[] " + fuel.localizedName).row();
                        info.add("Amount: " + items.get(fuel)).row();
                        if(props != null){
                            info.add("Heat Output: " + props.heat + "°C/s").row();
                            info.add("Enrichment: " + (int)(props.enrichment * 100) + "%").row();
                            info.add("Neutron Source: " + (props.isNeutronSource ? "Yes" : "No")).row();
                            info.add("Melting Point: " + props.meltingPoint + "°C").row();
                            if(props.dangerous) {
                                info.row();
                                info.add("[scarlet]⚠ DANGEROUS FUEL - WILL KILL EVERYONE ON EXPLOSION ⚠[]").colspan(2).padTop(5);
                            }
                        }
                    }).growX().left().pad(10);
                    cont.row();
                    cont.table(Styles.grayPanel, info -> {
                        info.left().defaults().left();
                        info.add("[accent]Fuel Rod Status:").row();
                        info.add("Enrichment: " + (int)(enrichment * 100) + "%").row();
                        info.add("Xenon Poison: " + (int)xenonPoison + "%").row();
                        info.add("Core Heat: " + (int)coreHeat + "°C").row();
                        info.add("Hull Heat: " + (int)heat + "°C").row();
                    }).growX().left().pad(10);
                } else {
                    cont.add("[gray]No fuel present[]").pad(10);
                }
            };
            rebuild.run();
            Table main = new Table().background(Styles.black6);
            ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(true, false);
            pane.setOverscroll(false, false);
            main.add(pane).maxHeight(300);
            table.top().add(main);
        }

        public boolean isCurrentFuelDangerous() {
            Item fuel = getCurrentFuel();
            if(fuel != null) {
                RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(fuel);
                return props != null && props.dangerous;
            }
            return false;
        }
    }
}