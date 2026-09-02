package VanillaExpansion;

import arc.math.Mathf;

public class Mathve {

    //储存

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }
}
