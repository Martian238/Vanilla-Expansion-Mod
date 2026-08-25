package VanillaExpansion.expand.world.block.units;

import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Structs;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ai.types.AssemblerAI;
import mindustry.content.Fx;
import mindustry.entities.EntityCollisions;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.graphics.Shaders;
import mindustry.io.TypeIO;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.PayloadSeq;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitAssemblerModule;
import mindustry.world.meta.BlockStatus;

import static mindustry.Vars.*;

public class ConfigurableUnitAssembler extends UnitAssembler {
    public ConfigurableUnitAssembler(String name) { super(name); }

    //TODO

    public Seq<AssemblerUnitPlan> plans = new Seq<>();

    @Override
    public void getPlanConfigs(Seq<mindustry.ctype.UnlockableContent> options){
        for(var plan : plans){
            if(!plan.unit.isBanned()){
                options.add(plan.unit);
            }
        }
    }

    public class ConfigurableUnitAssemblerBuild extends PayloadBlockBuild<Payload>{
        protected IntSeq readUnits = new IntSeq();
        //holds drone IDs that have been sent, but not synced yet - add to list as soon as possible
        protected IntSeq whenSyncedUnits = new IntSeq();

        public @Nullable Vec2 commandPos;
        public Seq<Unit> units = new Seq<>();
        public Seq<UnitAssemblerModule.UnitAssemblerModuleBuild> modules = new Seq<>();
        public PayloadSeq blocks = new PayloadSeq();
        public float progress, warmup, droneWarmup, powerWarmup, sameTypeWarmup;
        public float invalidWarmup = 0f;
        public int currentTier = 0;
        public int lastTier = -2;
        public boolean wasOccupied = false;

        public float droneProgress, totalDroneProgress;

        public Vec2 getUnitSpawn(){
            float len = tilesize * (areaSize + size)/2f;
            float unitX = x + Geometry.d4x(rotation) * len, unitY = y + Geometry.d4y(rotation) * len;
            return Tmp.v4.set(unitX, unitY);
        }

        public boolean moduleFits(Block other, float ox, float oy, int rotation){
            float
                    dx = ox + Geometry.d4x(rotation) * (other.size/2f + 0.5f) * tilesize,
                    dy = oy + Geometry.d4y(rotation) * (other.size/2f + 0.5f) * tilesize;

            Vec2 spawn = getUnitSpawn();

            if(Tile.relativeTo(ox, oy, spawn.x, spawn.y) != rotation){
                return false;
            }

            float dst = Math.max(Math.abs(dx - spawn.x), Math.abs(dy - spawn.y));
            return Mathf.equal(dst, tilesize * areaSize / 2f - tilesize/2f);
        }

        public void updateModules(UnitAssemblerModule.UnitAssemblerModuleBuild build){
            modules.addUnique(build);
            checkTier();
        }

        public void removeModule(UnitAssemblerModule.UnitAssemblerModuleBuild build){
            modules.remove(build);
            checkTier();
        }

        public void checkTier(){
            modules.sort(b -> b.tier());
            int max = 0;
            for(int i = 0; i < modules.size; i++){
                var mod = modules.get(i);
                if(mod.tier() == max || mod.tier() == max + 1){
                    max = mod.tier();
                }else{
                    //tier gap, TODO warning?
                    break;
                }
            }

            currentTier = max;
        }

        public UnitType unit(){
            return plan().unit;
        }

        public AssemblerUnitPlan plan(){
            //clamp plan pos
            return plans.get(Math.min(currentTier, plans.size - 1));
        }

        @Override
        public boolean shouldConsume(){
            //liquid is only consumed when building is being done
            return enabled && !wasOccupied && Units.canCreate(team, plan().unit) && consPayload.efficiency(this) > 0 && consItem.efficiency(this) > 0 && team.activateUnitFactories();
        }

