package VanillaExpansion.expand.util;

import arc.func.Cons2;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Geometry;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.Lightning;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.graphics.Drawf;

public class PosLightning {
    public static final float WIDTH = 2.5f;
    public static final float RANGE_RAND = 5f;
    public static final float ROT_DST = 9f;

    public static void createEffect(Position from, Position to, Color color, int bolts, float thick) {
        float fx = from.getX(), fy = from.getY();
        float tx = to.getX(), ty = to.getY();

        for (int j = 0; j < bolts; j++) {
            Lines.stroke(thick * Mathf.random(0.8f, 1.2f));
            Draw.color(color);

            Lines.line(fx, fy, tx, ty, false);

            float len = Mathf.dst(fx, fy, tx, ty);
            float angle = Mathf.atan2(tx - fx, ty - fy);

            Rand r = new Rand((long)fx + (long)fy * 1000 + j);
            float px = fx, py = fy;
            for (float i = ROT_DST; i < len; i += ROT_DST) {
                float ox = px + Angles.trnsx(angle, ROT_DST) + r.range(RANGE_RAND);
                float oy = py + Angles.trnsy(angle, ROT_DST) + r.range(RANGE_RAND);
                Lines.line(px, py, ox, oy, false);
                px = ox;
                py = oy;
            }
            Lines.line(px, py, tx, ty, false);
        }
        Draw.reset();

        Drawf.light(fx, fy, tx, ty, thick * 3f, color, 0.6f);
    }

    public static Position findInterceptedPoint(Position from, Position to, mindustry.game.Team team) {
        return to;
    }
}