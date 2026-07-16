package VanillaExpansion.expand.type.unit;

import VanillaExpansion.expand.ui.ExpandUnitUI;
import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.event.HandCursorListener;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.entities.abilities.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import VanillaExpansion.expand.type.unit.StuffType.*;
import mindustry.type.weapons.*;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

/**
 * 可扩展单位核心 - 支持装备物品的灵活单位
 */
public class ExpandUnitCore extends UnitType {
    public Seq<StuffSlotData> stuffSlots = new Seq<>();
    public ObjectMap<Integer, StuffType> equippedStuff = new ObjectMap<>();
    public float maxWeight = 20f;
    public float currentWeight = 0f;
    public int maxStuffSlots = 6;
    public Seq<Weapon> baseWeapons = new Seq<>();
    public Seq<Ability> baseAbilities = new Seq<>();
    public Seq<DrawPart> baseParts = new Seq<>();
    public float weightSpeedMultiplier = 0.02f;
    public float baseSpeed = 1f;

    /** 当前选中的物品槽位索引 */
    public int selectedSlot = 0;

    /** 物品栏是否启用（由设置控制） */
    public static boolean equipmentEnabled = true;

    public ExpandUnitCore(String name) {
        super(name);
        for (int i = 0; i < 4; i++) {
            stuffSlots.add(new StuffSlotData(i, StuffSlot.general, "Slot " + (i + 1)));
        }
        baseSpeed = speed;
    }

    @Override
    public void init() {
        super.init();
        weapons.clear();
        abilities.clear();
        parts.clear();
        weapons.addAll(baseWeapons);
        abilities.addAll(baseAbilities);
        parts.addAll(baseParts);
        applyAllStuffs();
        recalcStats();

        // 注册设置 - 参考 MindustryOptiFine 的方式
        registerSettings();
    }

    /** 注册设置到游戏设置菜单 - 参考 MindustryOptiFine 的方式 */
    private void registerSettings() {
        // 延迟注册，确保 ui 已初始化
        Time.runTask(5f, () -> {
            if (ui == null || ui.settings == null) return;

            ui.settings.addCategory("@proxima-equipment", "icon-equipment", st -> {
                // 启用/禁用物品栏
                st.checkPref("proxima-equipment-enabled", true, b -> {
                    equipmentEnabled = b;
                    if (!b) {
                        ExpandUnitUI.hide();
                    }
                });

                st.row();

                // 物品栏管理按钮
                st.button("Open Equipment Manager", Icon.settings, () -> {
                    if (Vars.player != null && Vars.player.unit() != null &&
                            Vars.player.unit().type instanceof ExpandUnitCore) {
                        showEquipmentManager(Vars.player.unit());
                    } else {
                        ui.showInfoFade("No ExpandUnitCore unit selected", 2f);
                    }
                }).growX().pad(4f);

                st.row();

                // 显示当前装备信息
                st.label(() -> {
                    Unit unit = Vars.player != null ? Vars.player.unit() : null;
                    if (unit == null || !(unit.type instanceof ExpandUnitCore)) {
                        return "[gray]No ExpandUnitCore unit selected";
                    }
                    ExpandUnitCore core = (ExpandUnitCore) unit.type;
                    StuffType selected = core.getSelectedStuff();
                    if (selected != null) {
                        return "[accent]Active: " + selected.stuffName + " [gray](Weight: " + core.currentWeight + "/" + core.maxWeight + ")";
                    }
                    return "[gray]No active item";
                }).left().pad(4f);
            });
        });
    }

    public boolean equipStuff(Unit unit, int slotIndex, StuffType stuff) {
        if (slotIndex < 0 || slotIndex >= stuffSlots.size) return false;
        StuffSlotData slot = stuffSlots.get(slotIndex);
        if (slot.locked) return false;
        if (stuff.slot != slot.slotType && slot.slotType != StuffSlot.general) return false;
        if (currentWeight + stuff.weight > maxWeight) return false;

        StuffType oldStuff = equippedStuff.get(slotIndex);
        if (oldStuff != null) unequipStuff(unit, slotIndex);

        equippedStuff.put(slotIndex, stuff);
        currentWeight += stuff.weight;
        reloadUnit(unit);

        if (unit instanceof ExpandUnit) {
            ExpandUnit eu = (ExpandUnit) unit;
            eu.getEquippedStuff().put(slotIndex, stuff);
        }
        return true;
    }

    public void unequipStuff(Unit unit, int slotIndex) {
        StuffType stuff = equippedStuff.remove(slotIndex);
        if (stuff != null) {
            currentWeight -= stuff.weight;
            reloadUnit(unit);
            if (unit instanceof ExpandUnit) {
                ExpandUnit eu = (ExpandUnit) unit;
                eu.getEquippedStuff().remove(slotIndex);
            }
        }
    }

