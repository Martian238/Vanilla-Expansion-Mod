package VanillaExpansion.expand.bullets;

import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.PointBulletType;
import VanillaExpansion.expand.world.block.power.RBMKRod;
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import java.util.Objects;

public class HyperKillBulletType extends PointBulletType {

    public String exceptName1 = "new-horizon-nucleoid";
    public String exceptName2 = "new-horizon-pester";

    public HyperKillBulletType(){}

    @Override
    public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        build.changeTeam(Team.derelict);
        for(Building building : Groups.build) {
            if(building != null) {
                try { building.kill(); building.remove(); } catch(Exception ignored) {}
            }
        }
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc entity, float health){
            if(entity instanceof Unit unit){
                if(Objects.equals(unit.type.name, exceptName1)) {
                    try {
                        unit.team(b.team);
                    } catch (Exception ignored) {
                    }
                }else if(Objects.equals(unit.type.name, exceptName2)) {
                    try {
                        unit.team(b.team);
                    } catch (Exception ignored) {
                    }
                }else{
                    try {
                        unit.kill();
                        unit.remove();
                    } catch (Exception ignored) {
                    }
                }
            }else if(entity instanceof Healthc h) {
                try {
                    h.kill();
                    h.remove();
                } catch (Exception ignored) {
                }
            }



    }

}
