package VanillaExpansion.expand.world.block.debug.Module;

import VanillaExpansion.expand.world.block.debug.DebugBlock;
import VanillaExpansion.expand.world.block.debug.DebugModule;
import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.PowerGraph;

public class SourceModule implements DebugModule {
    // 模块唯一标识
    public static final String ID = "source";
    // 配置指令前缀
    public static final String CONFIG_PREFIX = "source";

    // 模式配置键
    public static final String KEY_MODE = "mode";
    // 范围配置键
    public static final String KEY_RANGE = "range";
    // 物品速率配置键
    public static final String KEY_ITEM_RATE = "itemrate";
    // 液体速率配置键
    public static final String KEY_LIQUID_RATE = "liquidrate";
    // 电力速率配置键
    public static final String KEY_POWER_RATE = "powerrate";
    // 物品类型配置键
    public static final String KEY_ITEM = "item";
    // 液体类型配置键
    public static final String KEY_LIQUID = "liquid";
    // 物品模式开关配置键
    public static final String KEY_ITEM_ENABLED = "itemenabled";
    // 液体模式开关配置键
    public static final String KEY_LIQUID_ENABLED = "liquidEnabled";
    // 电力模式开关配置键
    public static final String KEY_POWER_ENABLED = "powerEnabled";

    // 模块名称本地化键
    public static final String NAME_KEY = "debug.module.source.name";
    // 模块名称默认值
    public static final String NAME_DEFAULT = "源 / Source";

    // 物品模式本地化键
    public static final String MODE_ITEM_KEY = "debug.module.source.mode.item";
    // 物品模式默认值
    public static final String MODE_ITEM = "物品 / Item";
    // 液体模式本地化键
    public static final String MODE_LIQUID_KEY = "debug.module.source.mode.liquid";
    // 液体模式默认值
    public static final String MODE_LIQUID = "液体 / Liquid";
    // 电力模式本地化键
    public static final String MODE_POWER_KEY = "debug.module.source.mode.power";
    // 电力模式默认值
    public static final String MODE_POWER = "电力 / Power";

    // 范围标签本地化键
    public static final String KEY_RANGE_LABEL = "debug.module.source.range";
    // 范围标签默认值
    public static final String RANGE_LABEL = "范围 / Range";
    // 物品速率标签本地化键
    public static final String KEY_ITEM_RATE_LABEL = "debug.module.source.itemRate";
    // 物品速率标签默认值
    public static final String ITEM_RATE_LABEL = "物品/秒 / Items/s";
    // 液体速率标签本地化键
    public static final String KEY_LIQUID_RATE_LABEL = "debug.module.source.liquidRate";
    // 液体速率标签默认值
    public static final String LIQUID_RATE_LABEL = "液体/秒 / Liquid/s";
    // 电力速率标签本地化键
    public static final String KEY_POWER_RATE_LABEL = "debug.module.source.powerRate";
    // 电力速率标签默认值
    public static final String POWER_RATE_LABEL = "电力/秒 / Power/s";
    // 状态文本本地化键
    public static final String KEY_STATUS = "debug.module.source.status";
    // 状态文本默认值
    public static final String STATUS = "[lightgray]输出：";
    // 范围绘制颜色
    public static final Color RANGE_COLOR = Color.valueOf("6ec6f0");

    // 最小范围（格）
    public static final float MIN_RANGE = 1f;
    // 最大范围（格）
    public static final float MAX_RANGE = 40f;
    // 最小物品速率
    public static final float MIN_ITEM_RATE = 1f;
    // 最大物品速率
    public static final float MAX_ITEM_RATE = 1000f;
    // 最小液体速率
    public static final float MIN_LIQUID_RATE = 1f;
    // 最大液体速率
    public static final float MAX_LIQUID_RATE = 10000f;
    // 最小电力速率
    public static final float MIN_POWER_RATE = 1f;
    // 最大电力速率
    public static final float MAX_POWER_RATE = 1000000f;
    // 每秒到每 tick 的换算系数
    public static final float SECONDS_TO_TICKS = 60f;
    // 模式按钮高度
    public static final float MODE_BUTTON_HEIGHT = 34f;
    // 图标按钮尺寸
    public static final float ICON_BUTTON_SIZE = 42f;
    // 图标显示尺寸
    public static final float ICON_SIZE = 32f;
    // 图标表每行数量
    public static final int ICON_ROW_COUNT = 11;
    // 物品暂存上限
    public static final int ITEM_BUFFER = 1;

