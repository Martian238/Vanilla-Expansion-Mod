package VanillaExpansion.expand.world.block.sandbox;

import VanillaExpansion.VanillaExpansionMod;
import VanillaExpansion.expand.world.block.liquid.LiquidSorter;
import arc.Core;
import arc.Input;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.MessageBlock;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;

import static mindustry.Vars.*;

public class TitleTextBlock extends Block {
    public int maxTextLength = 50;
    public int maxNewlines = 1;
    public TitleTextBlock(String name) {
        super(name);
        category = Category.logic;
        buildVisibility = BuildVisibility.worldProcessorOnly;
        envEnabled = Env.any;
        privileged = true;
        drawDisabled = false;


        config(String.class, (TitleTextBuild tile, String text) -> {
            if(text.length() > maxTextLength){
                return; //no.
            }
            tile.message.ensureCapacity(text.length());
            tile.message.setLength(0);
            text = text.trim();
            int count = 0;
            for(int i = 0; i < text.length(); i++){
                char c = text.charAt(i);
                if(c == '\n'){
                    if(count++ <= maxNewlines){
                        tile.message.append('\n');
                    }
                }else{
                    tile.message.append(c);
                }
            }
        });
    }

    @Override
    public boolean checkForceDark(Tile tile){
        return !accessible();
    }

    public boolean accessible(){
        return !privileged || state.rules.editor || state.playtestingMap != null || state.rules.allowEditWorldProcessors;
    }

    @Override
    public boolean canBreak(Tile tile){
        return accessible();
    }

    public class TitleTextBuild extends Building {
        public StringBuilder message = new StringBuilder();
        private String messageStr;
        private boolean soundPlayed = false;
        private boolean valid;
        private Texture texture;
        private TextureRegion region;

        private final Seq<String> validTitles = Seq.with(
                "serpulo","erekir","cyclant","maress","sitrullus","thavina","proxima"
        );

        public void draw(){
            messageStr = message.toString();
            valid = false;
            for(String s : validTitles){
                if(s.equals(messageStr)){
                    valid = true;
                    break;
                }
            }
            if(!enabled && valid && !state.isEditor()){
                if (!soundPlayed) {
                    soundPlayed = true;
                    Fi modRoot = Vars.mods.getMod(VanillaExpansionMod.class).root;
                }
                Draw.z(Layer.end - 1f);
                Draw.color(Color.white);
                Draw.alpha(1f);
                float scale = 1080f / 1920f;
                float w = Core.camera.width;
                float x = Core.camera.position.x;
                float y = Core.camera.position.y;
                Draw.rect(region, x, y, w, w * scale, 0f);
            }else{
                soundPlayed = false;
            }
        }

        public void updateTile(){

        }

        public void buildConfiguration(Table table){
            TextButton fluxButton = new TextButton("edit", Styles.flatTogglet);
            fluxButton.changed(() -> {
                        if (mobile) {
                            var contents = message;
                            Core.input.getTextInput(new Input.TextInput() {{
                                text = contents.toString();
                                multiline = true;
                                maxLength = maxTextLength;
                                accepted = str -> {
                                    if (!str.contentEquals(contents)) configure(str);
                                };
                            }});
                        } else {
                            BaseDialog dialog = new BaseDialog("title");
                            dialog.setFillParent(false);
                            TextArea a = dialog.cont.add(new TextArea(message.toString().replace("\r", "\n"))).size(380f, 160f).get();
                            a.setFilter((textField, c) -> {
                                if (c == '\n') {
                                    int count = 0;
                                    for (int i = 0; i < textField.getText().length(); i++) {
                                        if (textField.getText().charAt(i) == '\n') {
                                            count++;
                                        }
                                    }
                                    return count < maxNewlines;
                                }
                                return true;
                            });
                            a.setMaxLength(maxTextLength);
                            dialog.cont.row();
                            dialog.cont.label(() -> a.getText().length() + " / " + maxTextLength).color(Color.lightGray);
                            dialog.buttons.button("@ok", () -> {
                                if (!a.getText().contentEquals(message)) configure(a.getText());
                                dialog.hide();
                            }).size(130f, 60f);
                            dialog.update(() -> {
                                if (tile.build != this) {
                                    dialog.hide();
                                }
                            });
                            dialog.closeOnBack();
                            dialog.show();
                        }
                        deselect();
                        fluxButton.setChecked(false);
                        messageStr = message.toString();
                    }
            );
            table.add(fluxButton).height(40f).width(80f);
        }


        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.str(message.toString());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
                String savedMessage = read.str();
                message.setLength(0);
                message.append(savedMessage);
        }
    }


}
