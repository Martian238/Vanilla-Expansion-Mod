package VanillaExpansion.expand.world.block.sandbox;

import VanillaExpansion.expand.world.block.liquid.LiquidSorter;
import arc.Core;
import arc.Events;
import arc.Input;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.io.TypeIO;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.environment.EmptyFloor;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;

import static mindustry.Vars.*;

public class SpawnerBlock extends SpawnEgg{
    public SpawnerBlock(String name) {
        super(name);

        rebuildable = true;
        destroyEffect = Fx.dynamicExplosion;
        destroySoundVolume = 1f;
        baseShake = 3f;
        createRubble = true;
        drawTeamOverlay = true;
        targetable = true;
        solid = true;
        solidifes = true;
        underBullets = false;

        config(UnitType.class, (SpawnerBuild build, UnitType unit) -> {
            if (canProduce(unit) && build.unit != unit) {
                build.unit = unit;
                build.timer = 0f;
            }
            if(build.command != null && (build.unit == null || !build.unit.commands.contains(build.command))){
                build.command = null;
            }
        });
        configClear((SpawnerBuild build) -> {
            build.unit = null;
            build.command = null;
            build.timer = 0f;
        });

        config(Float.class, (SpawnerBuild tile, Float imin) -> {
            if(imin > 0) tile.intervalMin = imin * 60f;
            tile.timer = 0f;
            tile.timerTarget = -1f;
        });
        config(Double.class, (SpawnerBuild tile, Double imax) -> {
            if(imax > 0) tile.intervalMax = (float) (imax * 60f);
            tile.timer = 0f;
            tile.timerTarget = -1f;
        });
        config(Integer.class, (SpawnerBuild tile, Integer amount) -> {
            if(amount >= 0) tile.amount = amount;
        });
        config(String.class, (SpawnerBuild tile, String radius) -> {
            if(Strings.parseFloat(radius, 0f) >= 0) tile.radius = Strings.parseFloat(radius, 0f) * tilesize;
        });
    }
    public int maxTextLength = 40;
    public int maxNewlines = 1;

    public Effect spawnSmokeEffect = Fx.smokeCloud;
    public Effect spawnEffect = Fx.spawn;
    public Effect FireEffect = Fx.fireHit;

    public TextureRegion bottomRegion, shadowRegion;