        @Override
        public void drawSelect(){
            for(var module : modules){
                Drawf.selected(module, Pal.accent);
            }

            Drawf.dashRect(Tmp.c1.set(Pal.accent).lerp(Pal.remove, invalidWarmup), getRect(Tmp.r1, x, y, rotation));
        }

        @Override
        public void display(Table table){
            super.display(table);

            if(team != player.team()) return;

            table.row();
            table.table(t -> {
                t.left().defaults().left();

                Block prev = null;
                for(int i = 0; i < modules.size; i++){
                    var mod = modules.get(i);
                    if(prev == mod.block) continue;
                    //TODO crosses for missing reqs?
                    t.image(mod.block.uiIcon).size(iconMed).padRight(4);

                    prev = mod.block;
                }

                t.label(() -> "[accent] -> []" + unit().emoji() + " " + unit().localizedName);
            }).pad(4).padLeft(0f).fillX().left();
        }

        @Override
        public void updateTile(){
            if(!readUnits.isEmpty()){
                units.clear();
                readUnits.each(i -> {
                    var unit = Groups.unit.getByID(i);
                    if(unit != null){
                        units.add(unit);
                    }
                });
                readUnits.clear();
            }

            if(lastTier != currentTier){
                if(lastTier >= 0f){
                    progress = 0f;
                }

                lastTier =
                        lastTier == -2 ? -1 : currentTier;
            }

            //read newly synced drones on client end
            if(units.size < dronesCreated && whenSyncedUnits.size > 0){
                whenSyncedUnits.each(id -> {
                    var unit = Groups.unit.getByID(id);
                    if(unit != null){
                        units.addUnique(unit);
                    }
                });
            }

            units.removeAll(u -> !u.isAdded() || u.dead || !(u.controller() instanceof AssemblerAI));

            //unsupported
            if(!allowUpdate()){
                progress = 0f;
                units.each(Unit::kill);
                units.clear();
            }

            float powerStatus = !enabled ? 0f : power == null ? 1f : power.status;
            powerWarmup = Mathf.lerpDelta(powerStatus, powerStatus > 0.0001f ? 1f : 0f, 0.1f);
            droneWarmup = Mathf.lerpDelta(droneWarmup, units.size < dronesCreated ? powerStatus : 0f, 0.1f);
            totalDroneProgress += droneWarmup * delta();

            if(units.size < dronesCreated && enabled && (droneProgress += delta() * state.rules.unitBuildSpeed(team) * powerStatus / droneConstructTime) >= 1f){
                if(!net.client()){
                    var unit = droneType.create(team);
                    //If a unit isn't using AssemblerAI, it's bugged, likely because of an incorrect data patch or mod.
                    //In that case, just ignore it and don't spawn anything
                    if(unit.controller() instanceof AssemblerAI){
                        if(unit instanceof BuildingTetherc bt){
                            bt.building(this);
                        }
                        unit.set(x, y);
                        unit.rotation = 90f;
                        unit.add();
                        units.add(unit);
                        Call.assemblerDroneSpawned(tile, unit.id);
                    }else{
                        droneProgress = 0f;
                    }
                }
            }

            if(units.size >= dronesCreated){
                droneProgress = 0f;
            }

            Vec2 spawn = getUnitSpawn();

            if(moveInPayload() && !wasOccupied){
                yeetPayload(payload);
                payload = null;
            }

            //arrange units around perimeter
            for(int i = 0; i < units.size; i++){
                var unit = units.get(i);
                var ai = (AssemblerAI)unit.controller();

                ai.targetPos.trns(i * 90f + 45f, areaSize / 2f * Mathf.sqrt2 * tilesize).add(spawn);
                ai.targetAngle = i * 90f + 45f + 180f;
            }

            wasOccupied = checkSolid(spawn, false);
            boolean visualOccupied = checkSolid(spawn, true);
            float eff = (units.count(u -> ((AssemblerAI)u.controller()).inPosition()) / (float)dronesCreated);

            sameTypeWarmup = Mathf.lerpDelta(sameTypeWarmup, wasOccupied && !visualOccupied ? 0f : 1f, 0.1f);
            invalidWarmup = Mathf.lerpDelta(invalidWarmup, visualOccupied ? 1f : 0f, 0.1f);

            var plan = plan();

            //check if all requirements are met
            if(!wasOccupied && efficiency > 0 && Units.canCreate(team, plan.unit)){
                warmup = Mathf.lerpDelta(warmup, efficiency, 0.1f);

                if((progress += edelta() * state.rules.unitBuildSpeed(team) * eff / plan.time) >= 1f){
                    Call.assemblerUnitSpawned(tile);
                }
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.1f);
            }
        }

