package VanillaExpansion.expand.world.block.defense;

import arc.audio.Sound;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.ctype.ContentType;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.meta.BlockStatus;

import static mindustry.Vars.*;

public class ImmuneFieldProjector extends PayloadBlock {
    public ImmuneFieldProjector(String name) {
        super(name);

        outputsPayload = false;
        acceptsPayload = true;
        update = true;
        rotate = false;
        solid = true;
        size = 2;
        payloadSpeed = 1f;
        hasPower = true;
        lightColor = Pal.powerLight;
        canOverdrive = true;
    }

    public float radius = 10 * tilesize;
    public float baseCheckInterval = 20f;
    public boolean lighting = true;
    public boolean topSprite = false;
    public float maxRadiusMultiplier = 3;

    public Sound activateSound = Sounds.none;
    public Sound deactivateSound = Sounds.none;
    public Sound inputSound = Sounds.none;
    public Sound outputSound = Sounds.none;
    public Sound activeSound = Sounds.none;
    public float activeSoundVolume = 0.1f;
    public Sound nonactiveSound = Sounds.none;
    public float nonactiveSoundVolume = 0.1f;

    public ObjectMap<Block, Seq<StatusEffect>> plans = new ObjectMap<>();

    public class ImmuneFieldProjectorBuild extends PayloadBlockBuild<Payload> {
        public float actualRad = 0;
        public boolean wasActive = false;
        public float timer;
        public boolean foundUnits = false;
        public Seq<StatusEffect> currentImmunities = new Seq<>();
        public boolean hadPayload = false;
        public Seq<Unit> affectUnits = new Seq<>();

        public boolean fieldActive(){
            if(efficiency <= 0.1){
                return false;
            }
            if(!(payload != null && payload.content() instanceof Block b && plans.containsKey(b))){
                return false;
            }
            return enabled;
        }

        public Seq<StatusEffect> getImmunities(){
            for(ObjectMap.Entry<Block, Seq<StatusEffect>> entry : plans.entries()){
                if(payload != null && payload.content() instanceof Block b && b == entry.key){
                    return entry.value;
                }
            }
            return new Seq<>();
        }

        @Override
        public void updateTile() {
            super.updateTile();

            if (fieldActive()) {
                if (!wasActive && !headless) {
                    activateSound.at(x, y);
                }
                wasActive = true;

                timer += Time.delta;
                if (timer >= baseCheckInterval) {
                    foundUnits = false;
                    timer = 0f;
                    if (Units.any(x - actualRad, y - actualRad, actualRad * 2, actualRad * 2, unit -> unit.team == team)) {
                        foundUnits = true;
                    }
                }
                if(foundUnits){
                    currentImmunities = getImmunities();
                    affectUnits.clear();
                    Units.nearby(team, x - actualRad, y - actualRad, actualRad * 2, actualRad * 2, unit -> {
                        if(Mathf.dst(unit.x, unit.y, x, y) <= actualRad - unit.hitSize / 2){
                            for(Object o : currentImmunities){
                                if(o instanceof StatusEffect e){
                                    if(unit.hasEffect(e)) unit.unapply(e);
                                    affectUnits.add(unit);
                                }else if(o instanceof String s){
                                    StatusEffect e = content.getByName(ContentType.status, s);
                                    if(unit.hasEffect(e)) unit.unapply(e);
                                    affectUnits.add(unit);
                                }
                            }
                        }
                    });
                }
                float r = Math.min(radius * efficiency * timeScale(), radius * maxRadiusMultiplier);
                actualRad = Mathf.approachDelta(actualRad, r, 0.1f + 0.1f * Math.abs(r - actualRad));
                if(!headless){
                    control.sound.loop(activeSound, new Vec2(x, y), activeSoundVolume);
                }
            } else {
                if (wasActive && !headless) {
                    deactivateSound.at(x, y);
                }
                wasActive = false;

                actualRad = Mathf.approachDelta(actualRad, 0, 0.1f + 0.1f * Math.abs(actualRad));
                if(Float.isNaN(actualRad) || actualRad < 0){
                    actualRad = 0;
                }
                if(hadPayload && !headless){
                    control.sound.loop(nonactiveSound, new Vec2(x, y), nonactiveSoundVolume);
                }
            }

            if(payload != null && !hadPayload){
                if(!headless){
                    inputSound.at(x, y);
                }
                hadPayload = true;
            }
            if(payload == null && hadPayload){
                if(!headless) {
                    outputSound.at(x, y);
                }
                hadPayload = false;
            }
        }

        @Override
        public BlockStatus status(){
            if(!enabled) return BlockStatus.logicDisable;
            if(payload == null) return BlockStatus.noOutput;
            return wasActive ? BlockStatus.active : BlockStatus.noInput;
        }

        @Override
        public void draw() {
            if(headless) return;
            Draw.rect(region, x, y);
            Draw.z(Layer.blockOver);
            drawPayload();
            if(topSprite)Draw.rect(topRegion, x, y);
            if(wasActive){
                if(lighting) {
                    Drawf.light(x, y, actualRad * 2f, lightColor, 1f);
                }
            }
            Lines.stroke(1.5f);
            Draw.color(team.color);
            Draw.z(Layer.effect);
            float fra = 0.25f * Mathf.clamp((actualRad - size * tilesize / 2f) / (radius - size * tilesize / 2f));
            Lines.arc(x, y, actualRad, fra, Time.time);
            Lines.arc(x, y, actualRad, fra, Time.time + 120f);
            Lines.arc(x, y, actualRad, fra, Time.time - 120f);
            if(wasActive){
                for(Unit u : affectUnits){
                    Lines.arc(u.x, u.y, u.hitSize * 1.25f, 0.33f, Time.time * 2f);
                    Lines.arc(u.x, u.y, u.hitSize * 1.25f, 0.33f, Time.time * 2f + 180f);
                }
            }
            Draw.color();
        }

        @Override
        public boolean acceptUnitPayload(Unit unit){
            return false;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return this.payload == null && super.acceptPayload(source, payload) && payload.fits(size);
        }
    }
}
