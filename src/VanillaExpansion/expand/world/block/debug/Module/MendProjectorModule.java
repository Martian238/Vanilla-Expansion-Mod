package VanillaExpansion.expand.world.block.debug.Module;
import VanillaExpansion.expand.world.block.debug.DebugBlock;
import VanillaExpansion.expand.world.block.debug.DebugModule;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
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
public class MendProjectorModule implements DebugModule {
    public static final String ID = "mend-projector";
    public static final String CONFIG_PREFIX = "mend";

    public static final String KEY_RANGE = "range";
    public static final String KEY_RELOAD = "reload";
    public static final String KEY_HEAL = "heal";
    public static final String KEY_PHASE_BOOST = "phaseboost";
    public static final String KEY_PHASE_RANGE = "phaserange";
    public static final String KEY_BOOSTED = "boosted";

    public static final String NAME_KEY = "debug.module.mend.name";
    public static final String NAME_DEFAULT = "修复投影 / Mend Projector";

    public static final String KEY_RANGE_LABEL = "debug.module.mend.range";
    public static final String RANGE_LABEL = "范围 / Range";
    public static final String KEY_RELOAD_LABEL = "debug.module.mend.reload";
    public static final String RELOAD_LABEL = "间隔 / Reload";
    public static final String KEY_HEAL_LABEL = "debug.module.mend.heal";
    public static final String HEAL_LABEL = "修复量 / Heal Percent";
    public static final String KEY_PHASE_BOOST_LABEL = "debug.module.mend.phaseBoost";
    public static final String PHASE_BOOST_LABEL = "强化修复 / Phase Heal";
    public static final String KEY_PHASE_RANGE_LABEL = "debug.module.mend.phaseRange";
    public static final String PHASE_RANGE_LABEL = "强化范围 / Phase Range";
    public static final String KEY_BOOSTED_LABEL = "debug.module.mend.boosted";
    public static final String BOOSTED_LABEL = "强化状态 / Phase Boosted";
    public static final String KEY_STATUS = "debug.module.mend.status";
    public static final String STATUS = "[lightgray]修复：";

    public static final Color BASE_COLOR = Color.valueOf("84f491");
    public static final Color PHASE_COLOR = BASE_COLOR;

