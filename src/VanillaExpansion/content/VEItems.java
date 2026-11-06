package VanillaExpansion.content;
import arc.graphics.*;
import arc.struct.*;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.type.*;
public class VEItems {
    public static Item iron, uranium, manganese, gold;
    public static final Seq<Item> proximaOreItems = new Seq<>();
    public static RBMKRodItem plutonium238BerylliumSource; // 钚238-铍中子源
    public static RBMKRodItem heu235UraniumFuel; // HEU-235铀燃料棒
    public static RBMKRodItem dgammaFuel;
    //物品栏物品定义部分
    public static CustomItem test1,test2,test3,test4,test5,test6,test7;
    //
    public static void load(){
        //物品栏物品定义部分
        // 短程测试
        test1 = new CustomItem("test1", Color.valueOf("676767"));
        test1.weaponBullet = new BasicBulletType(8f, 35f) {{
            width = 6f;
            height = 12f;
            lifetime = 12f;
            hitEffect = Fx.hitBulletColor;
            despawnEffect = Fx.none;
            statusDuration = 30f;
        }};
        test1.weaponReload = 18f;
        test1.weaponShootCone = 25f;
        test1.weaponShootX = 6f;
        test1.weaponRecoil = 1.2f;
        test1.weaponShake = 1f;
        // 远程测试
        test2 = new CustomItem("test2", Color.valueOf("676767"));
        test2.weaponBullet = new BasicBulletType(10f, 25f) {{
            width = 4f;
            height = 14f;
            lifetime = 70f;
            pierce = true;
            pierceCap = 2;
            hitEffect = Fx.hitBulletColor;
            despawnEffect = Fx.none;
        }};
        test2.weaponReload = 45f;
        test2.weaponShootCone = 6f;
        test2.weaponShootX = 8f;
        // 血量测试
        test3 = new CustomItem("test3", Color.valueOf("676767"));
        test3.bonusHealth = 300f;
        // 速度测试
        test4 = new CustomItem("test4", Color.valueOf("676767"));
        test4.bonusSpeedMul = 1.5f;
        // 燃烧测试
        test5 = new CustomItem("test5", Color.valueOf("676767"));
        test5.weaponBullet = new BasicBulletType(5f, 12f) {{
            width = 8f;
            height = 8f;
            lifetime = 40f;
            hitEffect = Fx.hitBulletColor;
            despawnEffect = Fx.none;
            status = StatusEffects.burning;
            statusDuration = 60f;
        }};
        test5.weaponReload = 60f;
        test5.weaponShootCone = 15f;
        // 潮湿效果测试
        test6 = new CustomItem("test6", Color.valueOf("676767"));
        test6.bonusStatus = StatusEffects.wet;
        test6.bonusStatusDuration = 600f;
        // 纯装饰
        test7 = new CustomItem("test7", Color.valueOf("676767"));
        //结束
        iron = new Item("iron", Color.valueOf("a8a8a8")){{
            hardness = 2;
            cost = 0.8f;
            databaseTag = "basic-item";
        }};
        uranium = new Item("uranium", Color.valueOf("7fff00")){{
            hardness = 5;
            cost = 1.5f;
            radioactivity = 1.2f;
            explosiveness = 0.3f;
            healthScaling = 0.15f;
            databaseTag = "basic-item";
        }};
        manganese = new Item("manganese", Color.valueOf("E35745FF")){{
            hardness = 4;
            cost = 1.3f;
            healthScaling = 0.7f;
            databaseTag = "basic-item";
        }};
        gold = new Item("gold", Color.valueOf("ffd37f")){{
            hardness = 2;
            cost = 1.0f;
            databaseTag = "basic-item";
        }};
        proximaOreItems.addAll(iron, uranium, manganese, gold);

        plutonium238BerylliumSource = new RBMKRodItem("plutonium238-beryllium-source", new Color(0.2f, 0.3f, 0.8f)){{
            isNeutronSource = true;
            enrichment = 1f;
            cost = 5000;
            radioactivity = 5f;
            databaseTag = "processed-item";
        }}
            .setYield(50_000_000D)
            .setStats(40, 40)
            .setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
            .setHeat(0.1D)
            .setDiffusion(0.05D)
            .setMeltingPoint(1287D)
            .setNeutronTypes(RBMKRodItem.NType.SLOW, RBMKRodItem.NType.SLOW)
            .setTint(0x656E6B);
        heu235UraniumFuel = new RBMKRodItem("heu235-uranium-fuel", new Color(0.3f, 0.8f, 0.2f)){{
            enrichment = 1f;
            cost = 3000;
            radioactivity = 3f;
            databaseTag = "processed-item";
        }}
            .setYield(100_000_000D)
            .setStats(50, 0)
            .setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
            .setMeltingPoint(2865D)
            .setTint(0x868D82);
        dgammaFuel = new RBMKRodItem("dgamma-source", Color.valueOf("C70000FF")){{
            enrichment = 1f;
            cost = 5000;
            radioactivity = 5f;
            databaseTag = "processed-item";
        }}
            .setYield(10_000_000D)
            .setStats(1000, 10)
            .setFunction(RBMKRodItem.EnumBurnFunc.QUADRATIC)
            .setHeat(0.1D)
            .setMeltingPoint(100_000D)
            .setDangerous(true)
            .setTint(0xD77276);
    }
}
