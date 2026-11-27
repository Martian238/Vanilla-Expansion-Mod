package VanillaExpansion.expand.type.unit;
import VanillaExpansion.EntityRegister;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.*;
import mindustry.entities.units.StatusEntry;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.input.InputHandler;
import mindustry.mod.Mods;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.meta.Env;
import static mindustry.Vars.*;
public class HyperUnit extends PayloadUnit {
    private boolean multiMod = false, modFound = false;
    private float statTimer = 0f;
    public static PayloadUnit create(){
        return new HyperUnit();
    }
    private void findOtherModEnemies(){
        multiMod = false;
        for(Unit u : Groups.unit){
            if (!u.type.name.startsWith("ve-") && !u.type.isVanilla() && u.team != team){
                multiMod = true;
            }
        }
        for(Building b : Groups.build){
            if (!b.block.name.startsWith("ve-") && !b.block.isVanilla() && b.team != team){
                multiMod = true;
            }
        }
    }
    private void findMods(boolean log){
        multiMod = false;
        if(!mobile) {
            if(log)Log.info("Finding mods...");
            Seq<Mods.LoadedMod> mods = Vars.mods.getMods();
            Seq<String> multiModList = new Seq<>();
            multiModList.clear();
            if (mods != null) {
                for (Mods.LoadedMod mod : mods) {
                    if (mod.name.equals("ve") || mod.name.equals("vanilla-expansion") || mod.dependencies.contains(m -> m.name.equals("ve")) || mod.softDependencies.contains(m -> m.name.equals("ve"))) {
                        continue;
                    } else if (!mod.meta.hidden && mod.enabled()) {
                        multiMod = true;
                        multiModList.add(mod.name);
                    }
                }
            }
            if (multiMod) {
                if(log)Log.info("Other mods found: " + multiModList);
            } else {
                if(log)Log.info("Other mods not found");
            }
        }
        modFound = true;
    }

