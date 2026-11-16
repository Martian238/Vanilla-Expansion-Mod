package VanillaExpansion.expand.world.block.debug.Module;

import VanillaExpansion.expand.world.block.debug.DebugBlock;
import VanillaExpansion.expand.world.block.debug.DebugModule;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.graphics.Drawf;
import mindustry.graphics.Lod;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;

public class OverdriveProjectorModule implements DebugModule {
    public static final String ID = "overdrive-projector";
    public static final String CONFIG_PREFIX = "overdrive";

    public static final String KEY_RANGE = "range";
    public static final String KEY_RELOAD = "reload";
    public static final String KEY_SPEED = "speed";
    public static final String KEY_SPEED_PHASE = "speedphase";
    public static final String KEY_PHASE_RANGE = "phaserange";
    public static final String KEY_HAS_BOOST = "hasboost";
    public static final String KEY_BOOSTED = "boosted";

    public static final String NAME_KEY = "debug.module.overdrive.name";
    public static final String NAME_DEFAULT = "超速投影 / Overdrive Projector";

    public static final String KEY_RANGE_LABEL = "debug.module.overdrive.range";
    public static final String RANGE_LABEL = "范围 / Range";
    public static final String KEY_RELOAD_LABEL = "debug.module.overdrive.reload";
    public static final String RELOAD_LABEL = "间隔 / Reload";
    public static final String KEY_SPEED_LABEL = "debug.module.overdrive.speed";
    public static final String SPEED_LABEL = "加速倍率 / Speed Boost";
    public static final String KEY_SPEED_PHASE_LABEL = "debug.module.overdrive.speedPhase";
    public static final String SPEED_PHASE_LABEL = "强化加速 / Phase Boost";
    public static final String KEY_PHASE_RANGE_LABEL = "debug.module.overdrive.phaseRange";
    public static final String PHASE_RANGE_LABEL = "强化范围 / Phase Range";
    public static final String KEY_HAS_BOOST_LABEL = "debug.module.overdrive.hasBoost";
    public static final String HAS_BOOST_LABEL = "允许强化 / Has Boost";
    public static final String KEY_BOOSTED_LABEL = "debug.module.overdrive.boosted";
    public static final String BOOSTED_LABEL = "强化状态 / Phase Boosted";
    public static final String KEY_STATUS = "debug.module.overdrive.status";
    public static final String STATUS = "[lightgray]加速：";

    public static final Color BASE_COLOR = Color.valueOf("feb380");
    public static final Color PHASE_COLOR = Color.valueOf("ffd59e");

    public static final float MIN_RANGE = 1f, MAX_RANGE = 40f;
    public static final float MIN_RELOAD = 10f, MAX_RELOAD = 600f;
    public static final float MIN_SPEED = 1f, MAX_SPEED = 10f;
    public static final float MIN_SPEED_PHASE = 0f, MAX_SPEED_PHASE = 10f;
    public static final float MIN_PHASE_RANGE = 0f, MAX_PHASE_RANGE = 40f;
    public static final float TOGGLE_HEIGHT = 34f;

    // 范围单位为格
    public float range = 10f;
    public float reload = 60f;
    public float speedBoost = 1.5f;
    public float speedBoostPhase = 0.75f;
    public float phaseRangeBoost = 2.5f;
    public boolean hasBoost = true;
    public boolean boosted = false;

