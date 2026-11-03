package VanillaExpansion.expand.type.unit.internal;

import VanillaExpansion.EntityRegister;
import VanillaExpansion.expand.world.block.mechanism.OrbitStrikeProcessor;
import arc.Core;
import arc.Events;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.core.UI;
import mindustry.game.EventType;
import mindustry.gen.UnitEntity;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.renderer;
import static mindustry.Vars.tilesize;

public class OrbitStrikeBeaconUnit extends UnitEntity {


    public OrbitStrikeProcessor.OrbitStrikeBuild processor = null;

    public float activateTimer = 0;
    public float activateTime = 0;
    public boolean activated = false;

    public Color redColor = Color.valueOf("f25555");
    public Color orangeColor = Color.valueOf("ffa665");

    @Override
    public void update(){
        super.update();
        if(activateTime <= 0 && type instanceof OrbitStrikeBeaconUnitType ot){
            activateTime = ot.activateTime;
        }
        if(!activated && activateTime > 0 && activateTimer >= activateTime){
            activated = true;
            if(type instanceof OrbitStrikeBeaconUnitType ot){
                ot.activateSound.at(x, y, 1f, 0.5f);
            }
            if(processor != null){
                processor.activatedBeacons.add(this);
            }
        }
        if(drownTime() <= 0 && processor != null) {
            activateTimer += Time.delta * speedMultiplier();
        }
    }

    @Override
    public void draw(){
        super.draw();

        if(!activated && processor != null){
            Draw.z(Layer.blockOver + 0.1f);
            Draw.blend(Blending.additive);
            Draw.color(redColor);
            Draw.alpha(0.5f * (1 - drownTime()));
            Lines.stroke(1f);
            Lines.arc(x, y, 120f, 0.2f, Time.time / 2f);
            Lines.arc(x, y, 120f, 0.2f, Time.time / 2f + 90f);
            Lines.arc(x, y, 120f, 0.2f, Time.time / 2f + 180f);
            Lines.arc(x, y, 120f, 0.2f, Time.time / 2f + 270f);
            Draw.blend();
        }

        if(renderer.pixelate || processor == null) return;

        Draw.alpha(1f);
        Draw.z(Layer.overlayUI);
        Font font = Fonts.outline;
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 4f / Scl.scl(1f));
        font.setUseIntegerPositions(false);

        boolean red = activateTime - activateTimer <= 600f * speedMultiplier() && drownTime <= 0;
        float timer = Math.max(activateTime - activateTimer + 59.99f, 0f);
        int seconds = Mathf.floor(timer % 3600f / 60f);
        String text = Mathf.floor(timer / 3600f) + ":" + (seconds >= 10 ? seconds : ("0" + seconds));
        Color col = drownTime() > 0 || activated ? Color.gray : (red ? redColor.cpy().lerp(orangeColor, 0.5f * Mathf.sin(Time.time / 5f)) : Color.white);

        l.setText(font, text, col, 90f, Align.left, true);
        float offset = 1f;

        Draw.color(0f, 0f, 0f, 0.2f);
        Fill.rect(x, y, l.width + offset*2f, l.height + offset*2f);
        Draw.color();
        font.setColor(col);
        font.draw(text, x - l.width/2f, y + l.height/2f, 90f, Align.left, true);
        font.setUseIntegerPositions(ints);

        font.getData().setScale(1f);

        Pools.free(l);
    }

    @Override
    public void kill(){
        if(type instanceof OrbitStrikeBeaconUnitType ot && !activated){
            ot.activateSound.at(x, y, 1f, 0.5f);
        }
        super.kill();
    }

    @Override
    public void updateDrowning() {
        Floor floor = drownFloor();
        if (floor != null && floor.isLiquid && floor.drownTime > 0 && canDrown()) {
            lastDrownFloor = floor;
            drownTime += Time.delta / (hitSize / 8.0F * type.drownTimeMultiplier * floor.drownTime);
            if (Mathf.chanceDelta(0.05F)) {
                floor.drownUpdateEffect.at(x, y, hitSize, floor.mapColor);
            }
            if (drownTime >= 0.999F/* && !net.client()*/) {
                if(type instanceof OrbitStrikeBeaconUnitType ot){
                    ot.drownSound.at(x, y, 1f, 0.5f);
                }
                Fx.drillSteam.at(x, y);
                Events.fire(new EventType.UnitDrownEvent(this));
                if(processor != null) processor.safeRemovedBeacons.add(this);
                remove();
            }
        } else {
            drownTime -= Time.delta / 50.0F;
        }
        drownTime = Mathf.clamp(drownTime);
    }

    public void beaconWrite(Writes write){
        write.f(activateTimer);
        write.bool(activated);
    }
    public void beaconRead(Reads read){
        activateTimer = read.f();
        activated = read.bool();
    }
    @Override
    public void write(Writes write){
        super.write(write);
        beaconWrite(write);
    }
    @Override
    public void read(Reads read){
        super.read(read);
        beaconRead(read);
    }
    @Override
    public int classId() {
        return EntityRegister.getID(getClass());
    }
}
