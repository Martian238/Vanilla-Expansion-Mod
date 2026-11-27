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
    public static final String ID = "source";
    public static final String CONFIG_PREFIX = "source";
    public static final String KEY_MODE = "mode";
    public static final String KEY_RANGE = "range";
    public static final String KEY_ITEM_RATE = "itemrate";
    public static final String KEY_LIQUID_RATE = "liquidrate";
    public static final String KEY_POWER_RATE = "powerrate";
    public static final String KEY_ITEM = "item";
    public static final String KEY_LIQUID = "liquid";
    public static final String KEY_ITEM_ENABLED = "itemenabled";
    public static final String KEY_LIQUID_ENABLED = "liquidEnabled";
    public static final String KEY_POWER_ENABLED = "powerEnabled";
    public static final String NAME_KEY = "debug.module.source.name";
    public static final String NAME_DEFAULT = "源 / Source";
    public static final String MODE_ITEM_KEY = "debug.module.source.mode.item";
    public static final String MODE_ITEM = "物品 / Item";
    public static final String MODE_LIQUID_KEY = "debug.module.source.mode.liquid";
    public static final String MODE_LIQUID = "液体 / Liquid";
    public static final String MODE_POWER_KEY = "debug.module.source.mode.power";
    public static final String MODE_POWER = "电力 / Power";
    public static final String KEY_RANGE_LABEL = "debug.module.source.range";
    public static final String RANGE_LABEL = "范围 / Range";
    public static final String KEY_ITEM_RATE_LABEL = "debug.module.source.itemRate";
    public static final String ITEM_RATE_LABEL = "物品/秒 / Items/s";
    public static final String KEY_LIQUID_RATE_LABEL = "debug.module.source.liquidRate";
    public static final String LIQUID_RATE_LABEL = "液体/秒 / Liquid/s";
    public static final String KEY_POWER_RATE_LABEL = "debug.module.source.powerRate";
    public static final String POWER_RATE_LABEL = "电力/秒 / Power/s";
    public static final String KEY_STATUS = "debug.module.source.status";
    public static final String STATUS = "[lightgray]输出：";
    public static final Color RANGE_COLOR = Color.valueOf("6ec6f0");
    public static final float MIN_RANGE = 1f;
    public static final float MAX_RANGE = 40f;
    public static final float MIN_ITEM_RATE = 1f;
    public static final float MAX_ITEM_RATE = 1000f;
    public static final float MIN_LIQUID_RATE = 1f;
    public static final float MAX_LIQUID_RATE = 10000f;
    public static final float MIN_POWER_RATE = 1f;
    public static final float MAX_POWER_RATE = 1000000f;
    public static final float SECONDS_TO_TICKS = 60f;
    public static final float MODE_BUTTON_HEIGHT = 34f;
    public static final float ICON_BUTTON_SIZE = 42f;
    public static final float ICON_SIZE = 32f;
    public static final int ICON_ROW_COUNT = 11;
    public static final int ITEM_BUFFER = 1;
    public boolean itemEnabled = true;
    public boolean liquidEnabled = false;
    public boolean powerEnabled = false;
    public float range = 10f;
    public float itemRate = 100f;
    public float liquidRate = 1000f;
    public float powerRate = 10000f;
    public Item outputItem = null;
    public Liquid outputLiquid = null;
    public float itemCounter = 0f;
    public float liquidCounter = 0f;
    private DebugBlock.DebugBuild activeBuild;
    @Override
    public String id() {
        return ID;
    }
    @Override
    public String displayName() {
        return Core.bundle.get(NAME_KEY, NAME_DEFAULT);
    }
    @Override
    public void attach(DebugBlock.DebugBuild build) {
        activeBuild = build;
        itemCounter = 0f;
        liquidCounter = 0f;
        build.modulePowerProduction = 0f;
    }
    @Override
    public void detach(DebugBlock.DebugBuild build) {
        activeBuild = null;
        build.modulePowerProduction = 0f;
    }
    @Override
    public void update(DebugBlock.DebugBuild build) {
        activeBuild = build;
        build.modulePowerProduction = 0f;
        if (itemEnabled) updateItem(build);
        if (liquidEnabled) updateLiquid(build);
        if (powerEnabled) updatePower(build);
    }
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
    private void updatePower(DebugBlock.DebugBuild build) {
        build.modulePowerProduction = powerRate / SECONDS_TO_TICKS;
        if (build.power == null) return;
        float realRange = range * Vars.tilesize;
        boolean changed = false;
        IntSeq targets = new IntSeq();
        Vars.indexer.eachBlock(build, realRange,
                other -> other != build && other.team == build.team
                        && other.power != null && other.block.hasPower && other.block.connectedPower,
                other -> targets.add(other.pos()));
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
        if (changed) {
            PowerGraph graph = new PowerGraph();
            graph.reflow(build);
            graph.update();
        }
    }
    @Override
    public void drawSelect(DebugBlock.DebugBuild build) {
        float rad = range * Vars.tilesize;
        Drawf.dashCircle(build.x, build.y, rad, RANGE_COLOR);
        Drawf.text(displayName(), build.x, build.y + rad + 6f, RANGE_COLOR, 1f);
    }
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
    private void applyValue(DebugBlock.DebugBuild build, String key, float value, boolean integer) {
        switch (key) {
            case KEY_RANGE -> range = Mathf.clamp(value, MIN_RANGE, MAX_RANGE);
            case KEY_ITEM_RATE -> itemRate = Mathf.clamp(value, MIN_ITEM_RATE, MAX_ITEM_RATE);
            case KEY_LIQUID_RATE -> liquidRate = Mathf.clamp(value, MIN_LIQUID_RATE, MAX_LIQUID_RATE);
            case KEY_POWER_RATE -> powerRate = Mathf.clamp(value, MIN_POWER_RATE, MAX_POWER_RATE);
        }
        build.configure(CONFIG_PREFIX + ":" + key + ":" + (integer ? Integer.toString((int) value) : Float.toString(value)));
    }
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
    private String format(float value, boolean integer) {
        return integer ? Integer.toString((int) value) : Strings.fixed(value, 2);
    }
    private String label(String key, String fallback) {
        return Core.bundle.get(key, fallback);
    }
}