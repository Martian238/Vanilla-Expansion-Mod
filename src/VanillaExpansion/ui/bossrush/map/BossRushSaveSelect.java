package VanillaExpansion.ui.bossrush.map;

import arc.Core;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.Saves.SaveSlot;
import mindustry.gen.Icon;
import mindustry.ui.BorderImage;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import VanillaExpansion.ui.bossrush.BossRushNode;
import VanillaExpansion.ui.bossrush.BossRushRegistry;

public class BossRushSaveSelect extends BaseDialog{
    private final BossRushNode node;
    private Table slots;
    private String searchString;

    public BossRushSaveSelect(BossRushNode node){
        super("@bossrush.load");
        this.node = node;

        setup();

        shown(() -> {
            searchString = "";
            setup();
        });
        onResize(this::setup);

        addCloseButton();
    }

    private void setup(){
        cont.clear();

        slots = new Table();
        ScrollPane pane = new ScrollPane(slots);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        rebuild();

        Table search = new Table();
        search.image(Icon.zoom);
        TextField searchField = search.field("", t -> {
            searchString = t.length() > 0 ? t.toLowerCase() : null;
            rebuild();
        }).maxTextLength(50).growX().get();
        searchField.setMessageText("@save.search");

        cont.add(search).growX();
        cont.row();
        cont.add(pane).growY();
    }

    private void rebuild(){
        slots.clear();
        slots.marginRight(24).marginLeft(20f);

        String prefix = "bossrush/" + node.id + "/";
        Seq<SaveSlot> array = new Seq<>();
        for(SaveSlot slot : Vars.control.saves.getSaveSlots()){
            String path = slot.file.path().replace('\\', '/');
            if(path.contains(prefix)){
                array.add(slot);
            }
        }

        array.sort((slot, other) -> -Long.compare(slot.getTimestamp(), other.getTimestamp()));

        int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(470)), 1);
        int i = 0;
        boolean any = false;

        for(SaveSlot slot : array){
            if(searchString != null && !Strings.stripColors(slot.getName()).toLowerCase().contains(searchString)){
                continue;
            }
            any = true;

            TextButton button = new TextButton("", Styles.grayt);
            button.getLabel().remove();
            button.clearChildren();
            button.defaults().left();

            button.table(title -> {
                title.add("[accent]" + slot.getName()).left().growX().width(230f).wrap();
                title.table(t -> {
                    t.right();
                    t.defaults().size(40f);
                    t.button(Icon.trash, Styles.emptyi, () -> {
                        Vars.ui.showConfirm("@confirm", "@save.delete.confirm", () -> {
                            slot.delete();
                            rebuild();
                        });
                    }).right();
                }).padRight(-10).growX();
            }).growX().colspan(2);
            button.row();

            String color = "[lightgray]";
            TextureRegion def = Core.atlas.find("nomap");

            button.left().add(new BorderImage(def, 4f)).update(im -> {
                TextureRegionDrawable draw = (TextureRegionDrawable)im.getDrawable();
                if(draw.getRegion().texture.isDisposed()){
                    draw.setRegion(def);
                }
                Texture text = slot.previewTexture();
                if(draw.getRegion() == def && text != null){
                    draw.setRegion(new TextureRegion(text));
                }
                im.setScaling(Scaling.fit);
            }).left().size(160f).padRight(6);

            button.table(meta -> {
                meta.left().top();
                meta.defaults().padBottom(-2).left().width(280f);
                meta.row();
                meta.labelWrap(Core.bundle.format("save.map", color + (slot.getMap() == null ? Core.bundle.get("unknown") : slot.getMap().name())));
                meta.row();
                meta.labelWrap(color + slot.getDate());
                meta.row();
            }).left().growX().width(260f);

            button.clicked(() -> {
                if(!button.childrenPressed()){
                    runLoadSave(slot);
                }
            });

            slots.add(button).uniformX().fillX().pad(4).padRight(8f).margin(10f);

            if(++i % cols == 0){
                slots.row();
            }
        }

        if(!any){
            slots.add("@save.none");
        }
    }

    private void runLoadSave(SaveSlot slot){
        //Log.info("BossRushSaveSelect: loading slot @", slot.file.absolutePath());

        Vars.ui.loadAnd(() -> {
            //Log.info("BossRushSaveSelect: loadAnd started");
            try{
                Vars.net.reset();
                slot.load();
                //Log.info("BossRushSaveSelect: slot.load() done");

                Vars.state.rules.editor = false;
                Vars.state.rules.sector = null;
                Vars.state.set(mindustry.core.GameState.State.playing);
                //Log.info("BossRushSaveSelect: state set to playing");

                BossRushRegistry.currentNode = node;
                BossRushRegistry.setCurrentSaveFile(slot.file);
                BossRushRegistry.cleanupForLoad();
                if(Vars.ui.planet != null && Vars.ui.planet.isShown()){
                    Vars.ui.planet.hide();
                }
                hide();
                Vars.ui.paused.hide();

                Vars.logic.play();
                //Log.info("BossRushSaveSelect: all UI hidden, logic playing");
            }catch(Throwable e){
                //Log.err("BossRushSaveSelect: failed to load", e);
                Vars.logic.reset();
                Vars.ui.showErrorMessage("@save.corrupted");
            }
        });
    }
}