    private void clearEnemy(){
        Log.info("Clearing enemies");
        int count1 = 0;
        int count2 = 0;
        Seq<String> killNames = new Seq<>();
        for(Bullet ebl : Groups.bullet){
            if(ebl.owner != null && ebl.team != team){
                if(ebl.owner instanceof Unit eu){
                    if(eu.team != team && !eu.type.name.startsWith("ve-") && !eu.type.isVanilla()){
                        killNames.add(eu.type.name);
                        if(Groups.unit.contains(gu -> gu == eu)) Groups.unit.remove(eu);
                        eu.x(999999999);
                        eu.y(999999999);
                        eu.kill();eu.remove();
                        ebl.remove();
                        count1++;
                    }
                }
                if(ebl.owner instanceof Building eb){
                    if(eb.team != team && !eb.block.name.startsWith("ve-") && !eb.block.isVanilla()){
                        killNames.add(eb.block.name);
                        eb.tile.setAir();
                        try{eb.kill();eb.remove();}catch(Exception ignored){}
                        ebl.remove();
                        count2++;
                    }
                }
            }else if(ebl.team != team){
                ebl.remove();
            }
        }
        for(Unit eu : Groups.unit){
            if(eu.team != team && !eu.type.name.startsWith("ve-") && !eu.type.isVanilla()){
                killNames.add(eu.type.name);
                if(Groups.unit.contains(gu -> gu == eu)) Groups.unit.remove(eu);
                eu.x(999999999);
                eu.y(999999999);
                eu.kill();eu.remove();
                count1++;
            }
        }
        for(Building eb : Groups.build){
            if(eb.team != team && !eb.block.name.startsWith("ve-") && !eb.block.isVanilla() && eb.team != Team.derelict){
                killNames.add(eb.block.name);
                eb.tile.setAir();
                try{eb.kill();eb.remove();}catch(Exception ignored){}
                count2++;
            }
        }
        Log.info("Removed "+count1+" units, "+count2+" buildings");
        if(!Groups.unit.contains(u -> u == this)){
            Groups.unit.add(this);
            Log.info("Re-added into Groups.unit");
        }
    }
    private UnitController protectedController;
    private boolean mobile = false;
    private float updateTimer = 0;
    private boolean controllerGot = false;
    public boolean publicMultiMod = false;
    public boolean tryRecovery = false;
    @Override
    public int classId() {
        return EntityRegister.getID(getClass());
    }
    private boolean switchToEventMode = false;
    @Override
    public void update(){
        if(!switchToEventMode){
            actualUpdate();
        }
    }
    protected void actualUpdate(){
        if(!modFound){
            mobile = Vars.mobile;
            try {
                findMods(true);
            } catch (Exception e) {
                Log.info("Failed to find mods. Switching to mobile testing mode.");
                mobile = true;
            }
        }
        if(multiMod){
            if(dead){
                dead = false;
                healthSet(maxHealth);
            }
            if(statuses.contains(s -> statusB(s))){
                statuses.clear();
            }
            if(!switchToEventMode){
                switchToEventMode = true;
                UnitEventUpdateManager.add(this);
                Log.info("Switched to event updating mode.");
            }
        }
        statTimer++;
        if(statTimer >= 5) {
            keepStats();
            if(mobile) {
                findOtherModEnemies();
            }
            statTimer = 0f;
            if(protectedController == null){
                protectedController = this.controller;
                controllerGot = true;
                Log.info("Controller saved: "+protectedController);
            }
            /*
            if(!isShooting() && !isPlayer()){
                if(x < 0) x = 0;
                if(x > state.map.width * tilesize) x = state.map.width * tilesize;
                if(y < 0) y = 0;
                if(y > state.map.height * tilesize) y = state.map.height * tilesize;
            }
            */
            for(Bullet b : Groups.bullet){
                if(b.team != team && !isBulletVanilla(b)){
                    try{
                        b.x(99999999f);
                        b.y(99999999f);
                    }catch(Exception ignored){}
                    b.remove();
                }
            }
        }
        if(multiMod){
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            if(controller() == null || !controller().isValidController()){
                try {
                    resetController();
                }catch (Exception e){
                    try {
                        Core.app.exit();
                    }catch (Exception e2){
                        while(true){}
                    }
                }
            }
        }

        if(switchToEventMode) {
            overrideUpdate();
        }else{
            super.update();
        }

        updateTimer = 0;

        if(tryRecovery){
            tryRecovery = false;
        }
    }
    private void overrideUpdate(){
        if (!net.client() || isLocal()) {
            float px = x;
            float py = y;
            move(vel.x * Time.delta, vel.y * Time.delta);
            if (Mathf.equal(px, x)) vel.x = 0;
            if (Mathf.equal(py, y)) vel.y = 0;
            vel.scl(Math.max(1.0F - drag * Time.delta, 0));
        }
        updateBuildLogic();
        hitTime -= Time.delta / hitDuration;
        stack.amount = Mathf.clamp(stack.amount, 0, itemCapacity());
        itemTime = Mathf.lerpDelta(itemTime, Mathf.num(hasItem()), 0.05F);
        //miner
        if (mineTile != null) {
            Building core = closestCore();
            Item item = getMineResult(mineTile);
            if (core != null && item != null && !acceptsItem(item) && within(core, mineTransferRange) && !offloadImmediately()) {
                int accepted = core.acceptStack(item(), stack().amount, this);
                if (accepted > 0) {
                    Call.transferItemTo(this, item(), accepted, mineTile.worldx() + Mathf.range(tilesize / 2.0F), mineTile.worldy() + Mathf.range(tilesize / 2.0F), core);
                    clearItem();
                }
            }
            if ((!net.client() || isLocal()) && !validMine(mineTile)) {
                mineTile = null;
                mineTimer = 0.0F;
            } else if (mining() && item != null) {
                mineTimer += Time.delta * type.mineSpeed * state.rules.unitMineSpeed(team());
                if (Mathf.chance(0.06 * Time.delta)) {
                    Fx.pulverizeSmall.at(mineTile.worldx() + Mathf.range(tilesize / 2.0F), mineTile.worldy() + Mathf.range(tilesize / 2.0F), 0.0F, item.color);
                }
                if (mineTimer >= 50.0F + (type.mineHardnessScaling ? item.hardness * 15.0F : 15.0F)) {
                    mineTimer = 0;
                    if (state.rules.sector != null && team() == state.rules.defaultTeam)
                        state.rules.sector.info.handleProduction(item, 1);
                    if (core != null && within(core, mineTransferRange) && core.acceptStack(item, 1, this) == 1 && offloadImmediately()) {
                        if (item() == item && !net.client()) addItem(item);
                        Call.transferItemTo(this, item, 1, mineTile.worldx() + Mathf.range(tilesize / 2.0F), mineTile.worldy() + Mathf.range(tilesize / 2.0F), core);
                    } else if (acceptsItem(item)) {
                        InputHandler.transferItemToUnit(item, mineTile.worldx() + Mathf.range(tilesize / 2.0F), mineTile.worldy() + Mathf.range(tilesize / 2.0F), this);
                    } else {
                        mineTile = null;
                        mineTimer = 0.0F;
                    }
                }
                if (!headless) {
                    control.sound.loop(type.mineSound, this, type.mineSoundVolume);
                }
            }
        }
        //payload
        if (payloadPower != null) {
            payloadPower.clear();
        }
        for (Payload pay : payloads) {
            if (pay instanceof BuildPayload pb && pb.build.power != null) {
                if (payloadPower == null) payloadPower = new PowerGraph(false);
                pb.build.power.graph = null;
                payloadPower.add(pb.build);
            }
        }
        if (payloadPower != null) {
            payloadPower.update();
        }
        for (Payload pay : payloads) {
            pay.set(x, y, rotation);
            pay.update(this, null);
        }
        //shield
        shieldAlpha -= Time.delta / 15.0F;
        if (shieldAlpha < 0) shieldAlpha = 0.0F;
        //status
        mindustry.world.blocks.environment.Floor floor = floorOn();
        if (isGrounded() && !type.hovering) {
            apply(floor.status, floor.statusDuration);
        }
        applied.clear();
        armorOverride = -1.0F;
        speedMultiplier = damageMultiplier = healthMultiplier = reloadMultiplier = buildSpeedMultiplier = dragMultiplier = 1.0F;
        disarmed = false;
        if (!statuses.isEmpty()) {
            int index = 0;
            while (index < statuses.size) {
                StatusEntry entry = statuses.get(index++);
                entry.time = Math.max(entry.time - Time.delta, 0);
                if (entry.effect == null || (entry.time <= 0 && !entry.effect.permanent)) {
                    if (entry.effect != null) {
                        entry.effect.onRemoved(this);
                    }
                    Pools.free(entry);
                    index--;
                    statuses.remove(index);
                } else {
                    applied.set(entry.effect.id);
                    if (entry.effect.dynamic) {
                        if(entry.speedMultiplier >= 1) speedMultiplier *= entry.speedMultiplier;
                        if(entry.healthMultiplier >= 1) healthMultiplier *= entry.healthMultiplier;
                        if(entry.damageMultiplier >= 1) damageMultiplier *= entry.damageMultiplier;
                        if(entry.reloadMultiplier >= 1) reloadMultiplier *= entry.reloadMultiplier;
                        if(entry.buildSpeedMultiplier >= 1) buildSpeedMultiplier *= entry.buildSpeedMultiplier;
                        if (entry.armorOverride >= armor) armorOverride = entry.armorOverride;
                    } else {
                        if(entry.effect.speedMultiplier >= 1) speedMultiplier *= entry.effect.speedMultiplier;
                        if(entry.effect.healthMultiplier >= 1) healthMultiplier *= entry.effect.healthMultiplier;
                        if(entry.effect.damageMultiplier >= 1) damageMultiplier *= entry.effect.damageMultiplier;
                        if(entry.effect.reloadMultiplier >= 1) reloadMultiplier *= entry.effect.reloadMultiplier;
                        if(entry.effect.buildSpeedMultiplier >= 1) buildSpeedMultiplier *= entry.effect.buildSpeedMultiplier;
                    }
                    disarmed = false;
                    entry.effect.update(this, entry);
                }
            }
        }
        //sync
        if ((Vars.net.client() && !isLocal()) || isRemote()) {
            interpolate();
        }
        //unit
        if (type.bounded) {
            float bot = 0.0F;
            float left = 0.0F;
            float top = world.unitHeight();
            float right = world.unitWidth();
            if (state.rules.limitMapArea && !team.isAI()) {
                bot = state.rules.limitY * tilesize;
                left = state.rules.limitX * tilesize;
                top = state.rules.limitHeight * tilesize + bot;
                right = state.rules.limitWidth * tilesize + left;
            }
            if (!net.client() || isLocal()) {
                float dx = 0.0F;
                float dy = 0.0F;
                if (x < left) dx += (-(x - left) / warpDst);
                if (y < bot) dy += (-(y - bot) / warpDst);
                if (x > right - tilesize) dx -= (x - (right - tilesize)) / warpDst;
                if (y > top - tilesize) dy -= (y - (top - tilesize)) / warpDst;
                velAddNet(dx * Time.delta, dy * Time.delta);
                float margin = tilesize * 1.0F;
                x = Mathf.clamp(x, left - margin, right - tilesize + margin);
                y = Mathf.clamp(y, bot - margin, top - tilesize + margin);
            }
        }
        Tile tile = tileOn();
        if (wasHealed && healTime <= -1.0F) {
            healTime = 1.0F;
        }
        healTime -= Time.delta / 20.0F;
        wasHealed = false;
        if (!headless) {
            control.sound.loop(type.loopSound, this, type.loopSoundVolume);
            if (type.moveSound != Sounds.none) {
                float progress = Mathf.clamp(vel.len() / type.speed);
                float pitch = Mathf.lerp(type.moveSoundPitchMin, type.moveSoundPitchMax, progress);
                control.sound.loop(type.moveSound, this, type.moveSoundVolume * progress, pitch);
            }
        }
        for (Ability a : abilities) {
            a.update(this);
        }
        if (trail != null) {
            trail.length = type.trailLength;
            float scale = type.useEngineElevation ? elevation : 1.0F;
            float offset = type.engineOffset / 2.0F + type.engineOffset / 2.0F * scale;
            float cx = x + Angles.trnsx(rotation + 180, offset);
            float cy = y + Angles.trnsy(rotation + 180, offset);
            trail.update(cx, cy);
        }
        drag = type.drag * (isGrounded() ? (floorOn().dragMultiplier) : 1.0F) * dragMultiplier * state.rules.dragMultiplier;
        if (team != state.rules.waveTeam && state.hasSpawns() && (!net.client() || isLocal()) && hittable()) {
            float relativeSize = state.rules.dropZoneRadius + hitSize / 2.0F + 1.0F;
            for (Tile spawn : spawner.getSpawns()) {
                if (within(spawn.worldx(), spawn.worldy(), relativeSize)) {
                    velAddNet(Tmp.v1.set(this).sub(spawn.worldx(), spawn.worldy()).setLength(0.1F + 1.0F - dst(spawn) / relativeSize).scl(0.45F * Time.delta));
                }
            }
        }
        if (tile != null && tile.build != null) {
            tile.build.unitOnAny(this);
        }
        if (!net.client() && shouldUpdateController()) {
            if(controller.getClass() != CommandAI.class) {
                controller = new CommandAI();
            }
            controller.updateUnit();
        }
        if (!controller.isValidController()) {
            controller = new CommandAI();
        }
    }
    @Override
    public void draw() {
        if(!switchToEventMode) actualDraw();
    }
    public void actualDraw() {
        if(!state.isEditor() && !state.isPaused()){
            updateTimer++;
            if(updateTimer > 60f && !net.client()){
                try {
                    actualUpdate();
                }catch (Exception e3){
                    while(true){
                        Core.app.exit();
                    }
                }
            }
        }
        if(controllerGot && multiMod && controller != protectedController){
            //controller = protectedController;
            //Log.info("Controller protection test: in draw()");
        }
        if(multiMod){
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Draw.color(Color.red);
            Lines.stroke(3f);
            Lines.arc(x, y, hitSize, 0.25f, Time.time * 0.2f);
            Lines.arc(x, y, hitSize, 0.25f, Time.time * 0.2f + 120f);
            Lines.arc(x, y, hitSize, 0.25f, Time.time * 0.2f - 120f);
            Draw.color();
        }
        publicMultiMod = multiMod;
        super.draw();
    }
    @Override
    public boolean collides(Hitboxc other) {
        if(multiMod && other instanceof Bullet b){
            if(isBulletVanilla(b)) {
                if(b.damage() >= health * 0.25f){
                    b.x(99999999f);
                    b.y(99999999f);
                    b.remove();
                    healthSet(maxHealth);
                    return false;
                }
            }else{
                b.x(99999999f);
                b.y(99999999f);
                b.remove();
                return false;
            }
        }
        return super.collides(other);
    }
    private boolean isBulletVanilla(Bullet b){
        boolean c = false;
        if(b.type.getClass() == BulletType.class) c = true;
        else if(b.type.getClass() == ArtilleryBulletType.class) c = true;
        else if(b.type.getClass() == BasicBulletType.class) c = true;
        else if(b.type.getClass() == BombBulletType.class) c = true;
        else if(b.type.getClass() == ContinuousLaserBulletType.class) c = true;
        else if(b.type.getClass() == ContinuousFlameBulletType.class) c = true;
        else if(b.type.getClass() == EmpBulletType.class) c = true;
        else if(b.type.getClass() == ExplosionBulletType.class) c = true;
        else if(b.type.getClass() == FlakBulletType.class) c = true;
        else if(b.type.getClass() == LaserBoltBulletType.class) c = true;
        else if(b.type.getClass() == LaserBulletType.class) c = true;
        else if(b.type.getClass() == LightningBulletType.class) c = true;
        else if(b.type.getClass() == LiquidBulletType.class) c = true;
        else if(b.type.getClass() == MassDriverBolt.class) c = true;
        else if(b.type.getClass() == MissileBulletType.class) c = true;
        else if(b.type.getClass() == PointBulletType.class) c = true;
        else if(b.type.getClass() == PointLaserBulletType.class) c = true;
        else if(b.type.getClass() == SapBulletType.class) c = true;
        else if(b.type.getClass() == ShrapnelBulletType.class) c = true;
        else if(b.type.getClass() == SpaceLiquidBulletType.class) c = true;
        return c;
    }
    @Override
    public boolean killable() {
        return type.killable(this) && !multiMod;
    }
    @Override
    public boolean shouldUpdateController() {
        return true;
    }
    @Override
    public int cap() {
        return multiMod ? Integer.MAX_VALUE : Units.getCap(team);
    }
    @Override
    public UnitController controller(){
        return controllerGot && multiMod? protectedController : controller;
    }
    @Override
    public void controller(UnitController next){
        Log.info("Method: controller(next) with multiMod: "+multiMod);
        if(multiMod && !tryRecovery){
            return;
        }
        super.controller(next);
    }
    @Override
    public void resetController(){
        Log.info("Method: resetController() with multiMod: "+multiMod);
        if(protectedController != null && multiMod){
            controller = protectedController;
            return;
        }
        super.resetController();
    }
    @Override
    public void set(Position pos) {
        Log.info("Method: set(pos) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.set(pos);
    }
    @Override
    public void set(float x, float y) {
        Log.info("Method: set(x, y) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.set(x, y);
    }
    @Override
    public void set(UnitType def, UnitController controller) {
        Log.info("Method: set(def, controller) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.set(def, controller);
    }
    @Override
    public void setType(UnitType type) {
        Log.info("Method: setType(type) with multiMod: "+multiMod);
        if(multiMod){
            return;
        }
        super.setType(type);
    }
    private void keepStats(){
        maxHealth = type.health = 190000f;
        drag = type.drag = 0.025f;
        type.speed = 1.5f;
        type.rotateSpeed = 0.5f;
        type.canAttack = true;
        armor = type.armor = 32f;
        type.envEnabled = Env.any;
        type.envDisabled = Env.none;
        type.flying = true;
        type.bounded = false;
    }
    //a
    private boolean statusB(StatusEntry s){
        if (s.effect == StatusEffects.boss) {
            if(StatusEffects.boss.damageMultiplier < 1.3f) StatusEffects.boss.damageMultiplier = 1.3f;
            if(StatusEffects.boss.healthMultiplier < 1.5f) StatusEffects.boss.healthMultiplier = 1.5f;
            if(StatusEffects.boss.speedMultiplier < 1f) StatusEffects.boss.speedMultiplier = 1f;
            if(StatusEffects.boss.damage > 0f) StatusEffects.boss.damage = 0f;
            if(StatusEffects.boss.dragMultiplier != 1f) StatusEffects.boss.dragMultiplier = 1f;
            if(StatusEffects.boss.reloadMultiplier < 1f) StatusEffects.boss.reloadMultiplier = 1f;
            if(StatusEffects.boss.disarm) StatusEffects.boss.disarm = false;
        }
        if (s.effect == StatusEffects.invincible) {
            if(StatusEffects.invincible.healthMultiplier < 1f) StatusEffects.invincible.healthMultiplier = Float.POSITIVE_INFINITY;
            if(StatusEffects.invincible.speedMultiplier < 1f) StatusEffects.invincible.speedMultiplier = 1f;
            if(StatusEffects.invincible.damageMultiplier < 1f) StatusEffects.invincible.damageMultiplier = 1f;
            if(StatusEffects.invincible.reloadMultiplier < 1f) StatusEffects.invincible.reloadMultiplier = 1f;
            if(StatusEffects.invincible.dragMultiplier != 1f) StatusEffects.invincible.dragMultiplier = 1f;
            if(StatusEffects.invincible.damage > 0f) StatusEffects.invincible.damage = 0f;
            if(StatusEffects.invincible.disarm) StatusEffects.invincible.disarm = false;
        }
        if (s.effect.disarm) return true;
        if (!s.effect.name.startsWith("ve-") && !s.effect.isVanilla()) {
            if (s.effect.healthMultiplier < 1f) return true;
            if (s.effect.reloadMultiplier < 1f) return true;
            if (s.effect.speedMultiplier < 1f) return true;
            if (s.effect.damageMultiplier < 1f) return true;
            if (s.effect.dragMultiplier != 1f) return true;
            if (s.effect.damage > 0f) return true;
        }
        return false;
    }
    @Override
    public boolean dead(){
        if(multiMod){
            if(health <= 0){
                healthSet(maxHealth);
            }
            return false;
        }
        return super.dead();
    }
    @Override
    public void dead(boolean d){
        if(multiMod){
            dead = false;
            healthSet(maxHealth);
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                }
            });
            return;
        }
        super.dead(d);
    }
    private void healthSet(float h){
        health = h;
    }
    @Override
    public void kill(){
        Log.info("Method: kill() with multiMod: "+multiMod);
        if(multiMod) {
            if(x < 0) x = 0;
            if(x > state.map.width * tilesize) x = state.map.width * tilesize;
            if(y < 0) y = 0;
            if(y > state.map.height * tilesize) y = state.map.height * tilesize;
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.kill();
    }
    @Override
    public void killed(){
        Log.info("Method: killed() with multiMod: "+multiMod);
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.killed();
    }
    @Override
    public void destroy(){
        Log.info("Method: destroy() with multiMod: "+multiMod);
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.destroy();
    }
    @Override
    public void remove(){
        Log.info("Method: remove() with multiMod: "+multiMod);
        if(multiMod) {
            clearEnemy();
            dead = false;
            healthSet(maxHealth);
            Log.info("Tried stopped");
            Log.info("Contains: " + Groups.unit.contains(u -> u == this));
            if(!Groups.unit.contains(u -> u == this)){
                Groups.unit.add(this);
                Log.info("Re-added into Groups.unit");
            }
            Time.run(1f, ()->{
                if(!Groups.unit.contains(u -> u == this)){
                    Groups.unit.add(this);
                    Log.info("Re-added into Groups.unit");
                }
            });
            return;
        }
        super.remove();
    }
    @Override
    public boolean isAdded(){
        if(multiMod) return true;
        return super.isAdded();
    }
    @Override
    public void team(Team t){
        if(t != this.team && multiMod && !isPlayer()){
            clearEnemy();
            return;
        }
        super.team(t);
    }
    @Override
    public void health(float h){
        if(multiMod){
            healthSet(maxHealth);
            return;
        }
        super.health(h);
    }
    @Override
    public void rawDamage(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.rawDamage(d);
    }
    @Override
    public void damage(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damage(d);
    }
    @Override
    public void damage(float d, boolean withEffect){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damage(d, withEffect);
    }
    @Override
    public void damagePierce(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damagePierce(d);
    }
    @Override
    public void damagePierce(float d, boolean withEffect){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damagePierce(d, withEffect);
    }
    @Override
    public void damageArmorMult(float d, float m){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageArmorMult(d, m);
    }
    @Override
    public void damageArmorMult(float d, float m, boolean withEffect){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageArmorMult(d, m, withEffect);
    }
    @Override
    public void damageContinuous(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageContinuous(d);
    }
    @Override
    public void damageContinuousPierce(float d){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageContinuousPierce(d);
    }
    @Override
    public void damageContinuousArmorMult(float d, float m){
        if(multiMod){
            if(health < 0.25f * maxHealth){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
            if(d >= 5000f || d >= health){
                healthSet(maxHealth);
                apply(StatusEffects.invincible, 60f);
                return;
            }
        }
        super.damageContinuousArmorMult(d, m);
    }
    @Override
    public void apply(StatusEffect s, float dur){
        if(multiMod && !s.isVanilla() && !s.name.startsWith("ve-") && s != StatusEffects.invincible && s != StatusEffects.boss){
            apply(StatusEffects.invincible, dur);
            return;
        }
        super.apply(s, dur);
    }
    @Override
    public void apply(StatusEffect s){
        if(multiMod && !s.isVanilla() && !s.name.startsWith("ve-") && s != StatusEffects.invincible && s != StatusEffects.boss){
            apply(StatusEffects.invincible, 60f);
            return;
        }
        super.apply(s);
    }
}
