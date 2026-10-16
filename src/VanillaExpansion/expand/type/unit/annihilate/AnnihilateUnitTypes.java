package VanillaExpansion.expand.type.unit.annihilate;

public class AnnihilateUnitTypes {

    public static AnnihilatePartUnitType
            anniCap, anniSideR, anniSideL, anniShieldR, anniShieldL, anniFrontR, anniFrontL,
            anniArmFR, anniArmFL, anniArmBR, anniArmBL;
    public static AnnihilateMainUnitType anni;

    public static float partHealth = 500000f, partArmor = 32f;

    public static void load(){

        anniCap = new AnnihilatePartUnitType("annihilate-part-cap"){{
            health = partHealth;
            armor = partArmor;
            spriteY = -4f;
            hitSize = 60f;
            sprite = "ve-annihilate-cap-region";
        }};
        anniSideR = new AnnihilatePartUnitType("annihilate-part-side-r"){{
            health = partHealth;
            armor = partArmor;
            spriteX = 32f;
            hitSize = 50f;
            sprite = "ve-annihilate-side-region";
        }};
        anniSideL = new AnnihilatePartUnitType("annihilate-part-side-l"){{
            health = partHealth;
            armor = partArmor;
            spriteX = -32f;
            hitSize = 50f;
            sprite = "ve-annihilate-side-region";
        }};
        anniFrontR = new AnnihilatePartUnitType("annihilate-part-front-r"){{
            health = partHealth;
            armor = partArmor;
            spriteX = 28f;
            hitSize = 32f;
            sprite = "ve-annihilate-front-region";
        }};
        anniFrontL = new AnnihilatePartUnitType("annihilate-part-front-l"){{
            health = partHealth;
            armor = partArmor;
            spriteX = -28f;
            hitSize = 32f;
            sprite = "ve-annihilate-front-region";
        }};
        anniArmFR = new AnnihilatePartUnitType("annihilate-part-arm-fr"){{
            health = partHealth;
            armor = partArmor;
            hitSize = 32f;
            sprite = "ve-annihilate-arm-region";
            armSprite1 = "ve-annihilate-arm-spike1";
            armSprite2 = "ve-annihilate-arm-spike2";
        }};
        anniArmFL = new AnnihilatePartUnitType("annihilate-part-arm-fl"){{
            health = partHealth;
            armor = partArmor;
            hitSize = 32f;
            sprite = "ve-annihilate-arm-region";
            armSprite1 = "ve-annihilate-arm-spike1";
            armSprite2 = "ve-annihilate-arm-spike2";
        }};
        anniArmBR = new AnnihilatePartUnitType("annihilate-part-arm-br"){{
            health = partHealth;
            armor = partArmor;
            hitSize = 32f;
            sprite = "ve-annihilate-arm-region";
            armSprite1 = "ve-annihilate-arm-spike1";
            armSprite2 = "ve-annihilate-arm-spike2";
        }};
        anniArmBL = new AnnihilatePartUnitType("annihilate-part-arm-bl"){{
            health = partHealth;
            armor = partArmor;
            hitSize = 32f;
            sprite = "ve-annihilate-arm-region";
            armSprite1 = "ve-annihilate-arm-spike1";
            armSprite2 = "ve-annihilate-arm-spike2";
        }};
        anniShieldR = new AnnihilatePartUnitType("annihilate-part-shield-r"){{
            health = partHealth;
            armor = partArmor;
            hitSize = 48f;
            sprite = "ve-annihilate-shield-region";
            physics = false;
        }};
        anniShieldL = new AnnihilatePartUnitType("annihilate-part-shield-l"){{
            health = partHealth;
            armor = partArmor;
            hitSize = 48f;
            sprite = "ve-annihilate-shield-region";
            physics = false;
        }};

        anni = new AnnihilateMainUnitType("annihilate"){{
            health = 2000000f;
            armor = partArmor;
            hitSize = 100f;
            databaseTag = "10special-unit";
            rotateSpeed = 4f;
            buildSpeed = 1f;
            buildRange = 400f;
            buildBeamOffset = 24f;
        }};
    }
}
