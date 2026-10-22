package VanillaExpansion.expand.type.unit;

import arc.Core;
import arc.input.KeyCode;
import arc.util.Log;
import arc.util.Time;
import mindustry.game.Team;
import mindustry.gen.PayloadUnit;

public class OmicronUnit extends PayloadUnit {

    private float vulnerableTimer = 0;

    public static PayloadUnit create(){
        return new OmicronUnit();
    }

    @Override
    public void update(){
        super.update();
        if(type instanceof OmicronUnitType o && (isShooting() || (o.buildVulnerable && isBuilding()))) {
            vulnerableTimer = o.shootVulnerableTime;
        }else{
            vulnerableTimer = Math.max(vulnerableTimer - Time.delta, 0f);
        }
    }

    public boolean vulnerable(){
        return vulnerableTimer > 0;
    }

    @Override
    public boolean targetable(Team targeter){
        return type.targetable(this, targeter) || vulnerable();
    }

    @Override
    public boolean hittable() {
        return type.hittable(this) || vulnerable();
    }
}
