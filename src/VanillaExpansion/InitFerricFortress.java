package VanillaExpansion;

import VanillaExpansion.content.VEJSUnitTypes;
import arc.Events;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import java.util.Objects;

import static mindustry.Vars.tilesize;

public class InitFerricFortress {

    public static void init(){
        //铁堡垒生成
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            //Log.info("build end");
            if(Objects.equals(e.tile.block().name, "ve-watermelon")){
                //Log.info("watermelon");
                int x = e.tile.x;
                int y = e.tile.y;
                if(checkFerrumWalls(x, y, -90, e.unit.team)){
                    createFerricFortress(x, y, -90, e.unit, e.unit.team);
                    return;
                }
                if(checkFerrumWalls(x, y, 0, e.unit.team)){
                    createFerricFortress(x, y, 0, e.unit, e.unit.team);
                    return;
                }
                if(checkFerrumWalls(x, y, 90, e.unit.team)){
                    createFerricFortress(x, y, 90, e.unit, e.unit.team);
                    return;
                }
                if(checkFerrumWalls(x, y, 180, e.unit.team)){
                    createFerricFortress(x, y, 180, e.unit, e.unit.team);
                }
            }
        });
    }

    public static boolean checkFerrumWall(int x, int y, boolean mustNull, Team team){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null || tile.block() instanceof Floor){
            return mustNull;
        }else{
            return (Objects.equals(tile.block().name, "ve-ferrum-wall")) && !mustNull && tile.team() == team;
        }
    }

    public static boolean checkFerrumWalls(int x, int y, float rot, Team team){
        return checkFerrumWall(x + (int) Mathf.cosDeg(rot), y + (int) Mathf.sinDeg(rot), false, team) &&
                checkFerrumWall(x + 2 * (int) Mathf.cosDeg(rot), y + 2 * (int) Mathf.sinDeg(rot), false, team) &&
                checkFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot + 90f),
                        y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot + 90f), false, team) &&
                checkFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot - 90f),
                        y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot - 90f), false, team) &&
                checkFerrumWall(x + 2 * (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot + 90f),
                        y + 2 * (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot + 90f), true, team) &&
                checkFerrumWall(x + 2 * (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot - 90f),
                        y + 2 * (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot - 90f), true, team);
    }

    public static void createFerricFortress(int x, int y, float rot, Unit owner, Team team){
        removeFerrumWall(x, y);
        removeFerrumWall(x + (int) Mathf.cosDeg(rot), y + (int) Mathf.sinDeg(rot));
        removeFerrumWall(x + 2 * (int) Mathf.cosDeg(rot), y + 2 * (int) Mathf.sinDeg(rot));
        removeFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot + 90f),
                y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot + 90f));
        removeFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot - 90f),
                y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot - 90f));
        Tile tile = Vars.world.tile(x, y);
        if(tile != null) {
            Unit fu = VEJSUnitTypes.ferricFortress.create(team);
            fu.set(tile.worldx() + 2 * tilesize * Mathf.cosDeg(rot), tile.worldy() + 2 * tilesize * Mathf.sinDeg(rot));
            Events.fire(new EventType.UnitCreateEvent(fu, null, owner));
            if (!Vars.net.client()) {
                fu.add();
                Units.notifyUnitSpawn(fu);
            }
            fu.rotation = rot;
            Sounds.unitCreateBig.at(tile.worldx(), tile.worldy(), 1f, 0.7f);
            Effect.shake(4, 12, tile.worldx(), tile.worldy());
            //Log.info("golem");
        }
    }

    public static void removeFerrumWall(int x, int y){
        Tile tile = Vars.world.tile(x, y);
        if(tile != null && !(tile.block() instanceof Floor)){
            tile.setAir();
            Fx.dynamicExplosion.at(tile.worldx(), tile.worldy(), 1f);
        }
    }
}
