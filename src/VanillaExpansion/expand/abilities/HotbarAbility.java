package VanillaExpansion.expand.abilities;
import VanillaExpansion.content.CustomItem;
import VanillaExpansion.expand.ui.HotbarUI;
import arc.util.Log;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
public class HotbarAbility extends Ability {
    private HotbarWeaponSlot pooledSlot = new HotbarWeaponSlot();
    public HotbarWeaponSlot getActiveSlot() { return itemSlot; }
    public float getSpeedMul() { return speedMul; }
    public float getBonusHealth() { return bonusHealth; }
    public mindustry.type.StatusEffect getBonusStatus() { return bonusStatus; }
    private HotbarWeaponSlot itemSlot;
    private Item lastItem;
    private float speedMul = 1f;
    private float bonusHealth = 0f;
    private StatusEffect bonusStatus = null;

    @Override
    public HotbarAbility copy() {
        HotbarAbility out = (HotbarAbility) super.copy();
        out.lastItem = null;
        out.speedMul = 1f;
        out.bonusHealth = 0f;
        out.bonusStatus = null;
        out.itemSlot = null;
        out.pooledSlot = new HotbarWeaponSlot();
        return out;
    }
    @Override
    public void update(Unit unit) {
        HotbarUI ui = HotbarUI.instance;
        if (ui == null) return;

        Item current = ui.getItem(ui.getSelected());
        if (current != lastItem) {
            revoke(unit);
            lastItem = current;
            if (current != null) apply(unit, current);

            // 子弹调试
//            arc.util.Log.info("[HotbarAbility@" + System.identityHashCode(this) + "] unit="
//                    + System.identityHashCode(unit) + " itemSlot=" + itemSlot
//                    + " bullet=" + (itemSlot != null ? itemSlot.bullet : "null"));
        }

        if (itemSlot != null && itemSlot.bullet != null) {
            itemSlot.updateTimers();
            if (unit.isShooting() && itemSlot.readyToShoot(unit)) {
                itemSlot.tryShoot(unit, unit.aimX, unit.aimY);
            }
        }
    }
    private void revoke(Unit unit) {
        if (speedMul != 1f) {
            unit.speedMultiplier(unit.speedMultiplier() / speedMul);
            speedMul = 1f;
        }
        if (bonusHealth > 0f) {
            unit.maxHealth = Math.max(1f, unit.maxHealth - bonusHealth);
            unit.health = Math.min(unit.health, unit.maxHealth);
            bonusHealth = 0f;
        }
        if (bonusStatus != null) {
            unit.unapply(bonusStatus);
            bonusStatus = null;
        }
        itemSlot = null;
    }

    // 应用数据驱动
    private void apply(Unit unit, Item item) {
        if (!(item instanceof CustomItem ci)) {
            //Log.info("[HotbarAbility] 物品 " + item.name + " 不是 CustomItem，忽略");
            return;
        }
        if (ci.weaponBullet != null) {
            pooledSlot.bullet = ci.weaponBullet;
            pooledSlot.reload = ci.weaponReload;
            pooledSlot.fromCenter = ci.weaponShootMode == 0;
            pooledSlot.alongAim   = ci.weaponShootMode == 1;
            pooledSlot.shootX = ci.weaponShootX;
            pooledSlot.shootY = ci.weaponShootY;
            pooledSlot.recoil = ci.weaponRecoil;
            pooledSlot.shake = ci.weaponShake;
            pooledSlot.reloadTimer = 0f;
            pooledSlot.heatTimer = 0f;
            pooledSlot.warmupTimer = 0f;
            itemSlot = pooledSlot;
            //Log.info("[HotbarAbility] 物品 " + ci.name + " -> 武器");
        }
        if (ci.bonusHealth > 0f) {
            bonusHealth = ci.bonusHealth;
            unit.maxHealth += bonusHealth;
            unit.health += bonusHealth;
        }
        if (ci.bonusSpeedMul != 1f) {
            speedMul = ci.bonusSpeedMul;
            unit.speedMultiplier(unit.speedMultiplier() * speedMul);
        }
        if (ci.bonusStatus != null) {
            bonusStatus = ci.bonusStatus;
            unit.apply(bonusStatus, ci.bonusStatusDuration);
        }
    }
}