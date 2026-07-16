package VanillaExpansion.expand.graphics;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;

public class PointTrail{
    FloatSeq points = new FloatSeq();
    int length = 20;
    static float[] verts = new float[4 * 6];

    public void update(float x, float y, float x2, float y2){
        if(points.size >= length * 4) points.removeRange(0, 3);
        points.add(x, y, x2, y2);
    }

    public void clear(){
        points.clear();
    }

    public void draw(){
        int s = (points.size / 4) - 1;
        float[] items = points.items;

        float col = Draw.getColor().toFloatBits(), mcol = Draw.getMixColor().toFloatBits();

        for(int i = 0; i < s; i++){
            float f1 = i / (float)s;
            float f2 = (i + 1f) / s;

            int i1 = i * 4;
            int i2 = (i + 1) * 4;


            float x1 = items[i1], y1 = items[i1 + 1];
            float x2 = items[i1 + 2], y2 = items[i1 + 3];
            float mx1 = (x1 + x2) / 2f, my1 = (y1 + y2) / 2f;
            x1 = (x1 - mx1) * f1 + mx1;
            x2 = (x2 - mx1) * f1 + mx1;
            y1 = (y1 - my1) * f1 + my1;
            y2 = (y2 - my1) * f1 + my1;

            float x21 = items[i2], y21 = items[i2 + 1];
            float x22 = items[i2 + 2], y22 = items[i2 + 3];
            float mx2 = (x21 + x22) / 2f, my2 = (y21 + y22) / 2f;
            x21 = (x21 - mx2) * f2 + mx2;
            x22 = (x22 - mx2) * f2 + mx2;
            y21 = (y21 - my2) * f2 + my2;
            y22 = (y22 - my2) * f2 + my2;

            //Fill.quad(x1, y1, x2, y2, x22, y21, x21, y22);
            //Fill.quad(x1, y1, x2, y2, x21, y22, x22, y21);
            //Fill.quad(x1, y1, x2, y2, x22, y22, x21, y21);

            verts[0] = x1;
            verts[1] = y1;
            verts[2] = col;
            verts[5] = mcol;

            verts[6] = x2;
            verts[7] = y2;
            verts[8] = col;
            verts[11] = mcol;

            verts[12] = x22;
            verts[13] = y22;
            verts[14] = col;
            verts[17] = mcol;

            verts[18] = x21;
            verts[19] = y21;
            verts[20] = col;
            verts[23] = mcol;

        }
    }
}
