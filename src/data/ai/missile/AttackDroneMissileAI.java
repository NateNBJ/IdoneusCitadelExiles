package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import data.tools.SunUtils;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class AttackDroneMissileAI extends BaseMissileAI {
    static final String WEAPON_ID = "sun_ice_pulserepeater";
    static final String MISSILE_ID = "sun_ice_attackdrone";
    static final float WEAPON_RANGE = 400f;
    static final float WEAPON_RANGE_SQUARED = WEAPON_RANGE * WEAPON_RANGE;
    static final float POTENTIAL_TARGET_RANGE = WEAPON_RANGE * 2;
    static final float WEAPON_COOLDOWN = 0.2f;
    static final int MAX_AMMO = 1200;
    static final int MAX_ACTIVE_DRONES = 3;

    Vector2f destOffset = new Vector2f();
    Vector2f dest = new Vector2f();
    List potentialTargets = new ArrayList();
    int ammo = MAX_AMMO;
    float weaponCooldown = WEAPON_COOLDOWN;

    public AttackDroneMissileAI(MissileAPI missile) {
        super(missile);

        findTarget();
        this.circumstanceEvaluationTimer.setInterval(0.6f, 1.4f);

        int count = 0;

        Object[] missiles = Global.getCombatEngine().getMissiles().toArray();

        for(int i = missiles.length - 1; i >= 0; --i) {
            MissileAPI m = (MissileAPI)missiles[i];

            if(m.getProjectileSpecId().equals(MISSILE_ID)
                    && m.getWeapon() == missile.getWeapon()) {
                ++count;

                if(count >= MAX_ACTIVE_DRONES) {
                    SunUtils.destroy(m);
                }
            }
        }

        missile.setCollisionClass(CollisionClass.FIGHTER);
    }

//    @Override
//    public void findTarget() {
//        target = missile.getSource().getShipTarget();
//
//        if(target == null || target.getOwner() != missile.getOwner())
//            target = missile.getSource();
//
//        if(!target.isAlive())  target = AIUtils.getNearestAlly(missile);
//    }

    @Override
    public void findTarget() {
        super.findTarget();

        if(target == null) target = missile.getSource();
    }

    @Override
    public void evaluateCircumstances() {
        super.evaluateCircumstances();

        if(target == null || !target.isAlive()) {
            findTarget();
            return;
        }

        Vector2f.sub(MathUtils.getRandomPointInCircle(target.getLocation(),
                target.getCollisionRadius()), target.getLocation(), destOffset);

        if(missile.isFading() || ammo <= 0 || !missile.getSource().isAlive())
            SunUtils.destroy(missile);

        potentialTargets.clear();
        potentialTargets.addAll(AIUtils.getNearbyEnemies(missile, POTENTIAL_TARGET_RANGE));
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        weaponCooldown = Math.max(0, weaponCooldown - amount);

        if(target == null) return;

        Vector2f.add(destOffset, target.getLocation(), dest);

        accelerate();
        turnToward(dest);

        if(ammo > 0 && weaponCooldown == 0) {
            CombatEntityAPI nearest = null;
            float record = Float.MAX_VALUE;

            for(int i = 0; i < potentialTargets.size(); ++i) {
                CombatEntityAPI m = (CombatEntityAPI)potentialTargets.get(i);

                float dist2 = MathUtils.getDistanceSquared(missile, m);

                if(dist2 < record && dist2 <= WEAPON_RANGE_SQUARED) {
                    record = dist2;
                    nearest = m;
                }
            }

            if(nearest != null) {
                Global.getCombatEngine().spawnProjectile(missile.getSource(),
                        null, WEAPON_ID, missile.getLocation(),
                        VectorUtils.getAngle(missile.getLocation(), nearest.getLocation()),
                        new Vector2f());

                --ammo;
                weaponCooldown = WEAPON_COOLDOWN;
            }
        }
    }
}