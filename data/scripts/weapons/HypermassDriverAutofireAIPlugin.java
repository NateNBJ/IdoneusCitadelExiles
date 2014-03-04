
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.plugins.SunUtils;
import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class HypermassDriverAutofireAIPlugin implements AutofireAIPlugin {
    static final float SHOULD_FIRE_THRESHOLD = 0.0f;
    static final float UPDATE_FREQUENCY = 0.3f;

    WeaponAPI weapon;
    ShipAPI ship;
    ShipAPI target;
    float timeOfNextUpdate = 0;

    boolean targetWillDieSoonAnyway = false;
    float hitChance, danger, overloadBalance, fpRatio;

    public HypermassDriverAutofireAIPlugin() { }
    public HypermassDriverAutofireAIPlugin(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();
    }

    ShipAPI findTarget() {
        ShipAPI ally = WeaponUtils.getNearestAllyInArc(weapon);
        ShipAPI enemy = WeaponUtils.getNearestEnemyInArc(weapon);

        target = enemy != null && enemy.isAlive()
                && (ally == null || MathUtils.getDistance(ship, ally) > MathUtils.getDistance(ship, enemy))
            ? enemy : null;

        return target;
    }

    @Override
    public void advance(float amount) {
        float t = Global.getCombatEngine().getTotalElapsedTime(false);
        if(t > timeOfNextUpdate) {
            timeOfNextUpdate = t + UPDATE_FREQUENCY;

            if(findTarget() == null) return;

            float selfOverloadTime = SunUtils.getBaseOverloadDuration(ship);
            float targetOverloadTime = SunUtils.estimateOverloadDurationOnHit(target,
                    weapon.getDerivedStats().getDamagePerShot(), weapon.getDamageType());
            float incomingMissileDamage = SunUtils.estimateIncomingMissileDamage(ship);
            float fpOfSupport = SunUtils.getFPWorthOfSupport(ship, 2000);
            float fpOfEnemies = SunUtils.getFPWorthOfEnemies(ship, 2000);
            fpOfEnemies = Math.max(0, fpOfEnemies -SunUtils.getFP(target) / 2);
            
            hitChance = SunUtils.getHitChance(weapon, target);
            targetWillDieSoonAnyway = (SunUtils.getLifeExpectancy(ship) < 3);
            overloadBalance = targetOverloadTime - selfOverloadTime;
            fpRatio = SunUtils.getFP(target) / SunUtils.getFP(ship);
            danger = Math.max(0, fpOfEnemies - fpOfSupport + incomingMissileDamage / 100f);
        }
    }
    @Override
    public void forceOff() {
        findTarget();
    }
    @Override
    public Vector2f getTarget() {
        return ship.getMouseTarget();
    }
    @Override
    public ShipAPI getTargetShip() { return target; }
    @Override
    public WeaponAPI getWeapon() { return weapon; }
    @Override
    public boolean shouldFire() {
        if(target == null || !target.isAlive() || targetWillDieSoonAnyway
                || (target.getPhaseCloak() != null
                    && !target.getFluxTracker().isOverloadedOrVenting()))
            return false;

        float shouldFire = -danger;

        if(target.getShield() != null && target.getShield().isWithinArc(weapon.getLocation())) {
            shouldFire += overloadBalance * 4 * fpRatio;
        } else {
            shouldFire += Math.min(1, weapon.getDerivedStats().getDamagePerShot() / target.getHitpoints()) * 10 * fpRatio;
        }

        shouldFire *= hitChance;

        return shouldFire > SHOULD_FIRE_THRESHOLD;
    }

}