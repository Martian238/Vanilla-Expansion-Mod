package VanillaExpansion.expand.world.block.power;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.Align;
import arc.util.io.*;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

/**
 * 算法与 HBM 完全一致：每个结算 tick 根据柱体温度 heat 与
 * (levelLower/levelUpper/heatLower/heatUpper) 四参数 + function 插值函数，
 * 计算出目标棒位后照常移动。与手动棒不同，自动棒 getMult 直接返回 level
 * （无插棒浪涌），对应 HBM 基类 TileEntityRBMKControl.getMult()。
 */
public class RBMKControlAuto extends RBMKControl {

    /** 插值函数（对应 HBM TileEntityRBMKControlAuto.RBMKFunction） */
    public static enum RBMKFunction {
        LINEAR, QUAD_UP, QUAD_DOWN
    }

    public RBMKControlAuto(String name) {
        super(name);
        consoleType = ColumnType.CONTROL_AUTO;
        config(Long.class, (RBMKControlAutoBuild tile, Long value) -> tile.setPacked(value));
        buildType = RBMKControlAutoBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("level", (RBMKControlAutoBuild entity) -> new Bar(
            () -> "Level: " + (int) (entity.level * 100) + "%",
            () -> Pal.reactorPurple,
            () -> (float) entity.level
        ));
    }

    public class RBMKControlAutoBuild extends RBMKControlBuild {

        public RBMKFunction function = RBMKFunction.LINEAR;
        /** 阈值与对应棒位（HBM 字段名保持一致） */
        public double levelLower, levelUpper, heatLower, heatUpper;

        /**
         * HBM updateEntity：先根据温度算出目标棒位，再执行父类移动。
         * heatLower/heatUpper 与 levelLower/levelUpper 均为 HBM GUI 原始单位
         * （level 为百分数、heat 为 °C），目标棒位 = fauxLevel * 0.01。
         */
        private void computeTarget() {
            targetLevel = Mathf.clamp(eval(function, heat, levelLower, levelUpper, heatLower, heatUpper) * 0.01D, 0D, 1D);
        }

        /** 与 HBM 逐行一致的插值求值（界外取常数），返回棒位百分数 0..100 */
        static double eval(RBMKFunction function, double heat, double levelLower, double levelUpper, double heatLower, double heatUpper) {
            double fauxLevel = 0;

            double lowerBound = Math.min(heatLower, heatUpper);
            double upperBound = Math.max(heatLower, heatUpper);

            if (heat < lowerBound) {
                fauxLevel = levelLower;
            } else if (heat > upperBound) {
                fauxLevel = levelUpper;
            } else {
                switch (function) {
                    case LINEAR:
                        fauxLevel = (heat - heatLower) * ((levelUpper - levelLower) / (heatUpper - heatLower)) + levelLower;
                        break;
                    case QUAD_UP:
                        fauxLevel = Math.pow((heat - heatLower) / (heatUpper - heatLower), 2) * (levelUpper - levelLower) + levelLower;
                        break;
                    case QUAD_DOWN:
                        fauxLevel = Math.pow((heat - heatUpper) / (heatLower - heatUpper), 2) * (levelLower - levelUpper) + levelUpper;
                        break;
                }
            }

            return fauxLevel;
        }

        @Override
        public void updateTile() {
            if (!net.client()) {
                if (shouldSimulate()) computeTarget();
            }
            super.updateTile();
        }

        /** 自动棒无浪涌：直接返回当前棒位（对应 HBM 基类 getMult） */
        @Override
        public double getMult() {
            return this.level;
        }

        // ---------- 配置（5 参数打包进 Long） ----------

        /** 打包：function(2bit) + levelLower(7bit) + levelUpper(7bit) + heatLower(14bit) + heatUpper(14bit) */
        static long pack(int function, int levelLower, int levelUpper, int heatLower, int heatUpper) {
            long v = 0;
            v |= (long) (function & 3);
            v |= (long) (Mathf.clamp(levelLower, 0, 100) & 0x7F) << 2;
            v |= (long) (Mathf.clamp(levelUpper, 0, 100) & 0x7F) << 9;
            v |= (long) (Mathf.clamp(heatLower, 0, 9999) & 0x3FFF) << 16;
            v |= (long) (Mathf.clamp(heatUpper, 0, 9999) & 0x3FFF) << 30;
            return v;
        }

