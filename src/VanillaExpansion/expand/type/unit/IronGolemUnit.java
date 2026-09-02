package VanillaExpansion.expand.type.unit;

import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.gen.MechUnit;

import java.util.Objects;

public class IronGolemUnit extends MechUnit {

    public static MechUnit create(){
        return new IronGolemUnit();
    }

    private float invincibleTimer = 0f;

    @Override
    public void update(){
        super.update();
        if(invincibleTimer > 0f){
            invincibleTimer -= 1f;
        }
        if(type instanceof IronGolemType i){
            if(hasItem() && Objects.equals(item().name, "ve-ferrum") && invincibleTimer <= 0 && health < maxHealth){
                i.healSound.at(x, y, 1f + Mathf.range(i.healSoundRange), i.healSoundVolume);
                invincibleTimer = i.invincibleTime;
                health(Math.min(maxHealth, health + i.selfHealAmount));
                addItem(item(), -1);
            }
        }
    }

    @Override
    public void impulse(float x, float y){}

    @Override
    public void damage(float damage){
        if(invincibleTimer > 0f) return;
        super.damage(damage);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damage(float damage, boolean e){
        if(invincibleTimer > 0f) return;
        super.damage(damage, e);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damagePierce(float damage){
        if(invincibleTimer > 0f) return;
        super.damagePierce(damage);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damagePierce(float damage, boolean e){
        if(invincibleTimer > 0f) return;
        super.damagePierce(damage, e);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damageArmorMult(float damage, float m){
        if(invincibleTimer > 0f) return;
        super.damageArmorMult(damage, m);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damageArmorMult(float damage, float m, boolean e){
        if(invincibleTimer > 0f) return;
        super.damageArmorMult(damage, m, e);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damageContinuous(float damage){
        if(invincibleTimer > 0f) return;
        super.damageContinuous(damage);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damageContinuousPierce(float damage){
        if(invincibleTimer > 0f) return;
        super.damageContinuousPierce(damage);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }

    @Override
    public void damageContinuousArmorMult(float damage, float m){
        if(invincibleTimer > 0f) return;
        super.damageContinuousArmorMult(damage, m);
        if(type instanceof IronGolemType i) {
            i.hurtSound.at(x, y, 1f + Mathf.range(i.hurtSoundRange), i.hurtSoundVolume);
            invincibleTimer = i.invincibleTime;
        }
    }
}
