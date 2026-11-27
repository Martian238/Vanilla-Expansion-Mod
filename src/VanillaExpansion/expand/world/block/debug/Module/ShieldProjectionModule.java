package VanillaExpansion.expand.world.block.debug.Module;
import VanillaExpansion.expand.world.block.debug.DebugBlock;
import VanillaExpansion.expand.world.block.debug.DebugModule;
import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
public class ShieldProjectionModule implements DebugModule {
    public static final String ID = "shield-projection";
    public static final String CONFIG_PREFIX = "shield";
    public static final String KEY_MODE = "mode";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_SIDES = "sides";
    public static final String KEY_HEALTH = "health";
    public static final String KEY_REGEN = "regen";
    public static final String KEY_BROKEN_REGEN = "brokenregen";
    public static final String NAME_KEY = "debug.module.shield.name";
    public static final String NAME_DEFAULT = "护盾投影 / Shield Projection";
    public static final String KEY_RADIUS_LABEL = "debug.module.shield.radius";
    public static final String RADIUS_LABEL = "半径 / Radius";
    public static final String KEY_SIDES_LABEL = "debug.module.shield.sides";
    public static final String SIDES_LABEL = "边数 / Sides";
    public static final String KEY_HEALTH_LABEL = "debug.module.shield.health";
    public static final String HEALTH_LABEL = "护盾值 / Shield Health";
    public static final String KEY_REGEN_LABEL = "debug.module.shield.regen";
    public static final String REGEN_LABEL = "恢复速度 / Regen";
    public static final String KEY_BROKEN_REGEN_LABEL = "debug.module.shield.brokenRegen";
    public static final String BROKEN_REGEN_LABEL = "破碎恢复 / Broken Regen";
    public static final String KEY_STATUS_BASE = "debug.module.shield.status.base";
    public static final String STATUS_BASE = "[lightgray]BaseShield：护盾常开[]";
    public static final String KEY_STATUS_FORCE = "debug.module.shield.status.force";
    public static final String STATUS_FORCE = "[lightgray]ForceProjector：护盾[]";
    public static final String MODE_BASE_KEY = "debug.module.shield.mode.base";
    public static final String MODE_BASE = "BaseShield";
    public static final String MODE_FORCE_KEY = "debug.module.shield.mode.force";
    public static final String MODE_FORCE = "ForceProjector";
    public static final float MIN_RADIUS = 1f, MAX_RADIUS = 40f;
    public static final int MIN_SIDES = 3, MAX_SIDES = 32;
    public static final float MIN_HEALTH = 50f, MAX_HEALTH = 50000f;
    public static final float MIN_REGEN = 0f, MAX_REGEN = 20f;
    public static final float MIN_BROKEN_REGEN = 0f, MAX_BROKEN_REGEN = 20f;
    public static final float MODE_BUTTON_HEIGHT = 34f;
    public enum ShieldMode {
        base, force
    }
    public ShieldMode mode = ShieldMode.force;
    public float radius = 4f;
    public int sides = 6;
    public float shieldHealth = 700f;
    public float cooldownNormal = 1.75f;
    public float cooldownBrokenBase = 0.35f;
    public boolean broken = false;
    public float buildup = 0f;
    public float hit = 0f;
    public float smoothRadius = 0f;
    public float radscl = 0f;
    public float shieldRotation = 0f;
    private DebugBlock.DebugBuild activeBuild;
    private final Cons<Bullet> baseBulletConsumer = this::absorbBaseBullet;
    private final Cons<Unit> baseUnitConsumer = this::repelBaseUnit;
    private final Cons<Bullet> forceBulletConsumer = this::absorbForceBullet;
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
        broken = false;
        buildup = 0f;
        hit = 0f;
        smoothRadius = 0f;
        radscl = 0f;
    }
    @Override
    public void detach(DebugBlock.DebugBuild build) {
        activeBuild = null;
    }
    @Override
    public void update(DebugBlock.DebugBuild build) {
        activeBuild = build;
        if (mode == ShieldMode.base) {
            updateBase(build);
        } else {
            updateForce(build);
        }
    }
    @Override
    public void draw(DebugBlock.DebugBuild build) {
        float rad = mode == ShieldMode.base ? smoothRadius : realRadius();
        if (rad <= 0.001f || (mode == ShieldMode.force && broken)) return;
        Draw.z(Layer.shields);
        Draw.color(build.team.color, Color.white, Mathf.clamp(hit));
        if (Vars.renderer.animateShields) {
            Fill.poly(build.x, build.y, sides, rad, shieldRotation);
        } else {
            Lines.stroke(1.5f);
            Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
            Fill.poly(build.x, build.y, sides, rad, shieldRotation);
            Draw.alpha(1f);
            Lines.poly(build.x, build.y, sides, rad, shieldRotation);
        }
        Draw.reset();
    }
    @Override
    public void drawSelect(DebugBlock.DebugBuild build) {
        float rad = radius * Vars.tilesize;
        Color color = build.team.color;
        Drawf.dashCircle(build.x, build.y, rad, color);
        Drawf.text(displayName(), build.x, build.y + rad + 6f, color, 1f);
    }
    @Override
    public void buildConfig(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {
        table.add(modeRow(build, rebuild)).growX().padBottom(4f).row();
        addSlider(build, table, label(KEY_RADIUS_LABEL, RADIUS_LABEL), KEY_RADIUS, MIN_RADIUS, MAX_RADIUS, 0.5f, radius, false);
        addSlider(build, table, label(KEY_SIDES_LABEL, SIDES_LABEL), KEY_SIDES, MIN_SIDES, MAX_SIDES, 1f, sides, true);
        if (mode == ShieldMode.force) {
            addSlider(build, table, label(KEY_HEALTH_LABEL, HEALTH_LABEL), KEY_HEALTH, MIN_HEALTH, MAX_HEALTH, 50f, shieldHealth, true);
            addSlider(build, table, label(KEY_REGEN_LABEL, REGEN_LABEL), KEY_REGEN, MIN_REGEN, MAX_REGEN, 0.05f, cooldownNormal, false);
            addSlider(build, table, label(KEY_BROKEN_REGEN_LABEL, BROKEN_REGEN_LABEL), KEY_BROKEN_REGEN, MIN_BROKEN_REGEN, MAX_BROKEN_REGEN, 0.05f, cooldownBrokenBase, false);
        }
        table.label(this::status).left().padTop(2f).row();
    }
    @Override
    public boolean buildBars(DebugBlock.DebugBuild build, Table table) {
        if (mode == ShieldMode.base) return false;
        table.add(new Bar("stat.shieldhealth", Pal.accent,
                        () -> broken ? 0f : 1f - buildup / shieldHealth)
                        .blink(Color.white))
                .growX().height(18f).row();
        return true;
    }
    @Override
    public boolean configure(DebugBlock.DebugBuild build, Object value) {
        if (!(value instanceof String command)) return false;
        String[] parts = command.split(":");
        if (parts.length != 3 || !parts[0].equals(CONFIG_PREFIX)) return false;
        if (parts[1].equals(KEY_MODE)) {
            mode = parts[2].equals(ShieldMode.force.name()) ? ShieldMode.force : ShieldMode.base;
            broken = false;
            buildup = 0f;
            smoothRadius = 0f;
            radscl = 0f;
            return true;
        }
        float amount;
        try {
            amount = Float.parseFloat(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        switch (parts[1]) {
            case KEY_RADIUS -> radius = Mathf.clamp(amount, MIN_RADIUS, MAX_RADIUS);
            case KEY_SIDES -> sides = Mathf.clamp((int) amount, MIN_SIDES, MAX_SIDES);
            case KEY_HEALTH -> shieldHealth = Mathf.clamp(amount, MIN_HEALTH, MAX_HEALTH);
            case KEY_REGEN -> cooldownNormal = Mathf.clamp(amount, MIN_REGEN, MAX_REGEN);
            case KEY_BROKEN_REGEN -> cooldownBrokenBase = Mathf.clamp(amount, MIN_BROKEN_REGEN, MAX_BROKEN_REGEN);
            default -> {
                return false;
            }
        }
        return true;
    }
    @Override
    public void write(DebugBlock.DebugBuild build, Writes write) {
        write.bool(mode == ShieldMode.force);
        write.f(radius);
        write.i(sides);
        write.f(shieldHealth);
        write.f(cooldownNormal);
        write.f(cooldownBrokenBase);
        write.bool(broken);
        write.f(buildup);
        write.f(hit);
    }
    @Override
    public void read(DebugBlock.DebugBuild build, Reads read, byte revision) {
        mode = read.bool() ? ShieldMode.force : ShieldMode.base;
        radius = Mathf.clamp(read.f(), MIN_RADIUS, MAX_RADIUS);
        sides = Mathf.clamp(read.i(), MIN_SIDES, MAX_SIDES);
        shieldHealth = Mathf.clamp(read.f(), MIN_HEALTH, MAX_HEALTH);
        cooldownNormal = Mathf.clamp(read.f(), MIN_REGEN, MAX_REGEN);
        cooldownBrokenBase = Mathf.clamp(read.f(), MIN_BROKEN_REGEN, MAX_BROKEN_REGEN);
        if (revision >= 2) {
            broken = read.bool();
            buildup = Mathf.clamp(read.f(), 0f, shieldHealth);
            hit = read.f();
        } else {
            broken = false;
            buildup = 0f;
            hit = 0f;
        }
        smoothRadius = 0f;
        radscl = 0f;
    }
    private void updateBase(DebugBlock.DebugBuild build) {
        smoothRadius = Mathf.lerpDelta(smoothRadius, radius * Vars.tilesize, 0.05f);
        float rad = smoothRadius;
        if (rad <= 1f) return;
        Groups.bullet.intersect(build.x - rad, build.y - rad, rad * 2f, rad * 2f, baseBulletConsumer);
        Units.nearbyEnemies(build.team, build.x, build.y, rad + 10f, baseUnitConsumer);
    }
    private void updateForce(DebugBlock.DebugBuild build) {
        radscl = Mathf.lerpDelta(radscl, broken ? 0f : 1f, 0.05f);
        if (buildup > 0f) {
            float scale = !broken ? cooldownNormal : cooldownBrokenBase;
            buildup -= Time.delta * scale;
        }
        if (broken && buildup <= 0f) {
            broken = false;
            buildup = 0f;
        }
        if (hit > 0f) hit -= 1f / 5f * Time.delta;
        if (!broken) {
            float rad = realRadius();
            if (rad > 0.001f) {
                Groups.bullet.intersect(build.x - rad, build.y - rad, rad * 2f, rad * 2f, forceBulletConsumer);
            }
        }
        if (!broken && buildup >= shieldHealth) {
            broken = true;
            buildup = shieldHealth;
        }
    }
    private void absorbBaseBullet(Bullet bullet) {
        if (activeBuild == null) return;
        if (bullet.team != activeBuild.team && bullet.type.absorbable && !bullet.absorbed &&
                Mathf.dst2(bullet.x, bullet.y, activeBuild.x, activeBuild.y) <= smoothRadius * smoothRadius) {
            bullet.absorb();
        }
    }
    private void repelBaseUnit(Unit unit) {
        if (activeBuild == null) return;
        float overlapDst = (unit.hitSize / 2f + smoothRadius) - unit.dst(activeBuild);
        if (overlapDst <= 0f) return;
        if (overlapDst > unit.hitSize * 1.5f) {
            unit.kill();
        } else {
            unit.vel.setZero();
            unit.move(Tmp.v1.set(unit).sub(activeBuild).setLength(overlapDst + 0.01f));
        }
    }
    private void absorbForceBullet(Bullet bullet) {
        if (activeBuild == null) return;
        if (bullet.team != activeBuild.team && bullet.type.absorbable && !bullet.absorbed &&
                Intersector.isInRegularPolygon(sides, activeBuild.x, activeBuild.y, realRadius(), shieldRotation, bullet.x, bullet.y)) {
            bullet.absorb();
            hit = 1f;
            buildup += bullet.type.shieldDamage(bullet);
        }
    }
    public float realRadius() {
        return radius * Vars.tilesize * radscl;
    }
    private void setMode(DebugBlock.DebugBuild build, ShieldMode value, Runnable rebuild) {
        if (mode == value) return;
        mode = value;
        broken = false;
        buildup = 0f;
        hit = 0f;
        smoothRadius = 0f;
        radscl = 0f;
        build.configure(CONFIG_PREFIX + ":" + KEY_MODE + ":" + value.name());
        if (rebuild != null) rebuild.run();
    }
    private Table modeRow(DebugBlock.DebugBuild build, Runnable rebuild) {
        Table row = new Table();
        row.left();
        row.defaults().growX().height(MODE_BUTTON_HEIGHT).pad(2f);
        row.button(label(MODE_BASE_KEY, MODE_BASE), Styles.flatTogglet,
                        () -> setMode(build, ShieldMode.base, rebuild))
                .checked(b -> mode == ShieldMode.base);
        row.button(label(MODE_FORCE_KEY, MODE_FORCE), Styles.flatTogglet,
                        () -> setMode(build, ShieldMode.force, rebuild))
                .checked(b -> mode == ShieldMode.force);
        return row;
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
            case KEY_RADIUS -> radius = Mathf.clamp(value, MIN_RADIUS, MAX_RADIUS);
            case KEY_SIDES -> sides = Mathf.clamp((int) value, MIN_SIDES, MAX_SIDES);
            case KEY_HEALTH -> shieldHealth = Mathf.clamp(value, MIN_HEALTH, MAX_HEALTH);
            case KEY_REGEN -> cooldownNormal = Mathf.clamp(value, MIN_REGEN, MAX_REGEN);
            case KEY_BROKEN_REGEN -> cooldownBrokenBase = Mathf.clamp(value, MIN_BROKEN_REGEN, MAX_BROKEN_REGEN);
        }
        build.configure(CONFIG_PREFIX + ":" + key + ":" + (integer ? Integer.toString((int) value) : Float.toString(value)));
    }
    private String status() {
        if (mode == ShieldMode.base) return Core.bundle.get(KEY_STATUS_BASE, STATUS_BASE);
        int current = Math.max(0, (int) (shieldHealth - buildup));
        return Core.bundle.get(KEY_STATUS_FORCE, STATUS_FORCE) + " [accent]" + current + "[] / " + (int) shieldHealth;
    }
    private String format(float value, boolean integer) {
        return integer ? Integer.toString((int) value) : Strings.fixed(value, 2);
    }
    private String label(String key, String fallback) {
        return Core.bundle.get(key, fallback);
    }
}
