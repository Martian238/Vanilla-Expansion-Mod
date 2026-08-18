package VanillaExpansion.expand.exp;

import arc.math.*;

/** Exp 经验球占位实现，后续随完整 ExpOrbs(BulletType) 移植补充 */
public class ExpOrbs{
    public static final int expAmount = 10;

    public static void spreadExp(float x, float y, int amount){
    }

    public static void spreadExp(float x, float y, int amount, float v){
    }

    public static void spreadExp(float x, float y, float amount, float v){
        spreadExp(x, y, Mathf.ceilPositive(amount), v);
    }

    public static void dropExp(float x, float y, float rotation){
    }

    public static void dropExp(float x, float y, float rotation, float v, int amount){
    }

    public static int orbs(int exp){
        return exp / expAmount;
    }

    public static int convertedExp(int exp){
        return (exp / expAmount) * expAmount;
    }

    public static int oneOrb(int exp){
        return exp < expAmount ? 0 : expAmount;
    }
}