    // 物品模式是否启用
    public boolean itemEnabled = true;
    // 液体模式是否启用
    public boolean liquidEnabled = false;
    // 电力模式是否启用
    public boolean powerEnabled = false;
    // 供给范围（格）
    public float range = 10f;
    // 物品每秒输出量
    public float itemRate = 100f;
    // 液体每秒输出量
    public float liquidRate = 1000f;
    // 电力每秒输出量
    public float powerRate = 10000f;
    // 当前选中的物品
    public Item outputItem = null;
    // 当前选中的液体
    public Liquid outputLiquid = null;
    // 物品累积计数器
    public float itemCounter = 0f;
    // 液体累积计数器
    public float liquidCounter = 0f;
    // 当前附着建筑
    private DebugBlock.DebugBuild activeBuild;

    // 返回模块唯一标识
    @Override
    public String id() {
        return ID;
    }

    // 返回模块显示名称
    @Override
    public String displayName() {
        return Core.bundle.get(NAME_KEY, NAME_DEFAULT);
    }

    // 模块挂载到建筑
    @Override
    public void attach(DebugBlock.DebugBuild build) {
        activeBuild = build;
        itemCounter = 0f;
        liquidCounter = 0f;
        build.modulePowerProduction = 0f;
    }

    // 模块从建筑卸载
    @Override
    public void detach(DebugBlock.DebugBuild build) {
        activeBuild = null;
        build.modulePowerProduction = 0f;
    }

    // 随建筑更新三种供给
    @Override
    public void update(DebugBlock.DebugBuild build) {
        activeBuild = build;
        build.modulePowerProduction = 0f;
        if (itemEnabled) updateItem(build);
        if (liquidEnabled) updateLiquid(build);
        if (powerEnabled) updatePower(build);
    }

    // 更新物品供给（服务端可运行）
    private void updateItem(DebugBlock.DebugBuild build) {
        if (outputItem == null) return;
        float realRange = range * Vars.tilesize;
        itemCounter += itemRate / 60f * Time.delta;
        if (itemCounter < ITEM_BUFFER) return;
        int count = (int) itemCounter;
        itemCounter -= count;
        Vars.indexer.eachBlock(build, realRange,
                other -> other.team == build.team && other.acceptItem(build, outputItem),
                other -> {
                    for (int i = 0; i < count; i++) {
                        if (other.acceptItem(build, outputItem)) {
                            other.handleItem(build, outputItem);
                        }
                    }
                });
    }

    // 更新液体供给（服务端可运行）
    private void updateLiquid(DebugBlock.DebugBuild build) {
        if (outputLiquid == null) return;
        float realRange = range * Vars.tilesize;
        liquidCounter += liquidRate / 60f * Time.delta;
        if (liquidCounter <= 0f) return;
        float amount = liquidCounter;
        liquidCounter = 0f;
        Vars.indexer.eachBlock(build, realRange,
                other -> other.team == build.team && other.acceptLiquid(build, outputLiquid),
                other -> {
                    float cap = other.block.liquidCapacity;
                    float accepted = Math.min(amount, cap - other.liquids.get(outputLiquid));
                    if (accepted > 0f) {
                        other.handleLiquid(build, outputLiquid, accepted);
                    }
                });
    }

