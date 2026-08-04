package VanillaExpansion.expand.world.block.power;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.graphics.*;
import mindustry.ui.Bar;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

/**
 * <p>
 * 无耗电（区别于 HBM 电动棒的 5000 HE/t）。控制棒状态直接以两条竖向进度条绘制在
 * 方块表面（左=当前棒位，右=目标棒位），不占用地图上方空间、不遮挡视野。
 * 中子流穿越时按  缩放通量，level==0 时完全阻断。
 * 插棒方向（target &lt; level）触发 HBM 浪涌效应：瞬时通量尖峰（AZ-5 结局）。
 */
public class RBMKControl extends RBMKBase {

    /** 是否被石墨慢化（熔毁碎片类型 + isModerated 语义，对应 HBM rbmk_control_mod） */
    public boolean moderated = false;

    /** 分组颜色（对应 HBM TileEntityRBMKControlManual.RBMKColor，控制台按色批量设定） */
    public static enum RBMKColor {
        RED, YELLOW, GREEN, BLUE, PURPLE;

        public static RBMKColor get(int i) {
            if (i < 0 || i >= values().length) return null;
            return values()[i];
        }
    }

    public RBMKControl(String name) {
        super(name);
        consoleType = ColumnType.CONTROL;
        configurable = true;
        config(Integer.class, (RBMKControlBuild tile, Integer value) -> tile.setPacked(value));
        configClear((RBMKControlBuild tile) -> tile.setTarget(0));
        buildType = RBMKControlBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("level", (RBMKControlBuild entity) -> new Bar(
            () -> "Level: " + (int) (entity.level * 100) + "%",
            () -> Pal.reactorPurple,
            () -> (float) entity.level
        ));
    }

    public class RBMKControlBuild extends RBMKBaseBuild {

        /** 上一结算的棒位（用于 2D 渲染平滑 / 浪涌快照基准） */
        public double lastLevel;
        /** 当前棒位 [0;1]，0=全插 1=全出 */
        public double level;
        /** 移动速度（HBM 原值：约 18 秒全行程） */
        public static final double speed = 0.00277D;
        /** 目标棒位 */
        public double targetLevel;
        /** 浪涌快照：本次 setTarget 开始时的棒位（对应 HBM startingLevel） */
        public double startingLevel;
        /** 分组颜色：-1=未分组，0-4=RBMKColor */
        public int color = -1;

        @Override
        public void updateTile() {
            if (!net.client()) {
                if (shouldSimulate()) {
                    lastLevel = level;

                    if (level < targetLevel) {
                        level += speed * RBMKDials.controlSpeed;
                        if (level > targetLevel) level = targetLevel;
                    }
                    if (level > targetLevel) {
                        level -= speed * RBMKDials.controlSpeed;
                        if (level < targetLevel) level = targetLevel;
                    }
                }
            }
            super.updateTile();
        }

        /** 设定目标棒位并记录浪涌快照（对应 HBM setTarget） */
        public void setTarget(double target) {
            this.targetLevel = Mathf.clamp(target, 0D, 1D);
            this.startingLevel = this.level;
        }

        /** 中子穿越倍率（对应 HBM getMult：level + 插棒浪涌尖峰） */
        public double getMult() {
            double surge = 0;

            if (this.targetLevel < this.startingLevel && Math.abs(this.level - this.targetLevel) > 0.01D) {
                surge = Math.sin(Math.pow((1D - this.level), 15) * Math.PI)
                    * (this.startingLevel - this.targetLevel) * RBMKDials.surgeMod;
            }

            return this.level + surge;
        }

        // ---------- 配置 UI（分组 + 目标棒位） ----------

        static int pack(int color, int percent) {
            return (color + 1) * 1000 + percent;
        }

        static int unpackColor(int v) {
            return v / 1000 - 1;
        }

        static int unpackPercent(int v) {
            return v % 1000;
        }

        public void setPacked(int v) {
            setTarget(unpackPercent(v) / 100D);
            color = unpackColor(v);
        }

        @Override
        public Object config() {
            return pack(color, (int) (targetLevel * 100));
        }

