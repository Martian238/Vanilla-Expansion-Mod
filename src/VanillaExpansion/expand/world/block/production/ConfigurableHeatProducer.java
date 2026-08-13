package VanillaExpansion.expand.world.block.production;

import arc.Input;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.Effect;
import mindustry.entities.Lightning;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.game.Team;
import mindustry.graphics.Pal;
import mindustry.world.blocks.heat.HeatBlock;
import mindustry.world.blocks.heat.HeatProducer;
import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.Input.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.MessageBlock;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ConfigurableHeatProducer extends HeatProducer {

    public int maxTextLength = 20;
    public int maxNewlines = 1;

    public float defaultCraftTime = 60 * 8f;
    public float defaultHeat = 15f;
    public float maxHeat = 150f;
    public float penaltyMultiplier = 38 / 135f;
    public BulletType wrongBullet = new BasicBulletType();



    public ConfigurableHeatProducer(String name) {
        super(name);
        configurable = true;
        ambientSound = Sounds.loopHum;
        config(String.class, (ConfigurableHeatProducer.ConfigureableHeatProducerBuild tile, String text) -> {
            if(text.length() > maxTextLength || !accessible()){
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

    public boolean accessible(){
        return true;
    }

    @Override
    public boolean canBreak(Tile tile){
        return accessible();
    }


    public class ConfigureableHeatProducerBuild extends HeatProducerBuild implements LReadable, HeatBlock {
        public StringBuilder message = new StringBuilder();

        @Override
        public void drawSelect(){
            if(renderer.pixelate) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            String text = message == null || message.length() == 0 ? "[lightgray]" + Core.bundle.get("empty") : UI.formatIcons(message.toString());

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
        public boolean shouldShowConfigure(Player player){
            return accessible();
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.settings, Styles.cleari, () -> {
                if(mobile){
                    var contents = message;
                    Core.input.getTextInput(new TextInput(){{
                        text = contents.toString();
                        multiline = true;
                        maxLength = maxTextLength;
                        accepted = str -> {
                            if(!str.contentEquals(contents)) configure(str);
                        };
                    }});
                }else{
                    BaseDialog dialog = new BaseDialog("@editheater");
                    dialog.setFillParent(false);
                    TextArea a = dialog.cont.add(new TextArea(message.toString().replace("\r", "\n"))).size(380f, 160f).get();
                    a.setFilter((textField, c) -> {
                        if(c == '\n'){
                            int count = 0;
                            for(int i = 0; i < textField.getText().length(); i++){
                                if(textField.getText().charAt(i) == '\n'){
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
                        if(!a.getText().contentEquals(message)) configure(a.getText());
                        dialog.hide();
                    }).size(130f, 60f);
                    dialog.update(() -> {
                        if(tile.build != this){
                            dialog.hide();
                        }
                    });
                    dialog.closeOnBack();
                    dialog.show();
                }
                deselect();
            }).size(40f);
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other || !accessible()){
                deselect();
                return false;
            }

            return true;
        }

        @Override
        public Cursor getCursor(){
            return !accessible() ? SystemCursor.arrow : super.getCursor();
        }

        @Override
        public boolean readable(LExecutor exec){
            return isValid();
        }

        @Override
        public void read(LVar position, LVar output){
            int address = position.numi();
            output.setnum(address < 0 || address >= message.length() ? Double.NaN : message.charAt(address));
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case bufferSize -> message.length();
                default -> super.sense(sensor);
            };
        }

        @Override
        public void damage(float damage){
            if(privileged) return;
            super.damage(damage);
        }

        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public boolean collide(Bullet other){
            return !privileged;
        }

        @Override
        public void handleString(Object value){
            message.setLength(0);
            message.append(value);
        }

        @Override
        public void updateTableAlign(Table table){
            Vec2 pos = Core.input.mouseScreen(x, y + size * tilesize / 2f + 1);
            table.setPosition(pos.x, pos.y, Align.bottom);
        }

        @Override
        public String config(){
            return message.toString();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.str(message.toString());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            message = new StringBuilder(read.str());
        }




        public float heat;
        public float writtenHeat;
        public String messageStr;
        public float currentCraftTime;





        @Override
        public void updateTile(){

            messageStr = message.toString();
            if(!messageStr.isEmpty()){
                if(messageStr.matches("-?\\d+(\\.\\d+)?") || messageStr.matches("-?\\d+\\.")) {
                    writtenHeat = Strings.parseFloat(messageStr, 0f);
                    if(writtenHeat < 0f) {
                        baseExplosiveness = 10f;
                        wrongBullet.create(this, this.x, this.y, 0f);
                        Effect.shake(8,30,this.x, this.y);
                        this.kill();
                    }else if(writtenHeat > maxHeat){
                        writtenHeat = maxHeat;
                    }
                }else{
                    baseExplosiveness = 10f;
                    wrongBullet.create(this, this.x, this.y, 0f);
                    Effect.shake(8,30,this.x, this.y);
                    this.kill();
                }
            }else{
                writtenHeat = 0f;
            }

            boolean crazyMode = Strings.parseFloat(messageStr, 0f) > maxHeat;

            if(crazyMode && efficiency > 0){
                if(Mathf.chanceDelta(0.06f * efficiency)){
                    Fx.reactorsmoke.at(x + Mathf.range(size * 2.3f), y + Mathf.range(size * 2.3f), 0f);
                }
                if(Mathf.chanceDelta(0.015f * efficiency)){
                    Lightning.create(team, Items.phaseFabric.color, Math.abs(Mathf.random(20f, 200f)), x + Mathf.range(size * 2f), y + Mathf.range(size * 2f), Mathf.range(0,360), 7 + Mathf.range(5));
                    float crazyDamage = Math.abs(Mathf.random(0.01f, 0.2f) * maxHealth);
                    if(crazyDamage < health) {
                        damagePierce(crazyDamage);
                    }else{
                        baseExplosiveness = 10f;
                        wrongBullet.create(this, this.x, this.y, 0f);
                        Effect.shake(8,30,this.x, this.y);
                        this.kill();
                    }
                }
            }



            //heat approaches target at the same speed regardless of efficiency
            if(!crazyMode) {
                heat = Mathf.approachDelta(heat, writtenHeat * efficiency, warmupRate * delta());
            }else{
                heat = Math.min(writtenHeat * efficiency * Math.abs(Mathf.random(0f, Mathf.random(0f, Mathf.random(0f, Strings.parseFloat(messageStr, 0f) / Math.min(maxHeat, 1000f))))), 1000f);
            }




            if(writtenHeat > 0f) {
                currentCraftTime = Math.max(defaultCraftTime / (writtenHeat / defaultHeat) - penaltyMultiplier * (writtenHeat - defaultHeat), 1f);
            }else {
                currentCraftTime = Float.MAX_VALUE;
            }

            if(efficiency > 0){

                progress += getProgressIncrease(currentCraftTime);
                warmup = Mathf.approachDelta(warmup, warmupTarget(), warmupSpeed);

                //continuously output based on efficiency
                if(outputLiquids != null){
                    float inc = getProgressIncrease(1f);
                    for(var output : outputLiquids){
                        handleLiquid(this, output.liquid, Math.min(output.amount * inc, liquidCapacity - liquids.get(output.liquid)));
                    }
                }

                if(wasVisible && Mathf.chanceDelta(updateEffectChance)){
                    updateEffect.at(x + Mathf.range(size * updateEffectSpread), y + Mathf.range(size * updateEffectSpread));
                }
            }else{
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
            }

            totalProgress += warmup * Time.delta;

            if(progress >= 1f){
                craft();
            }

            dumpOutputs();


        }

        @Override
        public float heatFrac(){
            return heat / writtenHeat;
        }

        @Override
        public float heat(){
            return heat;
        }




    }


    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);
        stats.add(Stat.output, defaultHeat, StatUnit.heatUnits);
        stats.add(Stat.maxEfficiency, maxHeat,  StatUnit.heatUnits);
    }

    @Override
    public void setBars(){
        super.setBars();

        removeBar("heat");
        addBar("heat", (ConfigureableHeatProducerBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.heat / entity.writtenHeat));
        addBar("writtenEfficiency", (ConfigureableHeatProducerBuild entity) -> new Bar(
                () -> Core.bundle.format("bar.consumerate",
                        ((1 / (entity.currentCraftTime / 60f)))),
                () -> Items.phaseFabric.color,
                entity::heatFrac
        ));
    }
}
