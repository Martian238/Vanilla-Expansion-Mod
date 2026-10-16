package VanillaExpansion.ui.bossrush.card;

import arc.Core;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import VanillaExpansion.ui.bossrush.BossRushNode;
import VanillaExpansion.ui.bossrush.BossRushRegistry;
import VanillaExpansion.ui.bossrush.BossRushView;
import VanillaExpansion.ui.bossrush.map.BossRushSaveSelect;

public class LevelCardWidget extends BossRushCardWidget{
    public LevelCardWidget(BossRushNode node, BossRushView parentView){
        super(node, parentView);
    }

    @Override
    protected void buildExtra(Table buttons){
        float btn = 18f;
        buttons.defaults().size(btn).pad(4f);
        Button info = buttons.button(Icon.info, () -> {
            new BaseDialog(node.title){{
                cont.pane(t -> {
                    t.add(node.description).wrap().growX().pad(8f).row();
                }).grow();
                addCloseButton();
            }}.show();
        }).visible(() -> node.unlocked).get();
        info.hovered(() -> onButtonHover(node.title, node.description));
        info.exited(this::onButtonExit);
        Button load = buttons.button(Icon.upload, () -> {
            new BossRushSaveSelect(node).show();
        }).visible(() -> node.unlocked).get();
        load.hovered(() -> onButtonHover(
                Core.bundle.get("bossrush.hover.load"),
                Core.bundle.get("bossrush.hover.load.desc")
        ));
        load.exited(this::onButtonExit);
        Button play = buttons.button(Icon.play, () -> {
            if(node.map == null){
                Vars.ui.showInfo("@bossrush.map.notfound");
                return;
            }

            java.io.File nodeDir = new java.io.File(Vars.saveDirectory.absolutePath(), "bossrush/" + node.id);
            if(!nodeDir.exists()){
                boolean ok = nodeDir.mkdirs();
                //Log.info("BossRush: mkdirs @ -> @", nodeDir.getAbsolutePath(), ok);
            }
            BossRushRegistry.startNewGame(node, parentView);
            BossRushRegistry.returnToMenu();
            Vars.control.playMap(node.map, node.map.rules(), false);
        }).visible(() -> node.unlocked).get();
        play.hovered(() -> onButtonHover(
                Core.bundle.get("bossrush.hover.play"),
                Core.bundle.get("bossrush.hover.play.desc")
        ));
        play.exited(this::onButtonExit);
    }
}