        public void droneSpawned(int id){
            Fx.spawn.at(x, y);
            droneProgress = 0f;
            if(net.client()){
                whenSyncedUnits.add(id);
            }
        }

        public void spawned(){
            var plan = plan();
            Vec2 spawn = getUnitSpawn();
            consume();

            var unit = plan.unit.create(team);
            if(unit.isCommandable() && commandPos != null){
                unit.command().commandPosition(commandPos);
            }
            unit.set(spawn.x + Mathf.range(0.001f), spawn.y + Mathf.range(0.001f));
            unit.rotation = rotdeg();
            var targetBuild = unit.buildOn();
            //'source' is the target build instead of this building; this is because some blocks only accept things from certain angles, and this is a non-standard payload
            var payload = new UnitPayload(unit);
            if(targetBuild != null && targetBuild.team == team && targetBuild.acceptPayload(targetBuild, payload)){
                targetBuild.handlePayload(targetBuild, payload);
            }else if(!net.client()){
                unit.add();
                Units.notifyUnitSpawn(unit);
            }

            createSound.at(spawn.x, spawn.y, 1f + Mathf.range(0.06f), createSoundVolume);

            progress = 0f;
            Fx.unitAssemble.at(spawn.x, spawn.y, rotdeg() - 90f, plan.unit);
            blocks.clear();

            Events.fire(new EventType.UnitCreateEvent(unit, this));
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input conveyors
            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }

            Draw.rect(rotation >= 2 ? sideRegion2 : sideRegion1, x, y, rotdeg());

            Draw.z(Layer.blockOver);

