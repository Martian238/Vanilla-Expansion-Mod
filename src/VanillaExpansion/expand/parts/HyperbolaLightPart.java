package VanillaExpansion.expand.parts;

import VanillaExpansion.VESettings;
import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Nullable;
import arc.util.Tmp;
import mindustry.entities.part.DrawPart;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

import java.awt.*;

import static mindustry.Vars.*;

public class HyperbolaLightPart extends DrawPart {
    public HyperbolaLightPart(){}

    public float a = tilesize;
    public float b = 2 * tilesize;

    public float drawInterval = tilesize;
    public int times = 20;
    public float offset = 0.001f;
    public float opacity = 1f;
    public float opacityPow = 2f;
    public Color color = Pal.power;
    public @Nullable Color colorTo;
    public boolean useProgress = false;
    public PartProgress progress = PartProgress.warmup;
    public PartProgress colorProgress = PartProgress.warmup;
    public float x, y, rotation;
    public boolean extra = false;
    public float extraDrawIntervalMultiplier = 0.25f;
    public float extraAlpha = 0.4f;
    public float extraAlphaPow = 4f;
    public float extraSizeMultiplier = 1.4f;
    public boolean shrink = true;
    public boolean mirror = false;
    /** 抛物线光束 */
    public boolean parabola = false;
    public float c = tilesize;
    /** 梯形光束 */
    public boolean linear = false;
    public float d = 0.05f;
    /** 依靠设置项启用 */
    public boolean settingOnly = false;


    public TextureRegion circleSoft = new TextureRegion();

    @Override
    public void load(String name){
        if(headless) return;
        circleSoft = Core.atlas.find("ve-circle-soft");
    }

