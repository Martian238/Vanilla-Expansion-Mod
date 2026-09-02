package VanillaExpansion.expand.type.unit;

import arc.audio.Sound;
import arc.graphics.Color;
import mindustry.game.Team;
import mindustry.gen.Sounds;
import mindustry.type.UnitType;

import static mindustry.Vars.tilesize;

public class SentryUnitType extends UnitType {
    public SentryUnitType(String name){
        super(name);
        constructor = SentryUnit::create;
    }

    public float visionRadius = 80f;
    public float suspectRadius = 60f;
    public float visionAngle = 80f;
    public float suspectDuration = 120f;
    public float movingWarmupTime = 60f;
    public float suspectTime = 120f;

    public float areaWaveTime = 20f;
    public float areaWaveInterval = 40f;
    public float areaWaveMultiplierSuspect = 2f;
    public float areaWaveMultiplierAlert = 4f;
    public Color areaColorDefault = Color.valueOf("98ffa9");
    public Color areaColorSuspect = Color.valueOf("ffd37f");
    public Color areaColorAlert = Color.valueOf("f25555");

    public boolean continuousAttack;

    public Team playerTeam = Team.sharded;

    public Sound suspectSound = Sounds.none;
    public Sound alertSound = Sounds.none;
    public UnitType exclamationMarkUnit;
    public float exclamationMarkOffset = 3.75f * tilesize;
    public float confirmTime = 300f;

}