    public float heat = 0f;
    public float charge = 0f;
    public float phaseHeat = 0f;

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
        heat = 0f;
        charge = 0f;
        phaseHeat = 0f;
    }

    // 随建筑更新加速
    @Override
    public void update(DebugBlock.DebugBuild build) {
        heat = Mathf.lerpDelta(heat, 1f, 0.08f);
        if (hasBoost) phaseHeat = Mathf.lerpDelta(phaseHeat, boosted ? 1f : 0f, 0.1f);
        charge += heat * Time.delta;

        if (charge >= reload) {
            charge = 0f;
            float realRange = (range + phaseHeat * phaseRangeBoost) * Vars.tilesize;
            float boost = realBoost();
            Vars.indexer.eachBlock(build, realRange,
                    other -> other.block.canOverdrive,
                    other -> other.applyBoost(boost, reload + 1f));
        }
    }

    // 随建筑绘制加速效果
    @Override
    public void draw(DebugBlock.DebugBuild build) {
        if (!Lod.l2) return;
        float f = 1f - (Time.time / 100f) % 1f;
        float half = build.block.size * Vars.tilesize / 2f;

        Draw.color(BASE_COLOR, PHASE_COLOR, phaseHeat);
        Draw.alpha(Lod.alpha2);
        Lines.stroke((2f * f + 0.1f) * heat);

        float r = Math.max(0f, Mathf.clamp(2f - f * 2f) * half - f - 0.2f), w = Mathf.clamp(0.5f - f) * half * 2f;
        Lines.beginLine();
        for (int i = 0; i < 4; i++) {
            Lines.linePoint(build.x + Geometry.d4(i).x * r + Geometry.d4(i).y * w,
                    build.y + Geometry.d4(i).y * r - Geometry.d4(i).x * w);
            if (f < 0.5f) {
                Lines.linePoint(build.x + Geometry.d4(i).x * r - Geometry.d4(i).y * w,
                        build.y + Geometry.d4(i).y * r + Geometry.d4(i).x * w);
            }
        }
        Lines.endLine(true);

        Draw.reset();
    }

    // 鼠标悬停时绘制加速范围与名称
    @Override
    public void drawSelect(DebugBlock.DebugBuild build) {
        float rad = (range + phaseHeat * phaseRangeBoost) * Vars.tilesize;
        Vars.indexer.eachBlock(build, rad, other -> other.block.canOverdrive,
                other -> Drawf.selected(other, Tmp.c1.set(BASE_COLOR).a(Mathf.absin(4f, 1f))));
        Drawf.dashCircle(build.x, build.y, rad, BASE_COLOR);
        Drawf.text(displayName(), build.x, build.y + rad + 6f, BASE_COLOR, 1f);
    }

    // 处理卡片配置界面
    @Override
    public void buildConfig(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {
        table.add(toggleRow(build, rebuild)).growX().padBottom(4f).row();
        addSlider(build, table, label(KEY_RANGE_LABEL, RANGE_LABEL), KEY_RANGE, MIN_RANGE, MAX_RANGE, 0.5f, range, false);
        addSlider(build, table, label(KEY_RELOAD_LABEL, RELOAD_LABEL), KEY_RELOAD, MIN_RELOAD, MAX_RELOAD, 5f, reload, true);
        addSlider(build, table, label(KEY_SPEED_LABEL, SPEED_LABEL), KEY_SPEED, MIN_SPEED, MAX_SPEED, 0.05f, speedBoost, false);
        addSlider(build, table, label(KEY_SPEED_PHASE_LABEL, SPEED_PHASE_LABEL), KEY_SPEED_PHASE, MIN_SPEED_PHASE, MAX_SPEED_PHASE, 0.05f, speedBoostPhase, false);
        addSlider(build, table, label(KEY_PHASE_RANGE_LABEL, PHASE_RANGE_LABEL), KEY_PHASE_RANGE, MIN_PHASE_RANGE, MAX_PHASE_RANGE, 0.5f, phaseRangeBoost, false);
        table.label(this::status).left().padTop(2f).row();
    }

    // 接收配置指令
    @Override
    public boolean configure(DebugBlock.DebugBuild build, Object value) {
        if (!(value instanceof String command)) return false;
        String[] parts = command.split(":");
        if (parts.length != 3 || !parts[0].equals(CONFIG_PREFIX)) return false;

        if (parts[1].equals(KEY_HAS_BOOST)) {
            hasBoost = Boolean.parseBoolean(parts[2]);
            if (!hasBoost) phaseHeat = 0f;
            return true;
        }
        if (parts[1].equals(KEY_BOOSTED)) {
            boosted = Boolean.parseBoolean(parts[2]);
            return true;
        }

        float amount;
        try {
            amount = Float.parseFloat(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }

        switch (parts[1]) {
            case KEY_RANGE -> range = Mathf.clamp(amount, MIN_RANGE, MAX_RANGE);
            case KEY_RELOAD -> reload = Mathf.clamp(amount, MIN_RELOAD, MAX_RELOAD);
            case KEY_SPEED -> speedBoost = Mathf.clamp(amount, MIN_SPEED, MAX_SPEED);
            case KEY_SPEED_PHASE -> speedBoostPhase = Mathf.clamp(amount, MIN_SPEED_PHASE, MAX_SPEED_PHASE);
            case KEY_PHASE_RANGE -> phaseRangeBoost = Mathf.clamp(amount, MIN_PHASE_RANGE, MAX_PHASE_RANGE);
            default -> {
                return false;
            }
        }
        return true;
    }

    // 写入模块参数
    @Override
    public void write(DebugBlock.DebugBuild build, Writes write) {
        write.f(range);
        write.f(reload);
        write.f(speedBoost);
        write.f(speedBoostPhase);
        write.f(phaseRangeBoost);
        write.bool(hasBoost);
        write.bool(boosted);
        write.f(heat);
        write.f(charge);
        write.f(phaseHeat);
    }

    // 读取模块参数
    @Override
    public void read(DebugBlock.DebugBuild build, Reads read, byte revision) {
        range = Mathf.clamp(read.f(), MIN_RANGE, MAX_RANGE);
        reload = Mathf.clamp(read.f(), MIN_RELOAD, MAX_RELOAD);
        speedBoost = Mathf.clamp(read.f(), MIN_SPEED, MAX_SPEED);
        speedBoostPhase = Mathf.clamp(read.f(), MIN_SPEED_PHASE, MAX_SPEED_PHASE);
        phaseRangeBoost = Mathf.clamp(read.f(), MIN_PHASE_RANGE, MAX_PHASE_RANGE);
        hasBoost = read.bool();
        boosted = read.bool();
        if (revision >= 2) {
            heat = read.f();
            charge = read.f();
            phaseHeat = read.f();
        } else {
            heat = 0f;
            charge = 0f;
            phaseHeat = 0f;
        }
        if (!hasBoost) phaseHeat = 0f;
    }

    // 构建模块状态栏（与 OverdriveProjector 原版一致）
    @Override
    public boolean buildBars(DebugBlock.DebugBuild build, Table table) {
        table.add(new Bar(
                        () -> Core.bundle.format("bar.boost", Mathf.round(Math.max(realBoost() * 100f - 100f, 0f))),
                        () -> Pal.accent,
                        () -> realBoost() / (hasBoost ? speedBoost + speedBoostPhase : speedBoost)))
                .growX().height(18f).row();
        return true;
    }

    // 返回实际加速倍率
    public float realBoost() {
        return speedBoost + phaseHeat * speedBoostPhase;
    }

    // 构建开关行
    private Table toggleRow(DebugBlock.DebugBuild build, Runnable rebuild) {
        Table row = new Table();
        row.left();
        row.defaults().growX().height(TOGGLE_HEIGHT).pad(2f);
        row.button(label(KEY_HAS_BOOST_LABEL, HAS_BOOST_LABEL), Styles.flatTogglet,
                        () -> setHasBoost(build, !hasBoost, rebuild))
                .checked(b -> hasBoost);
        row.button(label(KEY_BOOSTED_LABEL, BOOSTED_LABEL), Styles.flatTogglet,
                        () -> setBoosted(build, !boosted, rebuild))
                .checked(b -> boosted);
        return row;
    }

    // 切换是否允许强化
    private void setHasBoost(DebugBlock.DebugBuild build, boolean value, Runnable rebuild) {
        if (hasBoost == value) return;
        hasBoost = value;
        if (!value) phaseHeat = 0f;
        build.configure(CONFIG_PREFIX + ":" + KEY_HAS_BOOST + ":" + value);
        if (rebuild != null) rebuild.run();
    }

    // 切换强化状态
    private void setBoosted(DebugBlock.DebugBuild build, boolean value, Runnable rebuild) {
        if (boosted == value) return;
        boosted = value;
        build.configure(CONFIG_PREFIX + ":" + KEY_BOOSTED + ":" + value);
        if (rebuild != null) rebuild.run();
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
            case KEY_RELOAD -> reload = Mathf.clamp(value, MIN_RELOAD, MAX_RELOAD);
            case KEY_SPEED -> speedBoost = Mathf.clamp(value, MIN_SPEED, MAX_SPEED);
            case KEY_SPEED_PHASE -> speedBoostPhase = Mathf.clamp(value, MIN_SPEED_PHASE, MAX_SPEED_PHASE);
            case KEY_PHASE_RANGE -> phaseRangeBoost = Mathf.clamp(value, MIN_PHASE_RANGE, MAX_PHASE_RANGE);
        }
        build.configure(CONFIG_PREFIX + ":" + key + ":" + (integer ? Integer.toString((int) value) : Float.toString(value)));
    }

    // 返回卡片状态文本
    private String status() {
        float realRange = range + phaseHeat * phaseRangeBoost;
        return Core.bundle.get(KEY_STATUS, STATUS) + " [accent]+"
                + Mathf.round(Math.max(realBoost() * 100f - 100f, 0f)) + "%[] / [accent]"
                + Strings.fixed(realRange, 1) + "[]";
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