    // 更新电力供给：写入生产量并维护范围内双向 links
    private void updatePower(DebugBlock.DebugBuild build) {
        build.modulePowerProduction = powerRate / SECONDS_TO_TICKS;
        if (build.power == null) return;
        float realRange = range * Vars.tilesize;
        boolean changed = false;

        // 收集范围内应连接的建筑
        IntSeq targets = new IntSeq();
        Vars.indexer.eachBlock(build, realRange,
                other -> other != build && other.team == build.team
                        && other.power != null && other.block.hasPower && other.block.connectedPower,
                other -> targets.add(other.pos()));

        // 移除已超出范围或失效的 links
        for (int i = 0; i < build.power.links.size; i++) {
            int pos = build.power.links.get(i);
            if (!targets.contains(pos)) {
                Building other = Vars.world.build(pos);
                if (other != null && other.power != null) {
                    other.power.links.removeValue(build.pos());
                }
                build.power.links.removeIndex(i);
                i--;
                changed = true;
            }
        }

        // 添加新的 links（双向）
        for (int i = 0; i < targets.size; i++) {
            int pos = targets.get(i);
            if (!build.power.links.contains(pos)) {
                Building other = Vars.world.build(pos);
                if (other != null && other.power != null) {
                    build.power.links.add(pos);
                    if (!other.power.links.contains(build.pos())) {
                        other.power.links.add(build.pos());
                    }
                    changed = true;
                }
            }
        }

        // 有变化时从源建筑重建整个连通电网并刷新平衡
        if (changed) {
            PowerGraph graph = new PowerGraph();
            graph.reflow(build);
            graph.update();
        }
    }

    // 鼠标悬停时绘制供给范围
    @Override
    public void drawSelect(DebugBlock.DebugBuild build) {
        float rad = range * Vars.tilesize;
        Drawf.dashCircle(build.x, build.y, rad, RANGE_COLOR);
        Drawf.text(displayName(), build.x, build.y + rad + 6f, RANGE_COLOR, 1f);
    }