    @Override
    public void load(){
        super.load();
        bottomRegion = Core.atlas.find(name + "-bottom");
        shadowRegion = Core.atlas.find("ve-circle-soft");
    }


    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (SpawnerBuild e) -> new Bar("bar.progress", Pal.ammo, () -> e.timerTarget > 0 ? Mathf.clamp(e.timer / e.timerTarget) : 0));

        addBar("units", (SpawnerBuild e) ->
                new Bar(
                        () -> e.unit == null ? "[lightgray]" + Iconc.cancel :
                                Core.bundle.format("bar.unitcap",
                                        Fonts.getUnicodeStr(e.unit.name),
                                        e.team.data().countType(e.unit),
                                        e.unit == null ? Units.getStringCap(e.team) : (e.unit.useUnitCap ? Units.getStringCap(e.team) : "∞")
                                ),
                        () -> Pal.power,
                        () -> e.unit == null ? 0f : (e.unit.useUnitCap ? (float)e.team.data().countType(e.unit) / Units.getCap(e.team) : 1f)
                ));
    }



    public class SpawnerBuild extends PayloadBlock.PayloadBlockBuild<Payload> {
        public UnitType unit;
        public Block configBlock;
        public @Nullable Vec2 commandPos;
        public @Nullable UnitCommand command;
        public float intervalMin = 600f;
        public float intervalMax = 2400f;
        public int amount = 4;
        public float rotateSpeedMin = 3f;
        public float rotateSpeedMax = 25f;
        public float radius = 64f;

        private float timer = 0f;
        private float timerTarget = -1f;
        private float rotateSpeed = 3f;
        private float currentRotation = 0f;

        public StringBuilder intervalMinMessage = new StringBuilder();
        public StringBuilder intervalMaxMessage = new StringBuilder();
        public StringBuilder amountMessage = new StringBuilder();
        public StringBuilder radiusMessage = new StringBuilder();

        private TextButton[] fluxButton = new TextButton[4];

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(SpawnerBlock.this, table,

                    (content.units().select(SpawnerBlock.this::canProduce).as()),
                    () -> (UnlockableContent) config(), this::configure, selectionRows, selectionColumns);
            inputButton(table,0, Float.class, intervalMinMessage, "spawner.intervalmin", intervalMin / 60f, "spawner.intervalmin.edit");
            inputButton(table,1, Double.class, intervalMaxMessage, "spawner.intervalmax", intervalMax / 60f, "spawner.intervalmax.edit");
            inputButton(table,2, Integer.class, amountMessage, "spawner.amount", amount, "spawner.amount.edit");
            inputButton(table,3, String.class, radiusMessage, "spawner.radius", radius / tilesize, "spawner.radius.edit");
        }

        public void inputButton(Table table, int num, Class<?> type, StringBuilder message,
                                String bundleButton, Object buttonValue, String bundleTitle){
            table.row();
            table.image().color(Pal.gray).height(2f).growX().padBottom(4f).row();
            fluxButton[num] = new TextButton(Core.bundle.format(bundleButton, buttonValue), Styles.flatTogglet);
            fluxButton[num].changed(() -> {
                        if (mobile) {
                            var contents = message;
                            Core.input.getTextInput(new Input.TextInput() {{
                                text = contents.toString();
                                multiline = true;
                                maxLength = maxTextLength;
                                accepted = str -> {
                                    if (!str.contentEquals(contents)) {
                                        if(type == Float.class || type == Double.class) {
                                            if(type == Double.class){
                                                configure((double) Strings.parseFloat(str, 0f));
                                            }else{
                                                configure(Strings.parseFloat(str, 0f));
                                            }
                                        }else if(type == Integer.class) {
                                            configure(Strings.parseInt(str, 0));
                                        }else{
                                            configure(str);
                                        }
                                    }
                                };
                            }});
                        } else {
                            BaseDialog dialog = new BaseDialog("@" + bundleTitle);
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
                                if (!a.getText().contentEquals(message)) {
                                    String str = a.getText();
                                    if(type == Float.class || type == Double.class) {
                                        if(type == Double.class){
                                            configure((double) Strings.parseFloat(str, 0f));
                                        }else{
                                            configure(Strings.parseFloat(str, 0f));
                                        }
                                    }else if(type == Integer.class) {
                                        configure(Strings.parseInt(str, 0));
                                    }else{
                                        configure(str);
                                    }
                                }
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
                        fluxButton[num].setChecked(false);
                    }
            );
            table.add(fluxButton[num]).height(40f).growX();
        }

        public void calculate(){
            if(!intervalMinMessage.isEmpty()) {
                float intervalMinFloat = Strings.parseFloat(intervalMinMessage.toString(), 0f);
                if (intervalMinFloat > 0f) intervalMin = intervalMinFloat * 60f;
            }
            if(!intervalMaxMessage.isEmpty()) {
                float intervalMaxFloat = Strings.parseFloat(intervalMaxMessage.toString(), 0f);
                if (intervalMaxFloat > 0f) intervalMax = intervalMaxFloat * 60f;
            }
            if(!amountMessage.isEmpty()) {
                int amountInteger = Strings.parseInt(amountMessage.toString(), 0);
                if (amountInteger >= 0) amount = amountInteger;
            }
            if(!radiusMessage.isEmpty()) {
                float radiusFloat = Strings.parseFloat(radiusMessage.toString(), 0f);
                if (radiusFloat >= 0f) radius = radiusFloat * tilesize;
            }
        }

        public void doSpawn(){
            if(amount > 0){
                UnitType spawnUnit = unit;
                Vec2 spawnCommandPos = commandPos;
                Team spawnTeam = this.team;
                float spawnRotation = rotdeg();
                for(int i = 0; i < amount; i++){
                    float spawnAngle = Mathf.range(180f);
                    float spawnLength = Mathf.range(radius);
                    float spawnX = x + spawnLength * Mathf.cosDeg(spawnAngle);
                    float spawnY = y + spawnLength * Mathf.sinDeg(spawnAngle);
                    if(canSpawnUnit(spawnUnit,  spawnX, spawnY)){
                        if(spawnTeam.data().countType(spawnUnit) < Units.getCap(spawnTeam) || !spawnUnit.useUnitCap) {
                            spawnSmokeEffect.at(spawnX, spawnY);
                            spawnEffect.at(spawnX, spawnY);
                            Unit u = spawnUnit.create(spawnTeam);
                            u.set(spawnX, spawnY);
                            Events.fire(new EventType.UnitCreateEvent(u, this, null));
                            if (!Vars.net.client()) {
                                u.add();
                                Units.notifyUnitSpawn(u);
                            }
                            u.rotation = spawnRotation;
                            if (u.isCommandable()) {
                                if (spawnCommandPos != null) {
                                    u.command().commandPosition(spawnCommandPos);
                                }
                                u.command().command(command == null && u.type.defaultCommand != null ? u.type.defaultCommand : command);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void updateTile(){
            //calculate();
            if(timerTarget <= 0f){
                timerTarget = Math.abs(Mathf.range(Math.min(intervalMin, intervalMax), Math.max(intervalMin, intervalMax)));
            }
            if(unit != null && enabled && !state.isEditor() && !state.isPaused()){
                if(timer < timerTarget) timer++;
                if(timer >= timerTarget && timerTarget > 0f && (team.data().countType(unit) < Units.getCap(team) || !unit.useUnitCap)){
                    doSpawn();
                    timer = 0;
                    timerTarget = -1f;
                }
            }
            if(enabled && timerTarget > 0f && !state.isEditor()){
                rotateSpeed = Mathf.lerp(rotateSpeedMin, rotateSpeedMax, Mathf.clamp(timer / timerTarget));
                if(unit != null && Mathf.chance(Mathf.clamp(0.2f * timer / timerTarget, 0.005f, 0.2f))){
                    FireEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }else{
                rotateSpeed = rotateSpeedMin;
            }
            currentRotation += rotateSpeed;
            currentRotation = angleStandardize(currentRotation);
            if(unit == null || !enabled){
                timer = 0f;
            }
        }

        @Override
        public void draw(){
            super.draw();
            Draw.z(Layer.blockUnder - 0.1f);
            Draw.rect(bottomRegion, x, y);
            Draw.color(Color.black);
            Draw.alpha(0.3f);
            Draw.rect(shadowRegion, x, y, size * tilesize, size * tilesize);
            if(unit != null){
                Draw.alpha(1f);
                Draw.color(Color.white);
                Draw.z(Layer.blockUnder);
                if(unit.fullIcon.width >= unit.fullIcon.height){
                    float width = Math.min(size * tilesize * 0.5f, unit.fullIcon.width / 4f);
                    float scale = (float) unit.fullIcon.height / unit.fullIcon.width;
                    Draw.rect(unit.fullIcon, x, y, width, width * scale, currentRotation);
                }else{
                    float height = Math.min(size * tilesize * 0.5f, unit.fullIcon.height / 4f);
                    float scale = (float) unit.fullIcon.width / unit.fullIcon.height;
                    Draw.rect(unit.fullIcon, x, y, height * scale, height, currentRotation);
                }
            }
            Draw.alpha(1f);
            Draw.color();
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
            if(radius > 0f){
                Draw.color(team.color);
                Draw.alpha(0.75f);
                Lines.stroke(2f);
                Lines.circle(x, y, radius);
                Draw.alpha(1f);
            }
            if(unit != null && enabled && !state.isEditor() && timerTarget > 0f){
                Draw.color(team.color);
                Draw.alpha(0.25f);
                Fill.rect(x, y + size * tilesize * 0.75f, size * tilesize, size);
                Draw.alpha(1f);
                Fill.rect(x - size * tilesize * 0.5f * Mathf.clamp(1 - timer / timerTarget), y + size * tilesize * 0.75f, size * tilesize * Mathf.clamp(timer / timerTarget), size);
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

            String text = team.emoji + unit.localizedName + (timerTarget > 0 ? Core.bundle.format("spawner.timer", (int) Math.floor((timerTarget - timer) / 60f)) : "");

            l.setText(font, text, Color.white, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize * size/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
            Draw.color();
            font.setColor(Color.white);
            font.draw(text, x - l.width/2f, y - tilesize * size/2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(1f);

            Pools.free(l);
        }


        public boolean canSpawnUnit(UnitType u, float x, float y) {
            Floor floor = world.floorWorld(x, y);
            Tile tile = world.tileWorld(x, y);
            Building build = world.buildWorld(x, y);
            if(u.flying || u.canBoost) {
                return true;
            }else if(u.naval){
                if(floor != null){
                    if(floor.isLiquid){
                        if(tile != null){
                            if(!tile.block().solid){
                                if(build != null){
                                    return !build.block.solid;
                                }else return true;
                            }else return false;
                        }else{
                            if(build != null){
                                return !build.block.solid;
                            }else return true;
                        }
                    }else return false;
                }else return false;
            }else{
                if((u.constructor instanceof LegsUnit && u.allowLegStep) || (u.constructor instanceof CrawlUnit)){
                    if(floor != null){
                        if(floor.placeableOn && !(floor instanceof EmptyFloor)){
                            if(tile != null){
                                return !tile.block().solid;
                            }else return true;
                        }else return false;
                    }else return false;
                }else{
                    if(floor != null){
                        if(floor.placeableOn && !(floor instanceof EmptyFloor)){
                            if(tile != null){
                                if(!tile.block().solid){
                                    if(build != null){
                                        return !build.block.solid;
                                    }else return true;
                                }else return false;
                            }else{
                                if(build != null){
                                    return !build.block.solid;
                                }else return true;
                            }
                        }else return false;
                    }else return false;
                }
            }
        }

        public float angleStandardize(Float a){
            if(a >= 360) return a - 360;
            return a < 0 ? a + 360f : a;
        }


        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(unit == null ? -1 : unit.id);
            TypeIO.writeVecNullable(write, commandPos);
            write.f(intervalMin);
            write.d((double)intervalMax);
            write.i(amount);
            write.str(String.valueOf(radius));
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            unit = Vars.content.unit(read.s());
            if(revision >= 1){
                commandPos = TypeIO.readVecNullable(read);
            }
            if(revision >= 2){
                intervalMin = read.f();
                intervalMax = (float) read.d();
                amount = read.i();
                radius = Strings.parseFloat(read.str(), 0f);
            }else{
                intervalMin = 600f;
                intervalMax = 2400f;
                amount = 4;
                radius = 64f;
            }
        }

        @Override
        public Vec2 getCommandPosition(){
            return commandPos;
        }

        @Override
        public void onCommand(Vec2 target){
            commandPos = target;
        }

        @Override
        public Object config(){
            return unit == null ? configBlock : unit;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return false;
        }
    }
}
