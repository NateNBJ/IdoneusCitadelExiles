
package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.IceUtils;
import org.lazywizard.lazylib.MathUtils;
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
        target = IceUtils.getShipInLineOfFire(weapon);
        
        if(target != null && target.getOwner() != ship.getOwner()) {
            return target;
        } else return target = null;
    }

    @Override
    public void advance(float amount) {
        if(Global.getCombatEngine() == null) return;
        
        float t = Global.getCombatEngine().getTotalElapsedTime(false);
        if(t > timeOfNextUpdate) {
            timeOfNextUpdate = t + UPDATE_FREQUENCY;

            if(findTarget() == null) return;

            float selfOverloadTime = IceUtils.getBaseOverloadDuration(ship);
            float targetOverloadTime = IceUtils.estimateOverloadDurationOnHit(target,
                    weapon.getDerivedStats().getDamagePerShot(), weapon.getDamageType());
            float incomingMissileDamage = IceUtils.estimateIncomingMissileDamage(ship);
            float fpOfSupport = IceUtils.getFPWorthOfSupport(ship, 2000);
            float fpOfEnemies = IceUtils.getFPWorthOfHostility(ship, 2000);
            fpOfEnemies = Math.max(0, fpOfEnemies - IceUtils.getFP(target) / 2);
            
            hitChance = IceUtils.getHitChance(weapon, target);
            targetWillDieSoonAnyway = (IceUtils.getLifeExpectancy(ship) < 3);
            overloadBalance = targetOverloadTime - selfOverloadTime;
            fpRatio = IceUtils.getFPStrength(target) / IceUtils.getFPStrength(ship);
            danger = Math.max(0, fpOfEnemies - fpOfSupport + incomingMissileDamage / 100f);
        }
    }
    @Override
    public void forceOff() {
        if(Global.getCombatEngine() == null) return;
        
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
            shouldFire += overloadBalance * 8 * fpRatio;
        } else {
            shouldFire += Math.min(1, weapon.getDerivedStats().getDamagePerShot() / target.getHitpoints()) * 16 * fpRatio;
        }

        shouldFire *= hitChance;

        return shouldFire > SHOULD_FIRE_THRESHOLD;
    }

}
