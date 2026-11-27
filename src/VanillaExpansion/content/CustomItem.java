package VanillaExpansion.content;
import arc.graphics.Color;
import mindustry.entities.bullet.BulletType;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
public class CustomItem extends Item {
    public int weaponShootMode = 0;
    //物品作为武器时发射的弹,null时表示这不是武器物品
    public BulletType weaponBullet = null;
    // 发射间隔(30)
    public float weaponReload = 30f;
    //发射角度容差
    public float weaponShootCone = 10f;
    //发射点本地偏移
    public float weaponShootX = 0f, weaponShootY = 0f;
    //后坐力
    public float weaponRecoil = 0f;
    //震动
    public float weaponShake = 0f;
    //选中时增加的最大生命值（0 = 无）
    public float bonusHealth = 0f;
    //选中时的速度乘数（1 = 无）
    public float bonusSpeedMul = 1f;
    //选中时施加的状态效果（null = 无）
    public StatusEffect bonusStatus = null;
    //状态持续时间
    public float bonusStatusDuration = 600f;
    public CustomItem(String name, Color color) {super(name, color);}
}