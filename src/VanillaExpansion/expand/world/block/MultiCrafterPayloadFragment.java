package VanillaExpansion.expand.world.block;

import arc.func.Prov;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.core.UI;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.type.PayloadSeq;

import static mindustry.Vars.control;
import static mindustry.Vars.ui;

public class MultiCrafterPayloadFragment {
    public Table table = new Table();
    private static Table itemInvRef;

    public void build(Group parent) {
        table.name = "multiCrafterPayload";
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);
    }

    public void rebuild() {
        if (!table.visible) {
            table.clear();
            return;
        }
        table.clear();
        table.background(Tex.inventory);
        table.touchable = Touchable.disabled;
        table.margin(4f);
        table.defaults().size(8 * 5).pad(4f);

        int row = 0;
        int cols = 4;

        Building b = getBuild();
        if (b instanceof MultiCrafter.MultiCrafterBuild build
                && build.block instanceof MultiCrafter mc
                && mc.hasPayloads) {

            PayloadSeq payloads = build.getPayloads();
            if (payloads != null && !payloads.isEmpty()) {
                for (UnlockableContent content : MultiCrafter.MultiCrafterBuild.allPayloadTypes) {
                    if (!payloads.contains(content)) continue;
                    int amount = payloads.get(content);
                    if (amount <= 0) continue;
                    table.add(itemImage(content.uiIcon, () -> round(amount)));
                    if (row++ % cols == cols - 1) table.row();
                }
            } else {
                table.visible = false;
            }
        } else {
            table.visible = false;
        }

        updateTablePosition();
    }

    private Building getBuild() {
        try { return Reflect.get(control.input.inv, "build"); }
        catch (Exception e) { Log.err(e); return null; }
    }

    private String round(float f) {
        f = (int) f;
        if (f >= 1000000) return (int)(f/1000000f) + "[gray]" + UI.millions;
        else if (f >= 1000) return (int)(f/1000) + UI.thousands;
        else return (int)f + "";
    }

    private void updateTablePosition() {
        table.pack();
        if (itemInvRef == null) itemInvRef = ui.hudGroup.find("inventory");
        if (itemInvRef != null) table.setPosition(itemInvRef.x, itemInvRef.y + itemInvRef.getPrefHeight() - 4, Align.bottomLeft);
    }

    private Element itemImage(TextureRegion region, Prov<CharSequence> text) {
        Stack stack = new Stack();
        Table t = new Table().left().bottom();
        t.label(text);
        stack.add(new Image(region));
        stack.add(t);
        return stack;
    }
}