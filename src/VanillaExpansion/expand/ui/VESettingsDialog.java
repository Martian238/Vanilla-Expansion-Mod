// VESettingsDialog.java (修改后)
package VanillaExpansion.expand.ui;

import VanillaExpansion.VESettings;
import VanillaExpansion.ui.VEStyles;
import arc.Core;
import arc.graphics.Color;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Slider;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Scaling;
import mindustry.ui.dialogs.BaseDialog;
public class VESettingsDialog extends BaseDialog {
    private Table leftPanel;
    private Table categoryTable;
    private Table settingsList;
    private ScrollPane scrollPane;
    private Image selectionFrame;
    private final ButtonGroup<TextButton> group = new ButtonGroup<>();
    private VESettings.Category currentCategory;
    private final Vec2 tmpA = new Vec2();
    private TextButton archiveBtn;
    private boolean archiveView = false;
    public VESettingsDialog() {
        super("");
        this.title.setText("");
        this.titleTable.clear();
        this.cont.clear();
        this.cont.background(VEStyles.dialog);
        this.cont.margin(0f);
        buildUI();
    }
    private void buildUI() {
        Table main = new Table();
        main.margin(8f);
        main.background(VEStyles.dialog);
        leftPanel = new Table();
        leftPanel.background(VEStyles.panel);
        leftPanel.top();
        categoryTable = new Table();
        categoryTable.top();
        categoryTable.defaults().growX().height(60f).pad(2f);
        for (VESettings.Category cat : VESettings.categories) {
            TextButton btn = new TextButton(
                    Core.bundle.get("setting.ve-" + cat.name, cat.name), VEStyles.category);
            btn.clicked(() -> selectCategory(cat, btn));
            categoryTable.add(btn).row();
            group.add(btn);
        }
        archiveBtn = new TextButton(Core.bundle.get("ve-archive-title", "成就档案馆"), VEStyles.category);
        archiveBtn.clicked(() -> selectArchive(archiveBtn));
        categoryTable.add(archiveBtn).row();
        group.add(archiveBtn);
        selectionFrame = new Image(VEStyles.selection, Scaling.stretch);
        selectionFrame.touchable = Touchable.disabled;
        selectionFrame.visible = false;
        leftPanel.addChild(selectionFrame);
        leftPanel.add(categoryTable).growX().pad(10f).row();
        leftPanel.add().grow().row();
        TextButton backBtn = new TextButton(Core.bundle.get("back"), mindustry.ui.Styles.flatt);
        backBtn.clicked(this::hide);
        leftPanel.add(backBtn).growX().height(60f).pad(10f);
        Table rightPanel = new Table();
        rightPanel.background(VEStyles.panel);
        settingsList = new Table();
        settingsList.top();
        settingsList.defaults().growX().padBottom(8f);
        scrollPane = new ScrollPane(settingsList);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setWidget(settingsList);
        rightPanel.add(scrollPane).grow().pad(10f);
        main.add(leftPanel).width(Scl.scl(180f)).growY();
        main.add().width(Scl.scl(8f));
        main.add(rightPanel).grow();
        this.cont.add(main).grow();
        if (VESettings.categories.size > 0) {
            Core.app.post(() -> {
                TextButton firstBtn = group.getButtons().first();
                selectCategory(VESettings.categories.first(), firstBtn);
            });
        }
    }
    private void selectCategory(VESettings.Category cat, TextButton btn) {
        if (currentCategory == cat && !archiveView) return;
        archiveView = false;
        currentCategory = cat;
        btn.setChecked(true);
        positionFrame(btn);
        rebuildSettings();
    }
    private void selectArchive(TextButton btn) {
        if (archiveView) return;
        archiveView = true;
        currentCategory = null;
        btn.setChecked(true);
        positionFrame(btn);
        rebuildArchive();
    }
    private void positionFrame(TextButton btn) {
        categoryTable.validate();
        leftPanel.validate();
        tmpA.set(0f, 0f);
        btn.localToAscendantCoordinates(leftPanel, tmpA);
        float pad = VEStyles.FRAME_PAD;
        float targetX = tmpA.x - pad;
        float targetY = tmpA.y - pad;
        float targetW = btn.getWidth() + pad * 2f;
        float targetH = btn.getHeight() + pad * 2f;
        if (!selectionFrame.visible) {
            selectionFrame.setBounds(targetX, targetY, targetW, targetH);
            selectionFrame.visible = true;
        } else {
            selectionFrame.clearActions();
            selectionFrame.actions(
                    Actions.parallel(
                            Actions.moveTo(targetX, targetY, 0.15f, Interp.smooth),
                            Actions.sizeTo(targetW, targetH, 0.15f, Interp.smooth)
                    )
            );
        }
    }
    private void rebuildArchive() {
        settingsList.clear();
        AchievementArchive.buildInto(settingsList);
        scrollPane.setScrollY(0f);
    }
    private void rebuildSettings() {
        settingsList.clear();
        if (currentCategory == null) return;
        Table header = new Table();
        header.top().left();
        Label headerLabel = new Label(Core.bundle.get("setting.ve-" + currentCategory.name, currentCategory.name));
        headerLabel.setColor(Color.white);
        headerLabel.setFontScale(1.2f);
        headerLabel.setWrap(true);
        header.add(headerLabel).growX().left().row();
        String catDesc = Core.bundle.get("setting.ve-" + currentCategory.name + ".desc", "");
        if (!catDesc.isEmpty()) {
            Label catDescLabel = new Label(catDesc);
            catDescLabel.setColor(Color.lightGray);
            catDescLabel.setFontScale(0.9f);
            catDescLabel.setWrap(true);
            header.add(catDescLabel).growX().left().padTop(10f).row();
        }
        settingsList.add(header).growX().padBottom(20f).row();
        for (VESettings.Setting setting : currentCategory.settings) {
            Stack stack = new Stack();
            Table card = new Table();
            card.margin(10f);
            card.left();
            card.defaults().left().padRight(10f);
            Label titleLabel = new Label(Core.bundle.get("setting." + setting.key + ".name", setting.key));
            titleLabel.setColor(Color.white);
            titleLabel.setWrap(true);
            card.add(titleLabel).growX();
            if (setting.type == VESettings.SettingType.bool) {
                CheckBox box = new CheckBox("");
                box.setChecked(Core.settings.getBool(setting.key, setting.boolDef));
                box.changed(() -> {
                    Core.settings.put(setting.key, box.isChecked());
                    if (setting.restart) VESettings.changed = true;
                });
                card.add(box).right().padRight(0f);
            } else if (setting.type == VESettings.SettingType.slider) {
                Table sliderTable = new Table();
                Slider slider = new Slider(setting.min, setting.max, setting.step, false);
                slider.setValue(Core.settings.getFloat(setting.key, setting.floatDef));
                Label valLabel = new Label(Mathf.round(slider.getValue() * 100f) / 100f + "");
                valLabel.setAlignment(Align.right);
                slider.changed(() -> {
                    Core.settings.put(setting.key, slider.getValue());
                    valLabel.setText(Mathf.round(slider.getValue() * 100f) / 100f + "");
                    if (setting.restart) VESettings.changed = true;
                });
                sliderTable.add(slider).growX().padRight(10f);
                sliderTable.add(valLabel).width(Scl.scl(40f));
                card.add(sliderTable).width(Scl.scl(200f)).right().padRight(0f);
            }
            String desc = Core.bundle.get("setting." + setting.key + ".description", "");
            if (!desc.isEmpty()) {
                card.row();
                Label descLabel = new Label("[lightgray]" + desc + "[]");
                descLabel.setWrap(true);
                descLabel.setFontScale(0.85f);
                card.add(descLabel).growX().padTop(6f).colspan(2);
            }
            stack.add(card);
            settingsList.add(stack).growX().padBottom(8f).row();
        }
        settingsList.add().height(20f).row();
        scrollPane.setScrollY(0f);
    }
}