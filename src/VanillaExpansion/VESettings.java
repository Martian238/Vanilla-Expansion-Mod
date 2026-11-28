package VanillaExpansion;

import VanillaExpansion.expand.ui.VESettingsDialog;
import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;

import static arc.Core.settings;

public class VESettings {
    public static final String mobileAutoWarp = "ve-mobile-auto-warp"; // 移动端自动跃迁
    public static final String skipCutscene = "ve-skip-cutscene"; // 跳过过场动画
    public static final String endfieldHealthBar = "ve-endfield-health-bar"; // 终末地风格血条
    public static final String endfieldHealthBarScale = "ve-endfield-health-bar-scale"; // 终末地风格血条尺寸
    public static final String endfieldPlanetTitle = "ve-endfield-planet-title"; // 星球大标题
    public static final String endfieldBossTitle = "ve-endfield-boss-title"; // Boss标题
    public static final String multicrafterSleep = "ve-multicrafter-sleep"; // 多配方工厂ZZZ
    public static final String multicrafterShowOutput = "ve-multicrafter-show-output"; // 多配方工厂产物预览
    public static final String propedroneQuiet = "ve-propedrone-quiet"; // 无人机螺旋桨
    public static final String coreUnitLight = "ve-core-unit-light"; // 核心单位探照灯
    public static final Seq<Category> categories = new Seq<>();
    public static boolean changed = false;
    public static boolean fcf = false;
    public static TextureRegionDrawable buttonIcon = new TextureRegionDrawable();
    public static TextureRegion buttonIconRegion = new TextureRegion();

    public static void load() {
        categories.clear();
        category("mechanism")//机制分支下的控件
                .bool(mobileAutoWarp, true)
                .bool(skipCutscene, false);
        category("graphics")//视效分支下的控件
                .bool(endfieldHealthBar, true)
                .slider(endfieldHealthBarScale, 1f, 0.2f, 1.8f, 0.1f)
                .bool(endfieldPlanetTitle, true)
                .bool(endfieldBossTitle, true)
                .bool(multicrafterSleep, true)
                .bool(multicrafterShowOutput, true)
                .bool(propedroneQuiet, false)
                .bool(coreUnitLight, true);

        for (Category category : categories) {
            for (Setting setting : category.settings) {
                setting.setDefault();
            }
        }
    }

    public static Category category(String name) {
        Category category = new Category(name);
        categories.add(category);
        return category;
    }

    public static void loadUI() {
        buttonIconRegion = Core.atlas.find("ve-team-projector-crux-glow");
        buttonIcon = new TextureRegionDrawable(buttonIconRegion);

        Core.app.post(() -> {
            if (Vars.ui.menufrag != null) {
                String text = Core.bundle.get("setting.ve");
                Vars.ui.menufrag.addButton(new mindustry.ui.fragments.MenuFragment.MenuButton(
                        text,
                        new ScaledDrawable(buttonIcon, 40f, 40f, -5f),//最后一个参数是偏移量，负为左，正为右
                        VESettings::show
                ));
            }
            Vars.ui.settings.addCategory("@setting.ve", buttonIcon, table -> table.update(() -> {
                if (table.visible && table.getScene() != null) {
                    if (fcf) return;
                    Vars.ui.settings.hide();
                    new VESettingsDialog().show();
                    fcf = true;
                }
            }));
        });
    }
    //艾滋贼UI整个那么大个icon干什么，又要写自适应
    public static class ScaledDrawable extends arc.scene.style.BaseDrawable {
        private final arc.scene.style.Drawable original; // 原始图标
        private final float maxWidth; // 最大宽度
        private final float maxHeight; // 最大高度
        private final float offsetRight; // 偏移像素，负为左，正为右

        public ScaledDrawable(arc.scene.style.Drawable original, float maxWidth, float maxHeight, float offsetRight) {
            this.original = original;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.offsetRight = offsetRight;
        }

        @Override
        public void draw(float x, float y, float width, float height) {
            float imgW = original.getMinWidth();
            float imgH = original.getMinHeight();
            if (imgW <= 0 || imgH <= 0) {
                original.draw(x, y, width, height);
                return;
            }
            float scale = Math.min(maxWidth / imgW, maxHeight / imgH);
            float drawW = imgW * scale;
            float drawH = imgH * scale;
            float offsetX = (width - drawW) / 2f + offsetRight; // 偏移
            float offsetY = (height - drawH) / 2f;
            original.draw(x + offsetX, y + offsetY, drawW, drawH);
        }

        @Override
        public float getMinWidth() {
            return maxWidth;
        }

        @Override
        public float getMinHeight() {
            return maxHeight;
        }
    }

    public static void show() {
        new VESettingsDialog().show();
    }

    public static boolean getBool(String key) {
        return settings.getBool(key);
    }

    public static boolean getBool(String key, boolean def) {
        return settings.getBool(key, def);
    }

    public enum SettingType {
        bool,
        slider
    }

    public static class Category {
        public final String name; // 分类名称的语言键后缀
        public final Seq<Setting> settings = new Seq<>(); // 分类下的全部设置项

        public Category(String name) {
            this.name = name;
        }

        public Category bool(String key, boolean def) {
            settings.add(new Setting(key, SettingType.bool, def, 0f, 0f, 0f, 0f));
            return this;
        }

        public Category bool(String key, boolean def, boolean restart) {
            settings.add(new Setting(key, SettingType.bool, def, 0f, 0f, 0f, 0f));
            settings.peek().restart = restart;
            return this;
        }

        public Category slider(String key, float def, float min, float max, float step) {
            settings.add(new Setting(key, SettingType.slider, false, def, min, max, step));
            return this;
        }
    }

    public static class Setting {
        public final String key; // 设置项的语言键
        public final SettingType type; // 设置项的控件类型
        public final boolean boolDef; // 布尔设置项的默认值
        public final float floatDef; // 滑块设置项的默认值
        public final float min; // 滑块最小值
        public final float max; // 滑块最大值
        public final float step; // 滑块步长
        public boolean restart; // 是否重启后生效

        public Setting(String key, SettingType type, boolean boolDef, float floatDef, float min, float max, float step) {
            this.key = key;
            this.type = type;
            this.boolDef = boolDef;
            this.floatDef = floatDef;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public void setDefault() {
            if (type == SettingType.bool) {
                if (!settings.has(key)) settings.put(key, boolDef);
            } else if (!settings.has(key)) {
                settings.put(key, floatDef);
            }
        }
    }
}