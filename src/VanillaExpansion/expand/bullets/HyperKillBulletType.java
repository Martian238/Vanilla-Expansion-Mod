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

public class HyperKillBulletType extends PointBulletType {
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
        if(health == 1000000f) {
            if(entity instanceof Unit unit){
                try {
                    unit.team(b.team);
                } catch(Exception ignored) {}
            }
        }else {
            if (entity instanceof Healthc h) {
                try {
                    h.kill();
                    h.remove();
                } catch (Exception ignored) {
                }
            }

            if (entity instanceof Unit unit) {
                try {
                    unit.kill();
                    unit.remove();
                } catch (Exception ignored) {
                }
            }
            try {
                for (Teams.TeamData teamData : Vars.state.teams.present) {
                    if (teamData.team != b.team) {
                        if (teamData.units != null) {
                            teamData.units.clear();
                        }
                        if (teamData.buildings != null) {
                            teamData.buildings.clear();
                        }
                        if (teamData.unitTree != null) {
                            teamData.unitTree.clear();
                        }
                        if (teamData.buildingTree != null) {
                            teamData.buildingTree.clear();
                        }
                        // unitCount 是 int 类型（基本类型），无法清空，跳过
                        // 可以通过反射设置内部数组
                        try {
                            java.lang.reflect.Field unitCountField = teamData.getClass().getDeclaredField("unitCount");
                            unitCountField.setAccessible(true);
                            Object unitCountObj = unitCountField.get(teamData);
                            if (unitCountObj instanceof IntIntMap) {
                                ((IntIntMap) unitCountObj).clear();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }


    }

}
