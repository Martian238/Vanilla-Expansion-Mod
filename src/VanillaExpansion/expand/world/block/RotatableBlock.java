package VanillaExpansion.expand.world.block;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

/**
 * 旋转方块基类
 * 作为一个简单的RotBlock，支持自动转向目标角度
 * 可通过UI调整方向
 */
public class RotatableBlock extends Block {

    /** 转向速度 */
    public float rotateSpeed = 5f;

    /** 是否启用自动转向 */
    public boolean autoRotate = true;

    /** 是否显示UI配置 */
    public boolean showConfigUI = true;

    /** 方向箭头图标 */
    public TextureRegion rotateArrowRegion;

    public RotatableBlock(String name) {
        super(name);
        update = true;
        solid = true;
        rotate = true;
        sync = true;
        configurable = true;
        saveConfig = true;

        config(Float.class, (RotatableBuild build, Float angle) -> {
            build.setTargetRotation(angle);
        });
    }

    @Override
    public void load() {
        super.load();
        rotateArrowRegion = Core.atlas.find("rotate-arrow");
    }

    @Override
    public void setStats() {
        super.setStats();
    }

    public class RotatableBuild extends Building implements RotBlock {
        /** 当前旋转角度（度数） */
        public float rotation = 90f;

        /** 目标旋转角度（度数） */
        protected float targetRotation = 90f;

        @Override
        public void placed() {
            super.placed();
            // 初始化旋转角度，配置会在之后被应用
            if (rotation == 0f && targetRotation == 0f) {
                rotation = 90f;
                targetRotation = 90f;
            }
        }

        @Override
        public void updateTile() {
            super.updateTile();

            if (autoRotate && !Mathf.equal(rotation, targetRotation, 0.001f)) {
                turnToTarget(targetRotation);
            }
        }

        protected void turnToTarget(float targetRot) {
            rotation = Angles.moveToward(rotation, targetRot, rotateSpeed * Time.delta);
        }

        public void setTargetRotation(float target) {
            this.targetRotation = Mathf.mod(target, 360f);
        }

        public float getTargetRotation() {
            return targetRotation;
        }

        @Override
        public float buildRotation() {
            return rotation;
        }

        @Override
        public float drawrot() {
            return rotation - 90f;
        }

        @Override
        public Object config() {
            return targetRotation;
        }

        @Override
        public void buildConfiguration(Table table) {
            if (!showConfigUI) {
                super.buildConfiguration(table);
                return;
            }

            table.background(Styles.black6);

            Table main = new Table();
            main.top().left();

            main.table(title -> {
                title.add("[accent]Rotation Settings[]").pad(10);
            }).growX().row();

            main.table(content -> {
                content.left().defaults().left();

                content.add("[lightgray]Current Angle:[] " + (int)rotation + "°").padLeft(10).row();
                content.add("[lightgray]Target Angle:[] " + (int)targetRotation + "°").padLeft(10).row();

                content.add("").row();

                content.table(buttons -> {
                    buttons.center().defaults().size(50).pad(5);

                    if (rotateArrowRegion.found()) {
                        ImageButton btn1 = new ImageButton(new TextureRegionDrawable(rotateArrowRegion), Styles.cleari);
                        btn1.clicked(() -> configure(Mathf.mod(targetRotation - 45f, 360f)));
                        btn1.getImage().setRotation(-45);
                        buttons.add(btn1).tooltip("Rotate -45°");

                        ImageButton btn2 = new ImageButton(new TextureRegionDrawable(rotateArrowRegion), Styles.cleari);
                        btn2.clicked(() -> configure(Mathf.mod(targetRotation - 15f, 360f)));
                        btn2.getImage().setRotation(-15);
                        buttons.add(btn2).tooltip("Rotate -15°");

                        ImageButton btn3 = new ImageButton(new TextureRegionDrawable(rotateArrowRegion), Styles.cleari);
                        btn3.clicked(() -> configure(Mathf.mod(targetRotation + 15f, 360f)));
                        btn3.getImage().setRotation(15);
                        buttons.add(btn3).tooltip("Rotate +15°");

                        ImageButton btn4 = new ImageButton(new TextureRegionDrawable(rotateArrowRegion), Styles.cleari);
                        btn4.clicked(() -> configure(Mathf.mod(targetRotation + 45f, 360f)));
                        btn4.getImage().setRotation(45);
                        buttons.add(btn4).tooltip("Rotate +45°");
                    } else {
                        buttons.button("-45°", Styles.defaultt, () -> {
                            configure(Mathf.mod(targetRotation - 45f, 360f));
                        });

                        buttons.button("-15°", Styles.defaultt, () -> {
                            configure(Mathf.mod(targetRotation - 15f, 360f));
                        });

                        buttons.button("+15°", Styles.defaultt, () -> {
                            configure(Mathf.mod(targetRotation + 15f, 360f));
                        });

                        buttons.button("+45°", Styles.defaultt, () -> {
                            configure(Mathf.mod(targetRotation + 45f, 360f));
                        });
                    }
                }).growX().row();

                content.table(presetButtons -> {
                    presetButtons.left().defaults().size(60, 35).pad(3);

                    String[] directions = {"N", "E", "S", "W"};
                    float[] angles = {90f, 0f, 270f, 180f};

                    for (int i = 0; i < 4; i++) {
                        final float angle = angles[i];
                        presetButtons.button(directions[i], Styles.defaultt, () -> {
                            configure(angle);
                        }).tooltip(directions[i] + " (" + (int)angle + "°)");
                    }
                }).growX().row();

                content.table(customAngle -> {
                    customAngle.left();
                    customAngle.add("[lightgray]Custom Angle:[] ").padLeft(10);

                    TextField field = new TextField((int)targetRotation + "");
                    field.setFilter((textField, c) -> Character.isDigit(c) || c == '-');
                    field.setMaxLength(4);

                    customAngle.add(field).width(60);

                    customAngle.button("Set", Styles.defaultt, () -> {
                        try {
                            float value = Float.parseFloat(field.getText());
                            configure(Mathf.mod(value, 360f));
                        } catch (NumberFormatException ignored) {
                        }
                    }).size(50, 30).padLeft(5);
                }).growX().padTop(10);

            }).growX().pad(10);

            main.row();

            main.table(sliderTable -> {
                sliderTable.left().defaults().padLeft(10);

                sliderTable.add("[lightgray]Slider:[] ");

                Slider slider = new Slider(0, 359, 1, false);
                slider.setValue(targetRotation);
                slider.changed(() -> {
                    configure(slider.getValue());
                });

                sliderTable.add(slider).width(200).padLeft(5);

                sliderTable.label(() -> (int)slider.getValue() + "°").padLeft(10);
            }).growX().padTop(5);

            table.add(main).grow();
        }

        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (this == other) {
                deselect();
                configure(targetRotation);
                return false;
            }
            return true;
        }

        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            write.f(rotation);
            write.f(targetRotation);
        }

        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            rotation = read.f();
            targetRotation = read.f();
        }
    }
}