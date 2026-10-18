package VanillaExpansion.expand.abilities;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

import static mindustry.Vars.tilesize;

public class PaintedOctRingAbility extends Ability {
    public PaintedOctRingAbility(){
        display = false;
    }
    private boolean draw = false;
    private float detectTimer = 0f;

    @Override
    public void update(Unit unit){
        if(Vars.state.enemies <= 0){
            draw = false;
            return;
        }
        detectTimer += Time.delta;
        if(detectTimer >= 480f){
            draw = Groups.unit.contains(u -> u.type.name.startsWith("ve-zentack"));
            detectTimer -= 6f;
        }
    }

    @Override
    public void draw(Unit unit){
        if(draw){
            Draw.color(Pal.heal);
            Draw.z(Layer.bullet - 0.01f);
            Lines.stroke(2f);
            Lines.arc(unit.x - tilesize * Mathf.cosDeg(unit.rotation),
                    unit.y - tilesize * Mathf.sinDeg(unit.rotation), unit.type.hitSize,
                    unit.health / unit.maxHealth, 90f);
            Draw.color();
        }
    }
}
