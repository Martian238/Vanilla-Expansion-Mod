package VanillaExpansion.expand.type.unit.internal;

import mindustry.gen.TimedKillUnit;

public class ChiniunChargeUnit extends TimedKillUnit {

    private boolean soundPlayed = false;

    @Override
    public void update(){
        super.update();

        if(!soundPlayed){
                if(type instanceof ChiniunChargeUnitType c){
                    c.startChargeSound.at(x, y, 1f, 0.5f);
                }
                soundPlayed = true;
        }
    }

    @Override
    public void draw(){
        if(type instanceof ChiniunChargeUnitType c){
            c.draw(this);
            return;
        }
        super.draw();
    }
}
