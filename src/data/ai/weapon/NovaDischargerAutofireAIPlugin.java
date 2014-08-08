
package data.ai.weapon;

import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class NovaDischargerAutofireAIPlugin implements AutofireAIPlugin {
    static final float UPDATE_FREQUENCY = 0.3f;

    WeaponAPI weapon;
    ShipAPI ship;
    ShipAPI target;
    IntervalTracker timer = new IntervalTracker(UPDATE_FREQUENCY);
    
    public NovaDischargerAutofireAIPlugin() { }
    public NovaDischargerAutofireAIPlugin(WeaponAPI weapon) {
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
        if(timer.intervalElapsed()) {
            findTarget();
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
        return target != null
                && !target.getFluxTracker().isOverloadedOrVenting()
                && target.getShield() != null
                && target.getShield().isOn()
                && (target.getPhaseCloak() == null || !target.getPhaseCloak().isActive());
    }
}