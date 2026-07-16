package VanillaExpansion.expand.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;

public class FixedSpriteBatch extends SpriteBatch{
    public static FixedSpriteBatch batch;
    static Batch lastBatch;

    public static void init(){
        batch = new FixedSpriteBatch();
    }

    public static void beginSwap(){
        lastBatch = Core.batch;
        Mat proj = Draw.proj(), trans = Draw.trans();
        //Draw.flush();
        batch.setFixedShader(VEShaders.pinkShader);
        Core.batch = batch;
        Draw.proj(proj);
        Draw.trans(trans);
    }
    public static void endSwap(){
        Draw.flush();
        Core.batch = lastBatch;
    }

    public FixedSpriteBatch(){
        super(1024, null);
    }

    public void setFixedShader(Shader shader){
        super.setShader(shader, true);
    }
    public void setFixedBlending(Blending blending){
        super.setBlending(blending);
    }

    @Override
    protected void setBlending(Blending blending){}

    @Override
    protected void setShader(Shader shader, boolean apply) {

    }

    public void setFixedAlpha(float fade) {
    }
}
