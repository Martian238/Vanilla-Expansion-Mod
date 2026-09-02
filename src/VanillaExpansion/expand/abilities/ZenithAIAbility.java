package VanillaExpansion.expand.abilities;

import arc.audio.Sound;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;

import static mindustry.Vars.tilesize;

public class ZenithAIAbility extends Ability {
    public ZenithAIAbility(){}

    public Seq<String> zenithUnits = Seq.with(

    );
    public Seq<String> miniZenithUnits = Seq.with(

    );
    public Sound talkSoundNormal = Sounds.none;
    public Sound talkSoundQuestion = Sounds.none;
    public Sound talkSoundUrgent = Sounds.none;

    public boolean isMini = false;
    public float chatDistance = 6f * tilesize;

    //TODO

    public boolean inBase = false;

    private Seq<Unit> scannedZeniths = new Seq<>();
    private Seq<Unit> scannedMiniZeniths = new Seq<>();
    private Seq<Unit> chattedZeniths = new Seq<>();
    private Seq<Unit> deadZeniths = new Seq<>();
    private Seq<Unit> deadChattedZeniths = new Seq<>();

    private Unit selectedZenith;
    private int zenithCount = 0;
    private int miniZenithCount = 0;

    public void refreshZeniths(Unit unit){
        for(Unit u : Groups.unit){
            if(u.team == unit.team && zenithUnits.contains(u.type.name) && !scannedZeniths.contains(u)){
                scannedZeniths.add(u);
            }
            if(u.team == unit.team && miniZenithUnits.contains(u.type.name) && !scannedMiniZeniths.contains(u)){
                scannedMiniZeniths.add(u);
            }
        }
        zenithCount = miniZenithCount = 0;
        for(Unit u : scannedZeniths){
            if(u == null || u.dead() || u.team != unit.team) {
                scannedZeniths.remove(u);
                deadZeniths.add(u);
                if(chattedZeniths.contains(u)){
                    deadChattedZeniths.add(u);
                }
            }
            zenithCount = scannedZeniths.size;
        }
        for(Unit u : scannedMiniZeniths){
            if(u == null || u.dead() || u.team != unit.team) {
                scannedMiniZeniths.remove(u);
                deadZeniths.add(u);
                if(chattedZeniths.contains(u)){
                    deadChattedZeniths.add(u);
                }
            }
            miniZenithCount = scannedMiniZeniths.size;
        }
    }

    public void selectZenith(Unit unit){
        if(isMini){
            int p = (int) Math.floor(Math.random() * (miniZenithCount));
            selectedZenith = scannedMiniZeniths.get(p);
        }
    }

    public void findSelected(Unit unit){
        if(isMini){
            if(selectedZenith != null){
                float d = getDistance(selectedZenith.x, selectedZenith.y, unit.x, unit.y);
                float td = d - chatDistance;
                float tx = unit.x + td / d * (selectedZenith.x - unit.x);
                float ty = unit.y + td / d * (selectedZenith.y - unit.y);
                Vec2 tv = new Vec2(tx, ty);
                unit.command().commandPosition(tv, true);
            }
        }
    }

    public void faceTo(Unit unit, Unit other){
        if(unit.flag == other.flag && getDistance(unit.x, unit.y, other.x, other.y) <= 2f * chatDistance){
            rotateTo(unit, other);
        }
    }

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }

    public void rotateTo(Unit unit, Unit other){
        if(other != null){
            float ta = Mathf.radiansToDegrees * Mathf.atan2(other.x - unit.x, other.y - unit.y);
            float sin = Mathf.sinDeg(ta - unit.rotation);
            if(Angles.angleDist(ta, unit.rotation()) <= unit.type.rotateSpeed){
                unit.rotation(ta);
            }else {
                if (sin > 0) {
                    unit.rotation += unit.type.rotateSpeed;
                }else{
                    unit.rotation -= unit.type.rotateSpeed;
                }
            }
        }
    }
}