            payRotation = rotdeg();
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);

            Draw.rect(topRegion, x, y);

            if(isPayload()) return;

            //draw drone construction
            if(droneWarmup > 0.001f){
                Draw.draw(Layer.blockOver + 0.2f, () -> {
                    Drawf.construct(this, droneType.fullIcon, Pal.accent, 0f, droneProgress, droneWarmup, totalDroneProgress, 14f);
                });
            }

            Vec2 spawn = getUnitSpawn();
            float sx = spawn.x, sy = spawn.y;

            var plan = plan();

            //draw the unit construction as outline
            Draw.draw(Layer.blockBuilding, () -> {
                Draw.color(Pal.accent, warmup);

                Shaders.blockbuild.region = plan.unit.fullIcon;
                Shaders.blockbuild.time = Time.time;
                Shaders.blockbuild.alpha = warmup;
                //margin due to units not taking up whole region
                Shaders.blockbuild.progress = Mathf.clamp(progress + 0.05f);

                Draw.rect(plan.unit.fullIcon, sx, sy, rotdeg() - 90f);
                Draw.flush();
                Draw.color();
                Shaders.blockbuild.alpha = 1f;
            });

            Draw.reset();

            Draw.z(Layer.buildBeam);

            //draw unit silhouette
            Draw.mixcol(Tmp.c1.set(Pal.accent).lerp(Pal.remove, invalidWarmup), 1f);
            Draw.alpha(Math.min(powerWarmup, sameTypeWarmup));
            Draw.rect(plan.unit.fullIcon, spawn.x, spawn.y, rotdeg() - 90f);

            //build beams do not draw when invalid
            Draw.alpha(Math.min(1f - invalidWarmup, warmup));

            //draw build beams
            for(var unit : units){
                if(!((AssemblerAI)unit.controller()).inPosition()) continue;

                float
                        px = unit.x + Angles.trnsx(unit.rotation, unit.type.buildBeamOffset),
                        py = unit.y + Angles.trnsy(unit.rotation, unit.type.buildBeamOffset);

                Drawf.buildBeam(px, py, spawn.x, spawn.y, plan.unit.hitSize/2f);
            }

            //fill square in middle
            Fill.square(spawn.x, spawn.y, plan.unit.hitSize/2f);

            Draw.reset();

            Draw.z(Layer.buildBeam);

            float fulls = areaSize * tilesize/2f;

            //draw full area
            Lines.stroke(2f, Pal.accent);
            Draw.alpha(powerWarmup);
            Drawf.dashRectBasic(spawn.x - fulls, spawn.y - fulls, fulls*2f, fulls*2f);

            Draw.reset();

            float outSize = plan.unit.hitSize + 9f;

            if(invalidWarmup > 0){
                //draw small square for area
                Lines.stroke(2f, Tmp.c3.set(Pal.accent).lerp(Pal.remove, invalidWarmup).a(invalidWarmup));
                Drawf.dashSquareBasic(spawn.x, spawn.y, outSize);
            }

            Draw.reset();
        }

        public boolean checkSolid(Vec2 v, boolean same){
            var output = unit();
            float hsize = output.hitSize * 1.4f;
            return ((!output.flying && collisions.overlapsTile(Tmp.r1.setCentered(v.x, v.y, output.hitSize), EntityCollisions::solid)) ||
                    Units.anyEntities(v.x - hsize/2f, v.y - hsize/2f, hsize, hsize, u -> (!same || u.type != output) && !u.spawnedByCore &&
                            ((u.type.allowLegStep && output.allowLegStep) || (output.flying && u.isFlying()) || (!output.flying && u.isGrounded()))));
        }

        /** @return true if this block is ready to produce units, e.g. requirements met */
        public boolean ready(){
            return efficiency > 0 && !wasOccupied;
        }

        public void yeetPayload(Payload payload){
            var spawn = getUnitSpawn();
            blocks.add(payload.content(), 1);
            float rot = payload.angleTo(spawn);
            Fx.shootPayloadDriver.at(payload.x(), payload.y(), rot);
            Fx.payloadDeposit.at(payload.x(), payload.y(), rot, new YeetData(spawn.cpy(), payload.content()));
            Sounds.shootPayload.at(x, y, 1f + Mathf.range(0.1f), 1f);
        }

        @Override
        public BlockStatus status(){
            if(!team.activateUnitFactories()) return BlockStatus.inactive;
            return super.status();
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return progress;
            return super.sense(sensor);
        }

        @Override
        public PayloadSeq getPayloads(){
            return blocks;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            var plan = plan();
            return (this.payload == null || (source instanceof UnitAssemblerModule.UnitAssemblerModuleBuild)) &&
                    plan.requirements.contains(b -> b.item == payload.content() &&
                            blocks.get(payload.content()) < Mathf.round(b.amount * state.rules.unitCost(team)) -
                                    (source instanceof UnitAssemblerModule.UnitAssemblerModuleBuild && (this.payload != null && this.payload.contentEquals(payload)) ? 1 : 0));
        }

        @Override
        public int getMaximumAccepted(Item item){
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return plan().itemReq != null && items.get(item) < getMaximumAccepted(item) &&
                    Structs.contains(plan().itemReq, stack -> stack.item == item);
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
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
            write.b(units.size);
            for(var unit : units){
                write.i(unit.id);
            }

            blocks.write(write);
            TypeIO.writeVecNullable(write, commandPos);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            int count = read.b();
            readUnits.clear();
            for(int i = 0; i < count; i++){
                readUnits.add(read.i());
            }
            whenSyncedUnits.clear();

            blocks.read(read);
            if(revision >= 1){
                commandPos = TypeIO.readVecNullable(read);
            }
        }
    }
}