    public void reloadUnit(Unit unit) {
        if (unit == null) return;
        if (unit instanceof ExpandUnit) {
            ExpandUnit eu = (ExpandUnit) unit;
            eu.clearDynamicComponents();
            for (StuffType stuff : equippedStuff.values()) {
                if (stuff == null) continue;
                eu.addDynamicWeapons(stuff.weapons);
                eu.addDynamicAbilities(stuff.abilities);
                eu.addDynamicParts(stuff.parts);
            }
        }
    }

    public void applyAllStuffs() {
        weapons.removeAll(w -> !baseWeapons.contains(w));
        abilities.removeAll(a -> !baseAbilities.contains(a));
        parts.removeAll(p -> !baseParts.contains(p));
        for (StuffType stuff : equippedStuff.values()) {
            if (stuff == null) continue;
            weapons.addAll(stuff.weapons);
            abilities.addAll(stuff.abilities);
            parts.addAll(stuff.parts);
        }
    }

    public void recalcStats() {
        float weightRatio = Mathf.clamp(currentWeight / Math.max(maxWeight, 0.001f));
        float speedPenalty = 1f - (weightRatio * weightSpeedMultiplier);
        speed = baseSpeed * Math.max(speedPenalty, 0.1f);
    }

    public Seq<StuffType> getAvailableStuffs(Unit unit) {
        return StuffType.allStuffs;
    }

