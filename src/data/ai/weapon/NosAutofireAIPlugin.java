
package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.IntervalTracker;
import data.tools.IceUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class NosAutofireAIPlugin implements AutofireAIPlugin {
    static final float UPDATE_FREQUENCY = 0.25f;

    WeaponAPI weapon;
    ShipAPI ship;
    ShipAPI target;
    Vector2f targetVect;
    IntervalTracker timer = new IntervalTracker(UPDATE_FREQUENCY);
    boolean shouldFire = false;
    
    float getRangeToHit(Vector2f edgePoint) {
        Vector2f t = VectorUtils.getDirectionalVector(weapon.getLocation(), edgePoint);
        t.scale(weapon.getRange());
        Vector2f.add(weapon.getLocation(), t, t);
        return MathUtils.getDistance(weapon.getLocation(), edgePoint);
    }
    
    public NosAutofireAIPlugin() { }
    public NosAutofireAIPlugin(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();
    }

    ShipAPI findTarget() {
        ShipAPI enemy = WeaponUtils.getNearestEnemyInArc(weapon);

        return target = enemy != null && enemy.isAlive() ? enemy : null;
    }

    @Override
    public void advance(float amount) {
        if(Global.getCombatEngine() == null) return;
        
        if(timer.intervalElapsed()) {
            findTarget();
            shouldFire = target != null
                    && (target.getPhaseCloak() == null || !target.getPhaseCloak().isActive())
                    && (target.getShield() == null || target.getShield().isOff()
                        || !target.getShield().isWithinArc(weapon.getLocation()))
                    && IceUtils.getShipInLineOfFire(weapon) == target;
        }
    }
    @Override
    public void forceOff() {
        if(Global.getCombatEngine() == null) return;
        
        findTarget();
    }
    @Override
    public Vector2f getTarget() { return ship.getMouseTarget(); }
    @Override
    public ShipAPI getTargetShip() { return target; }
    @Override
    public WeaponAPI getWeapon() { return weapon; }
    @Override
    public boolean shouldFire() {
        return shouldFire;
    }
}