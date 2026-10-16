package VanillaExpansion.ui.bossrush.card;

import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import VanillaExpansion.ui.bossrush.BossRushNode;
import VanillaExpansion.ui.bossrush.BossRushView;

public class GalleryCardWidget extends BossRushCardWidget{
    public GalleryCardWidget(BossRushNode node, BossRushView parentView){
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
    }
}