package VanillaExpansion.expand.world.block.liquid;

import arc.Core;
import arc.Input;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.style.Drawable;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Eachable;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.*;
import arc.util.pooling.Pools;
import mindustry.core.UI;
import mindustry.entities.Effect;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Edges;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.sandbox.LiquidVoid;

import java.util.Arrays;

import static mindustry.Vars.*;

public class LiquidSorter extends LiquidRouter {
    public int maxTextLength = 20;
    public int maxNewlines = 1;

    public LiquidSorter(String name) {
        super(name);
        configurable = true;
        saveConfig = true;
        clearOnDoubleTap = true;
        rotate = true;
        rotateDraw = false;

        config(Liquid.class, (SortLiquidBuild tile, Liquid l) -> {
            tile.sortLiquid = l;
        });
        config(String.class, (SortLiquidBuild tile, String text) -> {
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
        configClear((SortLiquidBuild tile) -> {
            if(tile.sortLiquid == null) tile.message.setLength(0);
            tile.sortLiquid = null;
        });
    }

    @Override
    public void setBars(){
        super.setBars();


            addBar("flux", (SortLiquidBuild entity) -> new Bar(
                    () -> !entity.hasMaxFlux? Core.bundle.format("noflux") : Core.bundle.format("showflux", entity.maxFlux),
                    () -> !entity.hasMaxFlux? Color.black : (entity.liquids.current() != null? entity.liquids.current().barColor() : Color.lightGray),
                    () -> entity.hasMaxFlux? 1f : 0f
            ));

    }



    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){
        if(configurable) drawPlanConfigCenter(plan, plan.config, name + "-config", false);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(bottomRegion, plan.drawx(), plan.drawy());
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(topRegion, plan.drawx(), plan.drawy(), rotate ? plan.rotation * 90 : 0);
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{bottomRegion, region, topRegion};
    }



    public class SortLiquidBuild extends LiquidRouterBuild{

        public StringBuilder message = new StringBuilder();
        public Liquid sortLiquid = null;
        public boolean hasMaxFlux;
        private float maxFlux = 0f;
        private int[] yesDirs = new int[4];




        private String messageStr;

        public boolean canOutput(Building target, Liquid l){
            if(target instanceof SortLiquidBuild slb){
                return (!slb.hasMaxFlux || slb.maxFlux < maxFlux || !hasMaxFlux) && (slb.sortLiquid == null || slb.sortLiquid.equals(l));
            }
            return (target != null && (target.acceptLiquid(this, l) || target instanceof LiquidVoid.LiquidVoidBuild));
        }

        public int checkDirs(Liquid l){
            int dirs = 0;
            Arrays.fill(yesDirs, 0);
            for(int i = 0; i < 4; i++) {
                Building target = nearby(i);
                if (canOutput(target, l)) {
                    dirs++;
                    yesDirs[i] = 1;
                }
            }
            return dirs;
        }

        @Override
        public void updateTile() {
            calculate();
            Liquid l = sortLiquid != null ? sortLiquid : liquids.current();
            if(!hasMaxFlux) {
                int dirs = checkDirs(l);
                if(enabled) dumpLiquid(liquids.current());
            }else{
                if(l != null && liquids.get(l) > 0 && enabled) {

                    int dirs = checkDirs(l);
                    float toOutput = Math.min(liquids.get(l), maxFlux * dirs / 60f * Time.delta);

                    Building target;
                    if(dirs > 0) {
                        float dirOutput = toOutput / dirs;

                        target = nearby(0);
                        if (canOutput(target, l)) {
                            float accepted = target.liquids.get(l);
                            if (accepted < target.block.liquidCapacity) {
                                float transfer = Math.min(dirOutput, target.block.liquidCapacity - accepted);
                                target.liquids.add(l, transfer);
                                toOutput -= transfer;
                                liquids.remove(l,  transfer);
                            }
                        }
                        target = nearby(1);
                        if (canOutput(target, l)) {
                            float accepted = target.liquids.get(l);
                            if (accepted < target.block.liquidCapacity) {
                                float transfer = Math.min(dirOutput, target.block.liquidCapacity - accepted);
                                target.liquids.add(l, transfer);
                                toOutput -= transfer;
                                liquids.remove(l,  transfer);
                            }
                        }
                        target = nearby(2);
                        if (canOutput(target, l)) {
                            float accepted = target.liquids.get(l);
                            if (accepted < target.block.liquidCapacity) {
                                float transfer = Math.min(dirOutput, target.block.liquidCapacity - accepted);
                                target.liquids.add(l, transfer);
                                toOutput -= transfer;
                                liquids.remove(l,  transfer);
                            }
                        }
                        target = nearby(3);
                        if (canOutput(target, l)) {
                            float accepted = target.liquids.get(l);
                            if (accepted < target.block.liquidCapacity) {
                                float transfer = Math.min(dirOutput, target.block.liquidCapacity - accepted);
                                target.liquids.add(l, transfer);
                                toOutput -= transfer;
                                liquids.remove(l,  transfer);
                            }
                        }
                    }
                }
            }
            if(sortLiquid != null && liquids.current() != sortLiquid) liquids.clear();
        }


        public void calculate(){
            messageStr = message.toString();
            if(!messageStr.isEmpty()){
                if(messageStr.matches("-?\\d+(\\.\\d+)?") || messageStr.matches("-?\\d+\\.")) {
                    maxFlux = Strings.parseFloat(messageStr, 0f);
                    hasMaxFlux = maxFlux > 0f;
                }else{
                    hasMaxFlux = false;
                }
            }else{
                hasMaxFlux = false;
            }
        }



        @Override
        public void draw() {
            super.draw();
            Draw.rect(topRegion, x, y, rotate ? rotdeg() : 0);
            if(sortLiquid != null) {
                Draw.color(sortLiquid.color);
                Draw.alpha(1f);
                Draw.rect(Core.atlas.find(name + "-config"), x, y);
            }
            if(enabled && liquids.current() != null) {
                for(int i = 0; i < 4; i++) {
                    if(yesDirs[i] > 0) drawOutputDir(i);
                }
            }
            Draw.color();
        }

        public void drawOutputDir(int dir){
            Draw.color(Color.white);
            Draw.alpha(Mathf.sin(Time.time / 7f) * 0.5f + 0.5f);
            if(dir < 2) {
                Draw.rect(Core.atlas.find(name + "-config1"), x, y, dir * 90);
            }else{
                Draw.rect(Core.atlas.find(name + "-config2"), x, y, dir * 90);
            }
        }


        @Override
        public void drawSelect(){
            if(renderer.pixelate) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            String text = message == null || message.length() == 0 || !hasMaxFlux ? "[lightgray]" + Core.bundle.format("noflux") : (Core.bundle.format("showflux", UI.formatIcons(message.toString())));

            l.setText(font, text, Color.white, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
            Draw.color();
            font.setColor(message.length() == 0 ? Color.lightGray : Color.white);
            font.draw(text, x - l.width/2f, y - tilesize/2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(1f);

            Pools.free(l);
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(LiquidSorter.this, table, content.liquids(), () -> sortLiquid, this::configure);
            table.row();
            table.image().color(Pal.gray).height(2f).growX().padBottom(4f).row();
            TextButton fluxButton = new TextButton(Core.bundle.format("editflux"), Styles.flatTogglet);
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
                            BaseDialog dialog = new BaseDialog("@editflux");
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
                    }
                );
            table.add(fluxButton).height(40f).growX();
        }

        @Override
        public Liquid config(){
            return sortLiquid;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            //int rel = this.relativeToEdge(source.tile);
            return (!rotate || Edges.getFacingEdge(source.tile, tile).relativeTo(tile) == rotation) && super.acceptLiquid(source, liquid) && (sortLiquid == null || sortLiquid == liquid);
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(sortLiquid == null ? -1 : sortLiquid.id);
            write.str(message.toString());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int id = read.s();
            sortLiquid = id == -1 ? null : content.liquid(id);
            if(revision >= 2) {
                String savedMessage = read.str();
                message.setLength(0);
                message.append(savedMessage);
            }else{
                message.setLength(0);
            }
        }

    }
}
