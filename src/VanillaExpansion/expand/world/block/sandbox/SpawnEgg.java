package VanillaExpansion.expand.world.block.sandbox;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.*;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.core.UI;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Units;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.io.TypeIO;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;

import static mindustry.Vars.*;

public class SpawnEgg extends PayloadBlock {


    public TextureRegion arrowRegion;

    public @Nullable UnitType presetUnitType;



    public SpawnEgg(String name) {
        super(name);
        commandable = true;
        clearOnDoubleTap = true;
        configurable = true;
        selectionRows = selectionColumns = 8;
        rotate = true;
        targetable = false;
        solid = false;
        placeableLiquid = true;
        floating = true;
        underBullets = true;
        update = true;
        noUpdateDisabled = true;
        rotateDraw = false;
        rotateDrawEditor = false;
        rebuildable = false;
        destroyEffect = Fx.none;
        destroySoundVolume = 0f;
        baseShake = 0f;
        createRubble = false;
        drawTeamOverlay = false;




        config(UnitType.class, (SpawnEggBuild build, UnitType unit) -> {
            if (canProduce(unit) && build.unit != unit) {
                build.unit = unit;
            }
            if(build.command != null && (build.unit == null || !build.unit.commands.contains(build.command))){
                build.command = null;
            }
        });
        configClear((SpawnEggBuild build) -> {
            build.unit = null;
            build.command = null;
        });
    }

    public boolean canProduce(UnitType u){
        return true;
    }


    @Override
    public void load(){
        super.load();
        arrowRegion = Core.atlas.find(name + "-arrow");
    }




    public class SpawnEggBuild extends PayloadBlock.PayloadBlockBuild<Payload> {
        public UnitType unit;
        public Block configBlock;
        public @Nullable Vec2 commandPos;
        public @Nullable UnitCommand command;
        private boolean spawned = false;

        @Override
        public Vec2 getCommandPosition(){
            return commandPos;
        }

        @Override
        public void onCommand(Vec2 target){
            commandPos = target;
        }


        public boolean canSetCommand(){
            var output = unit;
            return output != null && output.commands.size > 1 && output.allowChangeCommands &&
                    //to avoid cluttering UI, don't show command selection for "standard" units that only have two commands.
                    !(output.commands.size == 2 && output.commands.get(1) == UnitCommand.enterPayloadCommand);
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(SpawnEgg.this, table,

                            (content.units().select(SpawnEgg.this::canProduce).as()),
                    () -> (UnlockableContent)config(), this::configure, selectionRows, selectionColumns);
            /*if(unit!=null){
                table.row();

                Table commands = new Table();
                commands.top().left();

                Runnable rebuildCommands = () -> {
                    commands.clear();
                    commands.background(null);
                    if(unit != null && canSetCommand()){
                        commands.background(Styles.black6);
                        var group = new ButtonGroup<ImageButton>();
                        group.setMinCheckCount(0);
                        int i = 0, columns = selectionColumns;
                        var list = unit.commands;

                        commands.image(Tex.whiteui, Pal.gray).height(4f).growX().colspan(columns).row();

                        for(var item : list){
                            ImageButton button = commands.button(item.getIcon(), Styles.clearNoneTogglei, 40f, () -> {
                                configure(item);
                            }).tooltip(item.localized()).group(group).get();

                            button.update(() -> button.setChecked(command == item || (command == null && unit.defaultCommand == item)));

                            if(++i % columns == 0){
                                commands.row();
                            }
                        }

                        if(list.size < columns){
                            for(int j = 0; j < (columns - list.size); j++){
                                commands.add().size(40f);
                            }
                        }
                    }
                };

                rebuildCommands.run();

                table.row();

                table.add(commands).fillX().left();
            }*/
        }

        @Override
        public Object config(){
            return unit == null ? configBlock : unit;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return false;
        }

        @Override
        public void updateTile(){

            if(presetUnitType != null){
                unit = presetUnitType;
            }

            if(unit != null && !state.isPaused() && !state.isEditor() && isValid() && enabled && !spawned){
                    UnitType spawnUnit = unit;
                    Vec2 spawnCommandPos = commandPos;
                    float spawnX = this.tile.worldx();
                    float spawnY = this.tile.worldy();
                    Team spawnTeam = this.team;
                    float spawnRot = rotdeg();

                    //Log.info(spawnUnit);

                    Unit u = spawnUnit.create(spawnTeam);
                    u.set(spawnX, spawnY);
                    Events.fire(new EventType.UnitCreateEvent(u, this, null));
                    if(!Vars.net.client()){
                        u.add();
                        Units.notifyUnitSpawn(u);
                    }
                    u.rotation = spawnRot;
                    if (u.isCommandable()) {
                        if (spawnCommandPos != null) {
                            u.command().commandPosition(spawnCommandPos);
                        }
                        u.command().command(command == null && u.type.defaultCommand != null ? u.type.defaultCommand : command);
                    }
                    spawned = true;
                    kill();
            }
            if(spawned) kill();

        }

        @Override
        public void drawTeamTop(){
            float a = Draw.getColorAlpha();
            if(teamRegions[team().id] == teamRegion) Draw.color(team().color, a);
            Draw.rect(teamRegions[team().id], x, y);
            Draw.color(1f, 1f, 1f, a);
            Draw.color();
        }

        @Override
        public void draw(){
            super.draw();
            if(unit != null) {
                if(unit.uiIcon.width >= unit.uiIcon.height) {
                    float scale = (float) unit.uiIcon.height / unit.uiIcon.width;
                    Draw.rect(unit.uiIcon, x, y, tilesize, tilesize * scale, rotdeg() - 90f);
                }else{
                    float scale = (float) unit.uiIcon.width / unit.uiIcon.height;
                    Draw.rect(unit.uiIcon, x, y, tilesize * scale, tilesize, rotdeg() - 90f);
                }
            }
        }

        @Override
        public void drawSelect(){
            Draw.z(Layer.overlayUI);
            if(commandPos != null){
                Draw.color(team.color);
                Draw.alpha(0.75f);
                Lines.stroke(2f);
                Lines.line(x, y, commandPos.x, commandPos.y);
                Lines.poly(commandPos.x, commandPos.y, 4, tilesize);
                Draw.alpha(1f);
            }
            Draw.color(Color.white);
            Draw.rect(arrowRegion, x, y, tilesize, tilesize, rotdeg());
            Draw.color();
            if(renderer.pixelate || unit == null) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            String text = team.emoji + unit.localizedName;

            l.setText(font, text, Color.white, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
            Draw.color();
            font.setColor(Color.white);
            font.draw(text, x - l.width/2f, y - tilesize/2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(1f);

            Pools.free(l);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(unit == null ? -1 : unit.id);
            TypeIO.writeVecNullable(write, commandPos);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            unit = Vars.content.unit(read.s());
            if(revision >= 1){
                commandPos = TypeIO.readVecNullable(read);
            }
        }
    }
}