    @Override
    public void draw(PartParams params) {
        if(headless || (!state.rules.lighting && !state.isCampaign()) || (settingOnly && !VESettings.getBool("ve-core-unit-light"))) return;
        float prog = useProgress ? progress.getClamp(params, true) : 1f;
        if(prog <= 0) return;

        float colProg = colorTo == null ? 0 : colorProgress.getClamp(params, true);
        Color col = colorTo == null ? color : color.cpy().lerp(colorTo, colProg);

        int t = Mathf.ceil(times * prog);

        float z = Draw.z();
        Draw.z(Layer.light);

        Tmp.v1.set(x, y).rotate(params.rotation - 90f);
        float px = params.x + Tmp.v1.x;
        float py = params.y + Tmp.v1.y;

        Draw.blend(Blending.additive);
        Draw.color(col);

        if(!parabola && !linear) {

            //双曲线

            float b = shrink ? Math.max(this.b * prog, offset) : this.b;

            for (int i = 0; i < t; i++) {
                double x = i * drawInterval + a + offset;
                double a2 = a * a;
                double x2 = x * x;
                double tmp1 = Math.sqrt(x2 / a2 - 1);
                double tmp2 = 2 / a2;
                double y = b * tmp1;
                double dy = b * tmp2 * x / tmp1;
                double d2y = b * tmp2 * (1 / tmp1 - 0.5f * tmp2 * x2 / ((tmp1) * (x2 / a2 - 1)));
                double tmp3 = 1 + dy * dy;
                double R = tmp3 * Math.sqrt(tmp3) / Math.abs(d2y);
                double xR = x + Math.sqrt(R * R - y * y);
                double xr = x + y / dy;
                double r = y * Math.sqrt(1 / (dy * dy) + 1);

                float resultD, resultR,
                        resultO = prog * opacity * Mathf.pow((float) (t - i) / t, opacityPow);
                if (Double.isNaN(r) || r > R) {
                    resultD = (float) xR;
                    resultR = (float) R;
                } else {
                    resultD = (float) xr;
                    resultR = (float) r;
                }

                Drawf.light(px + resultD * Mathf.cosDeg(params.rotation + rotation),
                        py + resultD * Mathf.sinDeg(params.rotation + rotation),
                        resultR, col, resultO);
                if (mirror) Drawf.light(px - resultD * Mathf.cosDeg(params.rotation + rotation),
                        py - resultD * Mathf.sinDeg(params.rotation + rotation),
                        resultR, col, resultO);

                if (extra && extraDrawIntervalMultiplier == 1f) {
                    Draw.alpha(prog * extraAlpha * Mathf.pow((float) (t - i) / t, extraAlphaPow));
                    Draw.rect(circleSoft, px + resultD * Mathf.cosDeg(params.rotation + rotation),
                            py + resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                    if (mirror) Draw.rect(circleSoft, px - resultD * Mathf.cosDeg(params.rotation + rotation),
                            py - resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                }
            }

            if (extra && extraDrawIntervalMultiplier != 1f) {
                int te = Mathf.ceil(times * prog / extraDrawIntervalMultiplier);
                for (int i = 0; i < te; i++) {
                    double x = i * drawInterval * extraDrawIntervalMultiplier + a + offset;
                    double a2 = a * a;
                    double x2 = x * x;
                    double tmp1 = Math.sqrt(x2 / a2 - 1);
                    double tmp2 = 2 / a2;
                    double y = b * tmp1;
                    double dy = b * tmp2 * x / tmp1;
                    double d2y = b * tmp2 * (1 / tmp1 + tmp2 * x2 / ((tmp1) * (x2 / a2 - 1)));
                    double tmp3 = 1 + dy * dy;
                    double R = tmp3 * Math.sqrt(tmp3) / Math.abs(d2y);
                    double xR = x + Math.sqrt(R * R - y * y);
                    double xr = x + y / dy;
                    double r = y * Math.sqrt(1 / (dy * dy) + 1);

                    float resultD, resultR;
                    if (Double.isNaN(r) || r > R) {
                        resultD = (float) xR;
                        resultR = (float) R;
                    } else {
                        resultD = (float) xr;
                        resultR = (float) r;
                    }
                    Draw.alpha(prog * extraAlpha * Mathf.pow((float) (te - i) / te, extraAlphaPow));
                    Draw.rect(circleSoft, px + resultD * Mathf.cosDeg(params.rotation + rotation),
                            py + resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                    if (mirror) Draw.rect(circleSoft, px - resultD * Mathf.cosDeg(params.rotation + rotation),
                            py - resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                }
            }

        }else if (!linear){

            //抛物线

            float c = shrink ? Math.max(this.c * prog, offset) : this.c;

            for (int i = 0; i < t; i++) {
                double x = i * drawInterval + offset;
                double tmp1 = Math.sqrt(c * x);
                double y = tmp1;
                double dy = c / tmp1;
                double d2y = -0.5f * c * c / (c * x * tmp1);
                double tmp3 = 1 + dy * dy;
                double R = tmp3 * Math.sqrt(tmp3) / Math.abs(d2y);
                double xR = x + Math.sqrt(R * R - y * y);
                double xr = x + y / dy;
                double r = y * Math.sqrt(1 / (dy * dy) + 1);

                float resultD, resultR,
                        resultO = prog * opacity * Mathf.pow((float) (t - i) / t, opacityPow);
                if (Double.isNaN(r) || r > R) {
                    resultD = (float) xR;
                    resultR = (float) R;
                } else {
                    resultD = (float) xr;
                    resultR = (float) r;
                }

                Drawf.light(px + resultD * Mathf.cosDeg(params.rotation + rotation),
                        py + resultD * Mathf.sinDeg(params.rotation + rotation),
                        resultR, col, resultO);
                if (mirror) Drawf.light(px - resultD * Mathf.cosDeg(params.rotation + rotation),
                        py - resultD * Mathf.sinDeg(params.rotation + rotation),
                        resultR, col, resultO);

                if (extra && extraDrawIntervalMultiplier == 1f) {
                    Draw.alpha(prog * extraAlpha * Mathf.pow((float) (t - i) / t, extraAlphaPow));
                    Draw.rect(circleSoft, px + resultD * Mathf.cosDeg(params.rotation + rotation),
                            py + resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                    if (mirror) Draw.rect(circleSoft, px - resultD * Mathf.cosDeg(params.rotation + rotation),
                            py - resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                }
            }

            if (extra && extraDrawIntervalMultiplier != 1f) {
                int te = Mathf.ceil(times * prog / extraDrawIntervalMultiplier);
                for (int i = 0; i < te; i++) {
                    double x = i * drawInterval * extraDrawIntervalMultiplier + offset;
                    double tmp1 = Math.sqrt(c * x);
                    double y = tmp1;
                    double dy = c / tmp1;
                    double d2y = -0.5f * c * c / (c * x * tmp1);
                    double tmp3 = 1 + dy * dy;
                    double R = tmp3 * Math.sqrt(tmp3) / Math.abs(d2y);
                    double xR = x + Math.sqrt(R * R - y * y);
                    double xr = x + y / dy;
                    double r = y * Math.sqrt(1 / (dy * dy) + 1);

                    float resultD, resultR;
                    if (Double.isNaN(r) || r > R) {
                        resultD = (float) xR;
                        resultR = (float) R;
                    } else {
                        resultD = (float) xr;
                        resultR = (float) r;
                    }
                    Draw.alpha(prog * extraAlpha * Mathf.pow((float) (te - i) / te, extraAlphaPow));
                    Draw.rect(circleSoft, px + resultD * Mathf.cosDeg(params.rotation + rotation),
                            py + resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                    if (mirror) Draw.rect(circleSoft, px - resultD * Mathf.cosDeg(params.rotation + rotation),
                            py - resultD * Mathf.sinDeg(params.rotation + rotation),
                            resultR * extraSizeMultiplier, resultR * extraSizeMultiplier);
                }
            }

        }else{

            //梯形

            for(int i = 0; i < t; i++){
                float x = i * drawInterval;
                float r = (x * d + c) * prog;
                float resultO = prog * opacity * Mathf.pow((float) (t - i) / t, opacityPow);
                Drawf.light(px + x * Mathf.cosDeg(params.rotation + rotation),
                        py + x * Mathf.sinDeg(params.rotation + rotation),
                        r, col, resultO);
                if (mirror) Drawf.light(px - x * Mathf.cosDeg(params.rotation + rotation),
                        py - x * Mathf.sinDeg(params.rotation + rotation),
                        r, col, resultO);
                if (extra && extraDrawIntervalMultiplier == 1f) {
                    Draw.alpha(prog * extraAlpha * Mathf.pow((float) (t - i) / t, extraAlphaPow));
                    Draw.rect(circleSoft, px + x * Mathf.cosDeg(params.rotation + rotation),
                            py + x * Mathf.sinDeg(params.rotation + rotation),
                            r * extraSizeMultiplier, r * extraSizeMultiplier);
                    if (mirror) Draw.rect(circleSoft, px - x * Mathf.cosDeg(params.rotation + rotation),
                            py - x * Mathf.sinDeg(params.rotation + rotation),
                            r * extraSizeMultiplier, r * extraSizeMultiplier);
                }
            }

            if (extra && extraDrawIntervalMultiplier != 1f) {
                int te = Mathf.ceil(times * prog / extraDrawIntervalMultiplier);
                for (int i = 0; i < te; i++) {
                    float x = i * drawInterval * extraDrawIntervalMultiplier;
                    float r = (x * d + c) * prog;
                    Draw.alpha(prog * extraAlpha * Mathf.pow((float) (te - i) / te, extraAlphaPow));
                    Draw.rect(circleSoft, px + x * Mathf.cosDeg(params.rotation + rotation),
                            py + x * Mathf.sinDeg(params.rotation + rotation),
                            r * extraSizeMultiplier, r * extraSizeMultiplier);
                    if (mirror) Draw.rect(circleSoft, px - x * Mathf.cosDeg(params.rotation + rotation),
                            py - x * Mathf.sinDeg(params.rotation + rotation),
                            r * extraSizeMultiplier, r * extraSizeMultiplier);
                }
            }
        }

        Draw.z(z);
        Draw.blend();
        Draw.color();
    }
}
