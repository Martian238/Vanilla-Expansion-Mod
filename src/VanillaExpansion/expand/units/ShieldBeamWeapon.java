package VanillaExpansion.expand.units;

import VanillaExpansion.expand.abilities.RotatingShieldArcAbility;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Sized;
import mindustry.entities.Units;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.ShieldArcAbility;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.units.RepairTurret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class ShieldBeamWeapon extends Weapon {
    public ShieldBeamWeapon() {};
    public ShieldBeamWeapon(String name){
        super(name);
    }

    public boolean targetBuildings = true, targetUnits = true;

    public float repairSpeed = 0.5f;
    public float fractionRepairSpeed = 0f;
    public float beamWidth = 1f;
    public float pulseRadius = 6f;
    public float pulseStroke = 2f;
    public float widthSinMag = 0f, widthSinScl = 4f;

    public float max = 500f;
    public boolean repairForceFields = true;

    public TextureRegion laser, laserEnd, laserTop, laserTopEnd;

    public @Nullable Color laserColor;
    public Color laserTopColor = Color.white.cpy();

    private float blockShieldMax = 0;

    {
        //must be >0 to prevent various bugs
        reload = 1f;
        predictTarget = false;
        autoTarget = true;
        controllable = false;
        rotate = true;
        mountType = HealBeamMount2::new;
        recoil = 0f;
        noAttack = true;
        useAttackRange = false;
        activeSound = Sounds.beamHeal;
    }

    @Override
    public void addStats(UnitType u, Table w){
        w.row();
        w.add("[lightgray]" + Stat.repairSpeed.localized() + ": " + (mirror ? "2x " : "") + "[white]" + (int)(repairSpeed * 60) + " " + StatUnit.perSecond.localized());
        w.row();
        w.add("[lightgray]" + Stat.shieldHealth.localized() + ": [white]" + max);
    }

    @Override
    public float dps(){
        return 0f;
    }

    @Override
    public void load(){
        super.load();

        laser = Core.atlas.find("laser-white");
        laserEnd = Core.atlas.find("laser-white-end");
        laserTop = Core.atlas.find("laser-top");
        laserTopEnd = Core.atlas.find("laser-top-end");
    }

    @Override
    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        var out = targetUnits ? Units.closest(unit.team, x, y, range, u -> u != unit && (u.shield < max || shieldAbilityDamaged(u))) :  null;
        if(out != null || !targetBuildings ||!repairForceFields) return out;
        Building r = Units.findAllyTile(unit.team, x, y, range, b -> b instanceof ForceProjector.ForceBuild fb && fb.buildup > 0);
        if(r != null) blockShieldMax = ((ForceProjector)r.block).shieldHealth; return r;
    }

    private boolean shieldAbilityDamaged(Unit u){
        if(!repairForceFields) return false;
        for (int i = 0; i < u.abilities.length; i++) {
            if (u.abilities[i] != null && u.abilities[i] instanceof ForceFieldAbility ab) {
                if(ab.data < ab.max) return true;
            }
            if (u.abilities[i] != null && u.abilities[i] instanceof ShieldArcAbility ab) {
                if(ab.data < ab.max) return true;
            }
            if (u.abilities[i] != null && u.abilities[i] instanceof RotatingShieldArcAbility ab) {
                if(ab.data < ab.max) return true;
            }
        }
        return false;
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return !(target.within(unit, range + unit.hitSize/2f) && target.team() == unit.team && ((target instanceof Unit u && (u.shield < max || shieldAbilityDamaged(u)) && u.isValid()) || (target instanceof ForceProjector.ForceBuild f && f.buildup < blockShieldMax && f.isValid())));
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
        //does nothing, shooting is handled in update()
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        super.update(unit, mount);

        float
                weaponRotation = unit.rotation - 90,
                wx = unit.x + Angles.trnsx(weaponRotation, x, y),
                wy = unit.y + Angles.trnsy(weaponRotation, x, y);

        HealBeamMount2 heal = (HealBeamMount2)mount;
        boolean canShoot = mount.shoot;

        if(!autoTarget){
            heal.target = null;
            if(canShoot){
                heal.lastEnd.set(heal.aimX, heal.aimY);

                if(!rotate && !Angles.within(Angles.angle(wx, wy, heal.aimX, heal.aimY), unit.rotation, shootCone)){
                    canShoot = false;
                }
            }

            //limit range
            heal.lastEnd.sub(wx, wy).limit(range()).add(wx, wy);
        }

        heal.strength = Mathf.lerpDelta(heal.strength, Mathf.num(autoTarget ? mount.target != null : canShoot), 0.2f);

        if(canShoot && mount.target instanceof Unit u){
            float baseAmount = repairSpeed * heal.strength * Time.delta + fractionRepairSpeed * heal.strength * Time.delta * u.maxHealth() / 100f;
            if(u.shield + baseAmount > max && u.shield < max) u.shield(max);
            else if(u.shield < max) u.shield(u.shield + baseAmount);
            u.shieldAlpha = 1f;
            if(repairForceFields) {
                for (int i = 0; i < u.abilities.length; i++) {
                    if (u.abilities[i] != null && u.abilities[i] instanceof ForceFieldAbility ab) {
                        if(ab.data + baseAmount > ab.max && ab.data < ab.max) ab.data = ab.max;
                        else if(ab.data < ab.max) ab.data += baseAmount;
                    }
                    if (u.abilities[i] != null && u.abilities[i] instanceof ShieldArcAbility ab) {
                        if(ab.data + baseAmount > ab.max && ab.data < ab.max) ab.data = ab.max;
                        else if(ab.data < ab.max) ab.data += baseAmount;
                    }
                    if (u.abilities[i] != null && u.abilities[i] instanceof RotatingShieldArcAbility ab) {
                        if(ab.data + baseAmount > ab.max && ab.data < ab.max) ab.data = ab.max;
                        else if(ab.data < ab.max) ab.data += baseAmount;
                    }
                }
            }
        }
        if(canShoot && mount.target instanceof ForceProjector.ForceBuild u && blockShieldMax > 0){
            float baseAmount = repairSpeed * heal.strength * Time.delta + fractionRepairSpeed * heal.strength * Time.delta * blockShieldMax / 100f;
            if(u.buildup > baseAmount) u.buildup -= baseAmount;
            else if (u.buildup > 0) u.buildup = 0;
        }
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
        super.draw(unit, mount);

        HealBeamMount2 heal = (HealBeamMount2)mount;

        if(unit.canShoot()){
            float
                    weaponRotation = unit.rotation - 90,
                    wx = unit.x + Angles.trnsx(weaponRotation, x, y),
                    wy = unit.y + Angles.trnsy(weaponRotation, x, y),
                    z = Draw.z();
            RepairTurret.drawBeam(wx, wy, unit.rotation + mount.rotation, shootY, unit.id, mount.target == null || controllable ? null : (Sized)mount.target, unit.team, heal.strength,
                    pulseStroke, pulseRadius, beamWidth + Mathf.absin(widthSinScl, widthSinMag), heal.lastEnd, heal.offset, laserColor == null ? unit.team.color : laserColor, laserTopColor,
                    laser, laserEnd, laserTop, laserTopEnd);
            Draw.z(z);
        }
    }

    @Override
    public void init(){
        super.init();
        bullet.healPercent = fractionRepairSpeed;
    }

    public static class HealBeamMount2 extends WeaponMount{
        public Vec2 offset = new Vec2(), lastEnd = new Vec2();
        public float strength;

        public HealBeamMount2(Weapon weapon){
            super(weapon);
        }
    }
}
