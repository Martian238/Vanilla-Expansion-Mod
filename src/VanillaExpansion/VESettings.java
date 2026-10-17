package VanillaExpansion;

import VanillaExpansion.expand.abilities.BossBarAbility;
import VanillaExpansion.expand.world.block.sandbox.AutoBossBarBlock;
import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import arc.scene.actions.RunnableAction;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;

import static arc.Core.settings;
import static mindustry.Vars.ui;

//代码来源：New Horizon Mod (https://github.com/Yuria-Shikibe/NewHorizonMod/blob/main/src/newhorizon/NHSetting.java)

public class VESettings {

    public static final String
            endfieldHealthBar = "ve-endfield-health-bar",
            endfieldHealthBarScale = "ve-endfield-health-bar-scale",
            endfieldPlanetTitle = "ve-endfield-planet-title",
            endfieldBossTitle = "ve-endfield-boss-title",
            multicrafterSleep = "ve-multicrafter-sleep",
            multicrafterShowOutput = "ve-multicrafter-show-output",
            placeholder = "ve-placeholder";

    public static OrderedMap<String, Seq<SettingKey<?>>> allSettings = new OrderedMap<>();

    public static boolean changed = false;

    public static void load(){
        allSettings.put("graphics", Seq.with(
                new BoolSetting(endfieldHealthBar, true, false),
                new SliderSetting(endfieldHealthBarScale, 1, 0.2f, 1.8f, 0.1f, false),
                new BoolSetting(endfieldPlanetTitle, true, false),
                new BoolSetting(endfieldBossTitle, true, false),
                new BoolSetting(multicrafterSleep, true, false),
                new BoolSetting(multicrafterShowOutput, true, false)
        ));

        allSettings.each((name, seq) -> seq.each(SettingKey::setDefault));
    }

    public static TextureRegionDrawable buttonIcon = new TextureRegionDrawable();
    public static TextureRegion buttonIconRegion = new TextureRegion();
    public static void loadUI() {
        buttonIconRegion = Core.atlas.find("ve-team-projector-crux-glow");
        buttonIcon = new TextureRegionDrawable(buttonIconRegion);
        ui.settings.addCategory("@setting.ve", buttonIcon, VESettings::buildTable);
    }

    public static void buildTable(Table table) {
        table.pane(t -> allSettings.each((category, settings) -> {
            t.label(() -> Core.bundle.get("setting.ve-" + category)).row();
            t.image().color(Pal.accent).size(0, 4).growX().pad(4f).row();
            settings.each(settingKey -> settingKey.buildTable(t));
        })).margin(60f).get().setForceScroll(false, true);
    }

    public static void showDialog() {
        BaseDialog dialog = new BaseDialog("@setting") {
            @Override
            public void hide() {
                super.hide();
                if (changed) ui.showConfirm("@mods.reloadexit", () -> Core.app.exit());
            }
        };
        buildTable(dialog.cont);
        dialog.addCloseButton();
        dialog.show();
    }

    public static boolean getBool(String key) {
        return settings.getBool(key);
    }

    public static boolean getBool(String key, boolean def) {
        return settings.getBool(key, def);
    }

    public static float getFloat(String key) {
        return Core.settings.getFloat(key);
    }

    public static float getFloat(String key, float def) {
        return Core.settings.getFloat(key, def);
    }

    public static abstract class SettingKey<T> {
        public final String key;

        public boolean restart = false;
        public Cons<T> onChange = (T value) -> {
        };
        public T def;

        public SettingKey(String key) {
            this.key = key;
        }

        public void setDefault() {
            if (!settings.has(key)) settings.put(key, def);
        }

        public SettingKey<T> onChange(Cons<T> onChange) {
            this.onChange = onChange;
            return this;
        }

        public abstract T getValue();

        public abstract void buildTable(Table table);
    }

    public static class BoolSetting extends SettingKey<Boolean> {
        public BoolSetting(String key, boolean def, boolean restart) {
            super(key);
            this.def = def;
            this.restart = restart;
        }

        @Override
        public Boolean getValue() {
            return settings.getBool(key);
        }

        @Override
        public void buildTable(Table table) {
            table.table(t -> {
                t.add(new CheckBox(Core.bundle.get("setting." + key + ".name")) {{
                    changed(() -> {
                        settings.put(key, isChecked());
                        onChange.get(isChecked());
                        if (restart) {
                            if (!changed) {
                                Dialog.setHideAction(() -> new RunnableAction() {{
                                    setRunnable(() -> ui.showConfirm("@mods.reloadexit", () -> Core.app.exit()));
                                }});
                            }
                            changed = true;
                        }
                    });
                    update(() -> setChecked(settings.getBool(key)));
                }}).padRight(6f).left();
                t.row().table(i -> {
                    i.left();
                    i.defaults().left();
                    i.add("[lightgray]" + Core.bundle.get("setting." + key + ".description") + "[]").padLeft(6f).wrap()
                            .width(Math.min(Core.graphics.getWidth() / 1.2f, 420f)).row();
                }).growX();
            }).growX().fillY().margin(8f).left().width(Math.min(Core.graphics.getWidth() / 1.2f, 460f)).row();
        }
    }

    public static class SliderSetting extends SettingKey<Float> {
        public float min = 0f, max = 1f, step = 0.01f;

        public SliderSetting(String key, float def, float min, float max, float step, boolean restart) {
            super(key);
            this.def = def;
            this.min = min;
            this.max = max;
            this.step = step;

            this.restart = restart;
        }

        public SliderSetting(String key, float def, boolean restart) {
            super(key);
            this.def = def;
            this.restart = restart;
        }

        @Override
        public Float getValue() {
            return Core.settings.getFloat(key);
        }

        @Override
        public void buildTable(Table table) {
            table.table(t -> {
                t.add(new Label(() -> Core.bundle.format("setting." + key + ".name", getValue()))).padRight(6f).left();
                t.row().slider(min, max, step, def, f -> {
                    settings.put(key, f);
                    onChange.get(f);
                }).growX();
                t.row().table(i -> {
                    i.left();
                    i.defaults().left();
                    i.add("[lightgray]" + Core.bundle.get("setting." + key + ".description") + "[]").padLeft(6f).wrap()
                            .width(Math.min(Core.graphics.getWidth() / 1.2f, 420f)).row();
                }).growX();
            }).growX().fillY().margin(8f).left().width(Math.min(Core.graphics.getWidth() / 1.2f, 460f)).row();
        }
    }
}