        public void setPacked(long v) {
            function = RBMKFunction.values()[(int) (v & 3)];
            levelLower = (v >> 2) & 0x7F;
            levelUpper = (v >> 9) & 0x7F;
            heatLower = (v >> 16) & 0x3FFF;
            heatUpper = (v >> 30) & 0x3FFF;
        }

        @Override
        public Long config() {
            return pack(function.ordinal(), (int) levelLower, (int) levelUpper, (int) heatLower, (int) heatUpper);
        }

        // ---------- 配置 UI ----------

        @Override
        public void buildConfiguration(Table table) {
            table.background(Styles.black6);
            table.top().left();

            // 顶部一行：标题 + 实时状态
            table.add("[orange]Auto Rod").left().pad(4f);
            table.add(new Label(() ->
                "Level: [accent]" + (int) (level * 100) + "%[]  [gray]Heat: " + (int) heat + "°C"
            )).right().growX().pad(4f).row();

            // 左列：函数 + 参数
            Table left = new Table();
            left.top().left();

            left.add("[orange]Function").left().pad(3f).row();
            Table funcs = new Table();
            funcs.defaults().size(84f, 34f).pad(3f);
            ButtonGroup<TextButton> fgroup = new ButtonGroup<>();
            fgroup.setMinCheckCount(1);
            fgroup.setMaxCheckCount(1);

            String[] names = {"Linear", "Quadratic", "Inv.Quad"};
            for (int i = 0; i < RBMKFunction.values().length; i++) {
                final int fi = i;
                TextButton btn = new TextButton(names[i], Styles.flatTogglet);
                btn.setChecked(function.ordinal() == fi);
                btn.update(() -> btn.setChecked(function.ordinal() == fi));
                btn.changed(() -> {
                    if (btn.isChecked()) configure(pack(fi, (int) levelLower, (int) levelUpper, (int) heatLower, (int) heatUpper));
                });
                fgroup.add(btn);
                funcs.add(btn);
            }
            left.add(funcs).center().pad(3f).row();

            // 数字输入（对应 HBM GUI 的四个文本域：levelUpper/levelLower/heatUpper/heatLower）
            TextField fLevelUpper = new TextField(String.valueOf((int) levelUpper), Styles.defaultField);
            TextField fLevelLower = new TextField(String.valueOf((int) levelLower), Styles.defaultField);
            TextField fHeatUpper = new TextField(String.valueOf((int) heatUpper), Styles.defaultField);
            TextField fHeatLower = new TextField(String.valueOf((int) heatLower), Styles.defaultField);
            fLevelUpper.setFilter(TextField.TextFieldFilter.digitsOnly);
            fLevelLower.setFilter(TextField.TextFieldFilter.digitsOnly);
            fHeatUpper.setFilter(TextField.TextFieldFilter.digitsOnly);
            fHeatLower.setFilter(TextField.TextFieldFilter.digitsOnly);
            fLevelUpper.setMaxLength(3);
            fLevelLower.setMaxLength(3);
            fHeatUpper.setMaxLength(4);
            fHeatLower.setMaxLength(4);

            numRow(left, "Level @ max", fLevelUpper);
            numRow(left, "Level @ min", fLevelLower);
            numRow(left, "Max heat", fHeatUpper);
            numRow(left, "Min heat", fHeatLower);

            TextButton save = new TextButton("Save", Styles.defaultt);
            save.clicked(() -> configure(pack(function.ordinal(),
                clampInt(fLevelLower.getText(), 100),
                clampInt(fLevelUpper.getText(), 100),
                clampInt(fHeatLower.getText(), 9999),
                clampInt(fHeatUpper.getText(), 9999))));
            left.add(save).size(100f, 34f).center().pad(3f).row();

            // 右列：温度→棒位函数图（实时读取输入框预览；紫线=曲线，橙点=当前柱体温度工作点）
            Table right = new Table();
            right.top().left();
            right.add("[orange]Temperature -> Level").left().pad(3f).row();
            right.add(buildChart(fLevelLower, fLevelUpper, fHeatLower, fHeatUpper)).size(230f, 140f).pad(3f, 7f, 3f, 3f).row();

            table.add(left);
            table.add(right);
        }

        private void numRow(Table table, String label, TextField field) {
            table.add(label).left().pad(2f);
            table.add(field).width(80f).left().pad(2f, -3f, 2f, 2f).row();
        }