    public void selectSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < stuffSlots.size) {
            selectedSlot = slotIndex;
        }
    }

    public StuffType getSelectedStuff() {
        if (selectedSlot >= 0 && selectedSlot < stuffSlots.size) {
            return equippedStuff.get(selectedSlot);
        }
        return null;
    }

    public void nextSlot() {
        int next = (selectedSlot + 1) % stuffSlots.size;
        selectSlot(next);
    }

    public void prevSlot() {
        int prev = (selectedSlot - 1 + stuffSlots.size) % stuffSlots.size;
        selectSlot(prev);
    }

    @Override
    public void update(Unit unit) {
        super.update(unit);

        if (!equipmentEnabled) return;

        if (unit instanceof ExpandUnit) {
            ExpandUnit eu = (ExpandUnit) unit;

            // 数字键 1-9 选择槽位
            for (int i = 0; i < Math.min(9, stuffSlots.size); i++) {
                if (Core.input.keyTap(KeyCode.byOrdinal(49 + i))) {
                    selectSlot(i);
                }
            }

            // 处理物品效果
            for (int i = 0; i < stuffSlots.size; i++) {
                StuffType stuff = eu.getEquippedStuff().get(i);
                if (stuff != null) {
                    boolean isActive = (i == selectedSlot);
                    if (isActive && !stuff.passive) {
                        if (stuff.canActivate(unit)) {
                            StuffSlotData slotData = stuffSlots.get(i);
                            if (stuff.processor != null) {
                                stuff.processor.process(unit, stuff, slotData);
                            }
                        }
                    }
                    if (stuff.passive) {
                        if (stuff.canActivate(unit)) {
                            StuffSlotData slotData = stuffSlots.get(i);
                            if (stuff.processor != null) {
                                stuff.processor.process(unit, stuff, slotData);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Unit create(Team team) {
        Unit unit = super.create(team);
        if (unit instanceof ExpandUnit) {
            ExpandUnit eu = (ExpandUnit) unit;
            eu.initExpandable(this);
            for (int i = 0; i < stuffSlots.size; i++) {
                StuffType stuff = equippedStuff.get(i);
                if (stuff != null) {
                    eu.getEquippedStuff().put(i, stuff);
                    eu.addDynamicWeapons(stuff.weapons);
                    eu.addDynamicAbilities(stuff.abilities);
                    eu.addDynamicParts(stuff.parts);
                }
            }
        }
        return unit;
    }

    @Override
    public void display(Unit unit, Table table) {
        super.display(unit, table);

        // 在信息面板显示当前装备状态（只读）
        table.row();
        table.add("[accent]Equipment").left().colspan(2).padTop(6f).padBottom(2f);
        table.row();

        StuffType current = getSelectedStuff();
        if (current != null) {
            table.add("[gray]▶ []" + current.stuffName + " [gray](" + current.weight + "w)").left().colspan(2).padBottom(2f);
        } else {
            table.add("[gray]No active item").left().colspan(2).padBottom(2f);
        }

        table.row();
        table.add("[gray]Press 1-" + Math.min(9, stuffSlots.size) + " to switch").left().colspan(2).fontScale(0.55f);
        table.row();
        table.add("[gray]Settings → Equipment Manager").left().colspan(2).fontScale(0.45f);
    }

    /** 显示装备管理对话框 */
    public void showEquipmentManager(Unit unit) {
        Dialog dialog = new Dialog("Equipment Manager");
        dialog.setFillParent(false);
        dialog.addCloseButton();

        Table content = dialog.cont;

        content.add("[accent]Select equipment slot").padBottom(8f).row();

        ObjectMap<Integer, StuffType> unitEquipped = unit instanceof ExpandUnit ?
                ((ExpandUnit) unit).getEquippedStuff() : equippedStuff;

        for (int i = 0; i < stuffSlots.size; i++) {
            final int slotIndex = i;
            StuffSlotData slot = stuffSlots.get(i);
            StuffType currentStuff = unitEquipped.get(i);
            boolean isSelected = (i == selectedSlot);

            Table row = new Table();
            row.left().margin(2f);

            row.add("[gray]" + (i + 1) + ".").padRight(4f);
            row.add("[gray]" + slot.slotType.name()).padRight(6f);

            if (currentStuff != null && currentStuff.icon != null) {
                row.image(currentStuff.icon).size(20f).padRight(4f);
                row.add(currentStuff.stuffName).color(isSelected ? Pal.accent : Color.white).padRight(4f);
            } else {
                row.add("[gray]Empty").padRight(4f);
            }

            if (isSelected) {
                row.image(Icon.chat).color(Pal.accent).size(14f).padRight(4f);
            }

            row.add().growX();

            row.button("Select", Styles.flatBordert, () -> {
                showStuffSelection(unit, slotIndex);
            }).size(70f, 26f);

            if (currentStuff != null) {
                row.button(Icon.trash, Styles.clearNonei, () -> {
                    unequipStuff(unit, slotIndex);
                    dialog.hide();
                    showEquipmentManager(unit);
                }).size(26f);
            }

            if (!isSelected && currentStuff != null) {
                row.addListener(new HandCursorListener());
                row.clicked(() -> {
                    selectSlot(slotIndex);
                    dialog.hide();
                    showEquipmentManager(unit);
                });
            }

            content.add(row).growX().pad(2f);
            content.row();
        }

        content.row();
        content.add("[gray]Weight: " + currentWeight + "/" + maxWeight).left().padTop(6f);

        dialog.show();
    }

    /** 显示物品选择对话框 */
    private void showStuffSelection(Unit unit, int slotIndex) {
        Dialog selectDialog = new Dialog("Select Equipment");
        selectDialog.setFillParent(false);
        selectDialog.addCloseButton();

        Table content = selectDialog.cont;

        String slotName = stuffSlots.get(slotIndex).slotType.name();
        content.add("[accent]Slot " + (slotIndex + 1) + " (" + slotName + ")").padBottom(8f).row();

        Seq<StuffType> available = getAvailableStuffs(unit);
        StuffSlot targetSlot = stuffSlots.get(slotIndex).slotType;

        int cols = 3;
        int count = 0;

        for (StuffType stuff : available) {
            if (stuff.slot == targetSlot || targetSlot == StuffSlot.general || stuff.slot == StuffSlot.general) {
                content.button(b -> {
                    b.clear();
                    b.top().left();
                    b.table(Styles.black6, bg -> {
                        bg.margin(3f);
                        if (stuff.icon != null && stuff.icon.found()) {
                            bg.image(stuff.icon).size(32f).scaling(Scaling.fit);
                        }
                        bg.row();
                        bg.add(stuff.stuffName).fontScale(0.45f);
                        bg.row();
                        bg.add("[gray]" + stuff.weight + "w").fontScale(0.35f);
                    }).grow();
                }, () -> {
                    equipStuff(unit, slotIndex, stuff);
                    selectDialog.hide();
                    showEquipmentManager(unit);
                }).size(70f, 70f).pad(2f);

                if (++count % cols == 0) content.row();
            }
        }

        content.row();
        content.button("[red]Clear Slot", () -> {
            unequipStuff(unit, slotIndex);
            selectDialog.hide();
            showEquipmentManager(unit);
        }).size(160f, 32f).padTop(8f);

        selectDialog.show();
    }

    public static class StuffSlotData {
        public int index;
        public StuffSlot slotType;
        public String name;
        public boolean locked = false;
        public @Nullable StuffType defaultStuff;

        public StuffSlotData(int index, StuffSlot slotType, String name) {
            this.index = index;
            this.slotType = slotType;
            this.name = name;
        }
    }
}