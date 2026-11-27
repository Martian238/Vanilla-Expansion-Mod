package VanillaExpansion.expand.world.block.power;

import arc.Core;
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

public class RBMKControl extends RBMKBase {
    public boolean moderated = false;
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
            () -> Core.bundle.format("rbmk.bar.level", (int) (entity.level * 100)),
            () -> Pal.reactorPurple,
            () -> (float) entity.level
        ));
    }

    public class RBMKControlBuild extends RBMKBaseBuild {
        public double lastLevel;
        public double level;
        public static final double speed = 0.00277D;
        public double targetLevel;
        public double startingLevel;
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
        public void setTarget(double target) {
            this.targetLevel = Mathf.clamp(target, 0D, 1D);
            this.startingLevel = this.level;
        }
        public double getMult() {
            double surge = 0;

            if (this.targetLevel < this.startingLevel && Math.abs(this.level - this.targetLevel) > 0.01D) {
                surge = Math.sin(Math.pow((1D - this.level), 15) * Math.PI)
                    * (this.startingLevel - this.targetLevel) * RBMKDials.surgeMod;
            }

            return this.level + surge;
        }
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
            table.add(Core.bundle.get("rbmk.ui.control.title")).growX().center().pad(6f).row();

            table.label(() ->
                Core.bundle.format("rbmk.ui.control.level", (int) (level * 100), (int) (targetLevel * 100))
            ).left().pad(4f).row();

            Slider slider = new Slider(0f, 100f, 1f, false);
            slider.setValue((float) (targetLevel * 100));
            slider.moved(value -> configure(pack(color, (int) value)));
            table.add(slider).width(240f).pad(4f).row();

            table.add(Core.bundle.get("rbmk.ui.control.group")).left().pad(4f).row();
            Table buttons = new Table();
            buttons.defaults().size(64f, 44f).pad(3f);
            ButtonGroup<TextButton> group = new ButtonGroup<>();
            group.setMinCheckCount(0);

            TextButton none = new TextButton(Core.bundle.get("rbmk.ui.control.none"), Styles.flatTogglet);
            none.setChecked(color == -1);
            none.update(() -> none.setChecked(color == -1));
            none.changed(() -> {
                if (none.isChecked()) configure(pack(-1, (int) (targetLevel * 100)));
            });
            group.add(none);
            buttons.add(none);

            for (int i = 0; i < RBMKColor.values().length; i++) {
                final int col = i;
                TextButton btn = new TextButton(Core.bundle.get("rbmk.group." + RBMKColor.values()[i].name().toLowerCase()), Styles.flatTogglet);
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
        @Override
        public void draw() {
            super.draw();
            float barW = 3f;
            float barH = 11f;
            float barOffset = 4f;
            Draw.color(Pal.gray);
            Fill.rect(x - barOffset, y, barW, barH);
            Fill.rect(x + barOffset, y, barW, barH);
            float levelH = barH * (float) level;
            Draw.color(Pal.reactorPurple);
            Fill.rect(x - barOffset, y - barH / 2f + levelH / 2f, barW, levelH);
            float target = barH * (float) targetLevel;
            Draw.color(Pal.lightOrange);
            Fill.rect(x + barOffset, y - barH / 2f + target / 2f, barW, target);

            Draw.color();
        }

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