        private Element buildChart(TextField fLevelLower, TextField fLevelUpper, TextField fHeatLower, TextField fHeatUpper) {
            return new Element() {
                @Override
                public void draw() {
                    float w = getWidth(), h = getHeight();

                    int ll = parseInt(fLevelLower, (int) levelLower);
                    int lu = parseInt(fLevelUpper, (int) levelUpper);
                    int hl = parseInt(fHeatLower, (int) heatLower);
                    int hu = parseInt(fHeatUpper, (int) heatUpper);

                    double hmin = Math.min(hl, hu), hmax = Math.max(hl, hu);
                    float span = (float) (hmax - hmin);
                    float xmin = (float) hmin - span * 0.12f;
                    float xmax = (float) hmax + span * 0.12f;
                    if (xmax - xmin < 1f) { xmin -= 1f; xmax += 1f; }

                    // 绘图区：左侧留 y 轴标签、底部留 x 轴标签
                    float padL = 24f, padB = 12f, padT = 4f, padR = 4f;
                    float gx = x + padL, gy = y + padB;
                    float gw = w - padL - padR, gh = h - padB - padT;

                    Draw.color(Pal.darkMetal);
                    Fill.rect(x + w / 2f, y + h / 2f, w, h);

                    // 网格
                    Draw.color(Pal.gray);
                    Lines.stroke(1f);
                    for (int i = 0; i <= 4; i++) {
                        float yy = gy + gh * i / 4f;
                        Lines.line(gx, yy, gx + gw, yy);
                    }

                    // 标签
                    Fonts.outline.setColor(Color.white);
                    Fonts.outline.draw("100", gx - 3f, gy + gh - 1f, Align.right);
                    Fonts.outline.draw("50", gx - 3f, gy + gh / 2f - 1f, Align.right);
                    Fonts.outline.draw("0", gx - 3f, gy - 1f, Align.right);
                    Fonts.outline.draw((int) hmin + "", gx, gy - 1f, Align.center);
                    Fonts.outline.draw((int) hmax + "", gx + gw, gy - 1f, Align.center);
                    if (span > 0) Fonts.outline.draw((int) ((hmin + hmax) / 2) + "", gx + gw / 2f, gy - 1f, Align.center);
                    Fonts.outline.setColor(Color.white);

                    // 曲线
                    Draw.color(Pal.reactorPurple);
                    Lines.stroke(2f);
                    int steps = 128;
                    float prevX = 0, prevY = 0;
                    for (int i = 0; i <= steps; i++) {
                        float t = i / (float) steps;
                        double heatX = xmin + (xmax - xmin) * t;
                        double levelY = eval(function, heatX, ll, lu, hl, hu);
                        float cx = gx + t * gw;
                        float cy = gy + (float) (Mathf.clamp(levelY, 0, 100) / 100.0) * gh;
                        if (i > 0) Lines.line(prevX, prevY, cx, cy);
                        prevX = cx;
                        prevY = cy;
                    }

                    // 当前工作点（真实柱体温度），x 轴 clamp 在图内、y 轴按 0-100 钳制
                    double cur = eval(function, heat, ll, lu, hl, hu);
                    float wx = Mathf.clamp(gx + (float) ((heat - xmin) / (xmax - xmin)) * gw, gx, gx + gw);
                    float wy = gy + (float) (Mathf.clamp(cur, 0, 100) / 100.0) * gh;
                    Draw.color(Pal.lightOrange);
                    Fill.circle(wx, wy, 4f);
                    Draw.color();
                }
            };
        }

        private static int clampInt(String text, int max) {
            try {
                return Mathf.clamp(Integer.parseInt(text.trim()), 0, max);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static int parseInt(TextField field, int def) {
            try {
                return Integer.parseInt(field.getText().trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }

        // ---------- 控制台 API ----------

        @Override
        public ColumnType getConsoleType() {
            return ColumnType.CONTROL_AUTO;
        }

        @Override
        public ObjectMap<String, Object> getConsoleData() {
            ObjectMap<String, Object> data = super.getConsoleData();
            data.put("function", function.ordinal());
            data.put("levelLower", levelLower);
            data.put("levelUpper", levelUpper);
            data.put("heatLower", heatLower);
            data.put("heatUpper", heatUpper);
            return data;
        }

        // ---------- 序列化 ----------

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(function.ordinal());
            write.d(levelLower);
            write.d(levelUpper);
            write.d(heatLower);
            write.d(heatUpper);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            function = RBMKFunction.values()[read.i()];
            levelLower = read.d();
            levelUpper = read.d();
            heatLower = read.d();
            heatUpper = read.d();
        }
    }
}