        @Override
        public void buildConfiguration(Table table) {
            table.background(Styles.black6);
            table.top().left();
            table.add("[orange]Control Rod").growX().center().pad(6f).row();

            table.label(() ->
                "Level: [accent]" + (int) (level * 100) + "%[]  [gray]Target: " + (int) (targetLevel * 100) + "%"
            ).left().pad(4f).row();

            Slider slider = new Slider(0f, 100f, 1f, false);
            slider.setValue((float) (targetLevel * 100));
            slider.moved(value -> configure(pack(color, (int) value)));
            table.add(slider).width(240f).pad(4f).row();

            table.add("[orange]Group").left().pad(4f).row();
            Table buttons = new Table();
            buttons.defaults().size(64f, 44f).pad(3f);
            ButtonGroup<TextButton> group = new ButtonGroup<>();
            group.setMinCheckCount(0);

            TextButton none = new TextButton("None", Styles.flatTogglet);
            none.setChecked(color == -1);
            none.update(() -> none.setChecked(color == -1));
            none.changed(() -> {
                if (none.isChecked()) configure(pack(-1, (int) (targetLevel * 100)));
            });
            group.add(none);
            buttons.add(none);

            for (int i = 0; i < RBMKColor.values().length; i++) {
                final int col = i;
                TextButton btn = new TextButton(RBMKColor.values()[i].name(), Styles.flatTogglet);
                btn.setChecked(color == col);
                btn.update(() -> btn.setChecked(color == col));
                btn.changed(() -> {
                    if (btn.isChecked()) configure(pack(col, (int) (targetLevel * 100)));
                });
                group.add(btn);
                buttons.add(btn);
            }
            table.add(buttons).center().pad(4f).row();
        }

        // ---------- 渲染（方块表面绘制两条竖向进度条：左=当前棒位，右=目标棒位） ----------

        @Override
        public void draw() {
            super.draw();

            // 进度条几何：两根竖条并排绘制在方块中心两侧。
            // 按 1 格 = 8px 的比例，2×2 方块视为 16×16px，竖条收进中心 ±8px 内。
            float barW = 3f;
            float barH = 11f;
            float barOffset = 4f;

            // 底槽（灰色）：以方块中心 y 为轴心，高度不超过 ±8px
            Draw.color(Pal.gray);
            Fill.rect(x - barOffset, y, barW, barH);
            Fill.rect(x + barOffset, y, barW, barH);

            // 当前棒位：紫色（底边与底槽底边对齐，向上生长；最小长度 0 无偏移）
            float levelH = barH * (float) level;
            Draw.color(Pal.reactorPurple);
            Fill.rect(x - barOffset, y - barH / 2f + levelH / 2f, barW, levelH);

            // 目标棒位：亮橙
            float target = barH * (float) targetLevel;
            Draw.color(Pal.lightOrange);
            Fill.rect(x + barOffset, y - barH / 2f + target / 2f, barW, target);

            Draw.color();
        }

        // ---------- 控制台 API ----------

        @Override
        public RBMKType getRBMKType() {
            return RBMKType.CONTROL_ROD;
        }

        @Override
        public ColumnType getConsoleType() {
            return ColumnType.CONTROL;
        }

        @Override
        public boolean isModerated() {
            return RBMKControl.this.moderated;
        }

        @Override
        public ObjectMap<String, Object> getConsoleData() {
            ObjectMap<String, Object> data = super.getConsoleData();
            if (data == null) data = new ObjectMap<>();
            data.put("level", level);
            data.put("targetLevel", targetLevel);
            data.put("color", color);
            return data;
        }

        @Override
        public double consoleValue(RBMKBase.ScreenValue screen) {
            if (screen == RBMKBase.ScreenValue.ROD_EXTRACTION) return level * 100D;
            return super.consoleValue(screen);
        }

        // ---------- 熔毁（对应 HBM onMelt） ----------

        @Override
        public void onMelt(int reduce) {
            if (this.isModerated()) {
                int mCount = 2 + Mathf.random(1);
                for (int j = 0; j < mCount; j++) spawnDebris(DebrisType.GRAPHITE);
            }

            int count = 2 + Mathf.random(1);
            for (int i = 0; i < count; i++) spawnDebris(DebrisType.ROD);

            this.standardMelt(reduce);
        }

        // ---------- 序列化 ----------

        @Override
        public void write(Writes write) {
            super.write(write);
            write.d(level);
            write.d(targetLevel);
            write.d(startingLevel);
            write.i(color);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            level = read.d();
            targetLevel = read.d();
            startingLevel = read.d();
            color = read.i();
        }
    }
}