    // 处理卡片配置界面
    @Override
    public void buildConfig(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {
        table.add(modeRow(build, rebuild)).growX().padBottom(4f).row();
        addSlider(build, table, label(KEY_RANGE_LABEL, RANGE_LABEL), KEY_RANGE, MIN_RANGE, MAX_RANGE, 0.5f, range, false);
        if (itemEnabled) {
            addItemIconTable(build, table, rebuild);
            addSlider(build, table, label(KEY_ITEM_RATE_LABEL, ITEM_RATE_LABEL), KEY_ITEM_RATE, MIN_ITEM_RATE, MAX_ITEM_RATE, 1f, itemRate, true);
        }
        if (liquidEnabled) {
            addLiquidIconTable(build, table, rebuild);
            addSlider(build, table, label(KEY_LIQUID_RATE_LABEL, LIQUID_RATE_LABEL), KEY_LIQUID_RATE, MIN_LIQUID_RATE, MAX_LIQUID_RATE, 10f, liquidRate, true);
        }
        if (powerEnabled) {
            addSlider(build, table, label(KEY_POWER_RATE_LABEL, POWER_RATE_LABEL), KEY_POWER_RATE, MIN_POWER_RATE, MAX_POWER_RATE, 100f, powerRate, true);
        }
        table.label(this::status).left().padTop(2f).row();
    }

    // 接收配置指令
    @Override
    public boolean configure(DebugBlock.DebugBuild build, Object value) {
        if (!(value instanceof String command)) return false;
        String[] parts = command.split(":");
        if (parts.length != 3 || !parts[0].equals(CONFIG_PREFIX)) return false;

        switch (parts[1]) {
            case KEY_ITEM_ENABLED -> {
                itemEnabled = Boolean.parseBoolean(parts[2]);
                return true;
            }
            case KEY_LIQUID_ENABLED -> {
                liquidEnabled = Boolean.parseBoolean(parts[2]);
                return true;
            }
            case KEY_POWER_ENABLED -> {
                powerEnabled = Boolean.parseBoolean(parts[2]);
                return true;
            }
            case KEY_ITEM -> {
                outputItem = parts[2].equals("null") ? null : Vars.content.item(parts[2]);
                return true;
            }
            case KEY_LIQUID -> {
                outputLiquid = parts[2].equals("null") ? null : Vars.content.liquid(parts[2]);
                return true;
            }
        }

        float amount;
        try {
            amount = Float.parseFloat(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }

        switch (parts[1]) {
            case KEY_RANGE -> range = Mathf.clamp(amount, MIN_RANGE, MAX_RANGE);
            case KEY_ITEM_RATE -> itemRate = Mathf.clamp(amount, MIN_ITEM_RATE, MAX_ITEM_RATE);
            case KEY_LIQUID_RATE -> liquidRate = Mathf.clamp(amount, MIN_LIQUID_RATE, MAX_LIQUID_RATE);
            case KEY_POWER_RATE -> powerRate = Mathf.clamp(amount, MIN_POWER_RATE, MAX_POWER_RATE);
            default -> {
                return false;
            }
        }
        return true;
    }

    // 写入模块参数
    @Override
    public void write(DebugBlock.DebugBuild build, Writes write) {
        write.bool(itemEnabled);
        write.bool(liquidEnabled);
        write.bool(powerEnabled);
        write.f(range);
        write.f(itemRate);
        write.f(liquidRate);
        write.f(powerRate);
        write.s(outputItem == null ? -1 : outputItem.id);
        write.s(outputLiquid == null ? -1 : outputLiquid.id);
    }

    // 读取模块参数（兼容旧存档的 mode 布尔对）
    @Override
    public void read(DebugBlock.DebugBuild build, Reads read, byte revision) {
        itemEnabled = read.bool();
        liquidEnabled = read.bool();
        powerEnabled = read.bool();
        range = Mathf.clamp(read.f(), MIN_RANGE, MAX_RANGE);
        itemRate = Mathf.clamp(read.f(), MIN_ITEM_RATE, MAX_ITEM_RATE);
        liquidRate = Mathf.clamp(read.f(), MIN_LIQUID_RATE, MAX_LIQUID_RATE);
        powerRate = Mathf.clamp(read.f(), MIN_POWER_RATE, MAX_POWER_RATE);
        int itemId = read.s();
        outputItem = itemId == -1 ? null : Vars.content.item(itemId);
        int liquidId = read.s();
        outputLiquid = liquidId == -1 ? null : Vars.content.liquid(liquidId);
    }

    // 切换指定模式的启用状态
    private void setEnabled(DebugBlock.DebugBuild build, String key, boolean value, Runnable rebuild) {
        switch (key) {
            case KEY_ITEM_ENABLED -> itemEnabled = value;
            case KEY_LIQUID_ENABLED -> liquidEnabled = value;
            case KEY_POWER_ENABLED -> powerEnabled = value;
        }
        if (!powerEnabled) build.modulePowerProduction = 0f;
        build.configure(CONFIG_PREFIX + ":" + key + ":" + value);
        if (rebuild != null) rebuild.run();
    }

    // 构建开关行
    private Table modeRow(DebugBlock.DebugBuild build, Runnable rebuild) {
        Table row = new Table();
        row.left();
        row.defaults().growX().height(MODE_BUTTON_HEIGHT).pad(2f);
        row.button(label(MODE_ITEM_KEY, MODE_ITEM), Styles.flatTogglet,
                        () -> setEnabled(build, KEY_ITEM_ENABLED, !itemEnabled, rebuild))
                .checked(b -> itemEnabled);
        row.button(label(MODE_LIQUID_KEY, MODE_LIQUID), Styles.flatTogglet,
                        () -> setEnabled(build, KEY_LIQUID_ENABLED, !liquidEnabled, rebuild))
                .checked(b -> liquidEnabled);
        row.button(label(MODE_POWER_KEY, MODE_POWER), Styles.flatTogglet,
                        () -> setEnabled(build, KEY_POWER_ENABLED, !powerEnabled, rebuild))
                .checked(b -> powerEnabled);
        return row;
    }

    // 构建物品图标选择表
    private void addItemIconTable(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {
        Table grid = new Table();
        grid.left();
        int i = 0;
        for (Item item : Vars.content.items()) {
            Button btn = new Button(Styles.clearTogglei);
            btn.add(new Image(item.uiIcon)).size(ICON_SIZE);
            btn.setChecked(outputItem == item);
            btn.clicked(() -> {
                outputItem = item;
                build.configure(CONFIG_PREFIX + ":" + KEY_ITEM + ":" + item.name);
                if (rebuild != null) rebuild.run();
            });
            btn.addListener(new Tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName)));
            grid.add(btn).size(ICON_BUTTON_SIZE).pad(2f);
            if (++i % ICON_ROW_COUNT == 0) grid.row();
        }
        table.add(grid).growX().pad(2f).row();
    }

    // 构建液体图标选择表
    private void addLiquidIconTable(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {
        Table grid = new Table();
        grid.left();
        int i = 0;
        for (Liquid liquid : Vars.content.liquids()) {
            Button btn = new Button(Styles.clearTogglei);
            btn.add(new Image(liquid.uiIcon)).size(ICON_SIZE);
            btn.setChecked(outputLiquid == liquid);
            btn.clicked(() -> {
                outputLiquid = liquid;
                build.configure(CONFIG_PREFIX + ":" + KEY_LIQUID + ":" + liquid.name);
                if (rebuild != null) rebuild.run();
            });
            btn.addListener(new Tooltip(t -> t.background(Styles.black6).margin(4f).add(liquid.localizedName)));
            grid.add(btn).size(ICON_BUTTON_SIZE).pad(2f);
            if (++i % ICON_ROW_COUNT == 0) grid.row();
        }
        table.add(grid).growX().pad(2f).row();
    }

    // 构建参数滑条
    private void addSlider(DebugBlock.DebugBuild build, Table table, String label, String key,
                           float min, float max, float step, float value, boolean integer) {
        Table row = new Table();
        row.left();
        row.add(label).left().growX().color(Color.lightGray);

        Slider slider = new Slider(min, max, step, false);
        slider.setValue(value);
        Label amount = new Label(format(slider.getValue(), integer));

        slider.changed(() -> {
            amount.setText(format(slider.getValue(), integer));
            applyValue(build, key, slider.getValue(), integer);
        });

        row.add(slider).growX().padLeft(6f).padRight(6f);
        row.add(amount).right().width(70f);
        table.add(row).growX().pad(2f).row();
    }

    // 应用滑条数值
    private void applyValue(DebugBlock.DebugBuild build, String key, float value, boolean integer) {
        switch (key) {
            case KEY_RANGE -> range = Mathf.clamp(value, MIN_RANGE, MAX_RANGE);
            case KEY_ITEM_RATE -> itemRate = Mathf.clamp(value, MIN_ITEM_RATE, MAX_ITEM_RATE);
            case KEY_LIQUID_RATE -> liquidRate = Mathf.clamp(value, MIN_LIQUID_RATE, MAX_LIQUID_RATE);
            case KEY_POWER_RATE -> powerRate = Mathf.clamp(value, MIN_POWER_RATE, MAX_POWER_RATE);
        }
        build.configure(CONFIG_PREFIX + ":" + key + ":" + (integer ? Integer.toString((int) value) : Float.toString(value)));
    }

    // 返回卡片状态文本（每个启用模式独立占一行）
    private String status() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (itemEnabled) {
            sb.append(Core.bundle.get(KEY_STATUS, STATUS)).append(" [accent]")
                    .append(outputItem == null ? "无 / None" : outputItem.localizedName)
                    .append("[] / [accent]")
                    .append(Strings.fixed(itemRate, 0))
                    .append("[]/s");
            first = false;
        }
        if (liquidEnabled) {
            if (!first) sb.append("\n");
            sb.append(Core.bundle.get(KEY_STATUS, STATUS)).append(" [accent]")
                    .append(outputLiquid == null ? "无 / None" : outputLiquid.localizedName)
                    .append("[] / [accent]")
                    .append(Strings.fixed(liquidRate, 0))
                    .append("[]/s");
            first = false;
        }
        if (powerEnabled) {
            if (!first) sb.append("\n");
            sb.append(Core.bundle.get(KEY_STATUS, STATUS)).append(" [accent]")
                    .append(Strings.fixed(powerRate, 0))
                    .append("[]/s");
        }
        return sb.toString();
    }

    // 格式化数值
    private String format(float value, boolean integer) {
        return integer ? Integer.toString((int) value) : Strings.fixed(value, 2);
    }

    // 读取本地化文本
    private String label(String key, String fallback) {
        return Core.bundle.get(key, fallback);
    }
}