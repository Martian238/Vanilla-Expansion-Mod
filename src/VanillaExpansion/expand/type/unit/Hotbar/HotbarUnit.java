package VanillaExpansion.expand.type.unit.Hotbar;
import VanillaExpansion.expand.abilities.HotbarAbility;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;
public class HotbarUnit extends UnitType {
    public HotbarUnit(String name) {
        super(name);
        health = 500f;
        speed = 1.2f;
        hitSize = 10f;
        armor = 2f;
        flying = true;
        constructor = UnitEntity::create;
        mineSpeed = 6.5f;
        mineTier = 1;
        buildSpeed = 1.0f;
        abilities.add(new HotbarAbility());
    }
}