    public static final float MIN_RANGE = 1f, MAX_RANGE = 40f;
    public static final float MIN_RELOAD = 10f, MAX_RELOAD = 600f;
    public static final float MIN_HEAL = 0.1f, MAX_HEAL = 100f;
    public static final float MIN_PHASE_BOOST = 0f, MAX_PHASE_BOOST = 100f;
    public static final float MIN_PHASE_RANGE = 0f, MAX_PHASE_RANGE = 40f;
    public static final float TOGGLE_HEIGHT = 34f;
    public float range = 7.5f;
    public float reload = 250f;
    public float healPercent = 12f;
    public float phaseBoost = 12f;
    public float phaseRangeBoost = 6.25f;
    public boolean boosted = false;
    public float heat = 0f;
    public float charge = 0f;
    public float phaseHeat = 0f;
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
        heat = 0f;
        charge = 0f;
        phaseHeat = 0f;
    }
    @Override
    public void update(DebugBlock.DebugBuild build) {
        heat = Mathf.lerpDelta(heat, 1f, 0.08f);
        phaseHeat = Mathf.lerpDelta(phaseHeat, boosted ? 1f : 0f, 0.1f);
        charge += heat * Time.delta;

        if (charge >= reload) {
            charge = 0f;
            float realRange = (range + phaseHeat * phaseRangeBoost) * Vars.tilesize;
            float amount = (healPercent + phaseHeat * phaseBoost) / 100f;
            Vars.indexer.eachBlock(build, realRange,
                    other -> other.damaged() && !other.isHealSuppressed(),
                    other -> {
                        other.heal(other.maxHealth() * amount);
                        other.recentlyHealed();
                    });
        }
    }
    @Override
    public void draw(DebugBlock.DebugBuild build) {
        if (!Lod.l2) return;
        float f = 1f - (Time.time / 100f) % 1f;
        float half = build.block.size * Vars.tilesize / 2f;

        Draw.color(BASE_COLOR, PHASE_COLOR, phaseHeat);
        Draw.alpha(Lod.alpha2);
        Lines.stroke((2f * f + 0.2f) * heat);
        Lines.square(build.x, build.y, Math.min(1f + (1f - f) * half, half));
        Draw.reset();
    }
    @Override
    public void drawSelect(DebugBlock.DebugBuild build) {
        float rad = (range + phaseHeat * phaseRangeBoost) * Vars.tilesize;
        Vars.indexer.eachBlock(build, rad, other -> true,
                other -> Drawf.selected(other, Tmp.c1.set(BASE_COLOR).a(Mathf.absin(4f, 1f))));
        Drawf.dashCircle(build.x, build.y, rad, BASE_COLOR);
        Drawf.text(displayName(), build.x, build.y + rad + 6f, BASE_COLOR, 1f);
    }
    @Override
    public void buildConfig(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {
        table.add(toggleRow(build, rebuild)).growX().padBottom(4f).row();
        addSlider(build, table, label(KEY_RANGE_LABEL, RANGE_LABEL), KEY_RANGE, MIN_RANGE, MAX_RANGE, 0.5f, range, false);
        addSlider(build, table, label(KEY_RELOAD_LABEL, RELOAD_LABEL), KEY_RELOAD, MIN_RELOAD, MAX_RELOAD, 5f, reload, true);
        addSlider(build, table, label(KEY_HEAL_LABEL, HEAL_LABEL), KEY_HEAL, MIN_HEAL, MAX_HEAL, 0.1f, healPercent, false);
        addSlider(build, table, label(KEY_PHASE_BOOST_LABEL, PHASE_BOOST_LABEL), KEY_PHASE_BOOST, MIN_PHASE_BOOST, MAX_PHASE_BOOST, 0.1f, phaseBoost, false);
        addSlider(build, table, label(KEY_PHASE_RANGE_LABEL, PHASE_RANGE_LABEL), KEY_PHASE_RANGE, MIN_PHASE_RANGE, MAX_PHASE_RANGE, 0.5f, phaseRangeBoost, false);
        table.label(this::status).left().padTop(2f).row();
    }
    @Override
    public boolean configure(DebugBlock.DebugBuild build, Object value) {
        if (!(value instanceof String command)) return false;
        String[] parts = command.split(":");
        if (parts.length != 3 || !parts[0].equals(CONFIG_PREFIX)) return false;
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
            case KEY_HEAL -> healPercent = Mathf.clamp(amount, MIN_HEAL, MAX_HEAL);
            case KEY_PHASE_BOOST -> phaseBoost = Mathf.clamp(amount, MIN_PHASE_BOOST, MAX_PHASE_BOOST);
            case KEY_PHASE_RANGE -> phaseRangeBoost = Mathf.clamp(amount, MIN_PHASE_RANGE, MAX_PHASE_RANGE);
            default -> {
                return false;
            }
        }
        return true;
    }
    @Override
    public void write(DebugBlock.DebugBuild build, Writes write) {
        write.f(range);
        write.f(reload);
        write.f(healPercent);
        write.f(phaseBoost);
        write.f(phaseRangeBoost);
        write.bool(boosted);
        write.f(heat);
        write.f(charge);
        write.f(phaseHeat);
    }
    @Override
    public void read(DebugBlock.DebugBuild build, Reads read, byte revision) {
        range = Mathf.clamp(read.f(), MIN_RANGE, MAX_RANGE);
        reload = Mathf.clamp(read.f(), MIN_RELOAD, MAX_RELOAD);
        healPercent = Mathf.clamp(read.f(), MIN_HEAL, MAX_HEAL);
        phaseBoost = Mathf.clamp(read.f(), MIN_PHASE_BOOST, MAX_PHASE_BOOST);
        phaseRangeBoost = Mathf.clamp(read.f(), MIN_PHASE_RANGE, MAX_PHASE_RANGE);
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
    }
    private Table toggleRow(DebugBlock.DebugBuild build, Runnable rebuild) {
        Table row = new Table();
        row.left();
        row.defaults().growX().height(TOGGLE_HEIGHT).pad(2f);
        row.button(label(KEY_BOOSTED_LABEL, BOOSTED_LABEL), Styles.flatTogglet,
                        () -> setBoosted(build, !boosted, rebuild))
                .checked(b -> boosted);
        return row;
    }
    private void setBoosted(DebugBlock.DebugBuild build, boolean value, Runnable rebuild) {
        if (boosted == value) return;
        boosted = value;
        build.configure(CONFIG_PREFIX + ":" + KEY_BOOSTED + ":" + value);
        if (rebuild != null) rebuild.run();
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
            case KEY_RELOAD -> reload = Mathf.clamp(value, MIN_RELOAD, MAX_RELOAD);
            case KEY_HEAL -> healPercent = Mathf.clamp(value, MIN_HEAL, MAX_HEAL);
            case KEY_PHASE_BOOST -> phaseBoost = Mathf.clamp(value, MIN_PHASE_BOOST, MAX_PHASE_BOOST);
            case KEY_PHASE_RANGE -> phaseRangeBoost = Mathf.clamp(value, MIN_PHASE_RANGE, MAX_PHASE_RANGE);
        }
        build.configure(CONFIG_PREFIX + ":" + key + ":" + (integer ? Integer.toString((int) value) : Float.toString(value)));
    }
    private String status() {
        float realRange = range + phaseHeat * phaseRangeBoost;
        float amount = healPercent + phaseHeat * phaseBoost;
        return Core.bundle.get(KEY_STATUS, STATUS) + " [accent]" + Strings.fixed(amount, 1) + "%[] / [accent]"
                + Strings.fixed(realRange, 1) + "[]";
    }
    private String format(float value, boolean integer) {
        return integer ? Integer.toString((int) value) : Strings.fixed(value, 2);
    }

    private String label(String key, String fallback) {
        return Core.bundle.get(key, fallback);
    }
}
