
package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.IceUtils;
import data.tools.IntervalTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class MobiusRayAutofireAIPlugin implements AutofireAIPlugin {
    static HashMap autofireMap = new HashMap();
    static final float SHOULD_FIRE_THRESHOLD = 0.3f;
    static final float UPDATE_FREQUENCY = 0.5f;

    public static MobiusRayAutofireAIPlugin get(WeaponAPI weapon) {
        //if(!autofireMap.containsKey(weapon)) return null;
        
        return (MobiusRayAutofireAIPlugin)autofireMap.get(weapon);
    }

    WeaponAPI weapon;
    ShipAPI target;
    boolean isOn = false;
    boolean previouslyCoolingDown = false;
    IntervalTracker timer = new IntervalTracker(UPDATE_FREQUENCY);

    public MobiusRayAutofireAIPlugin() { }
    public MobiusRayAutofireAIPlugin(WeaponAPI weapon) {
        this.weapon = weapon;
        autofireMap.put(weapon, this);
    }

    public boolean isOn() {
        return isOn;
    }
    public void setIsOn(boolean on) {
        isOn = on;
    }

    void findTarget() {
        if(target != null) return;
        
        float theta = (weapon.getCurrAngle() / 180f) * (float)Math.PI;
        float halfRange = weapon.getRange() / 2f;
        Vector2f midPoint = new Vector2f(weapon.getLocation());
        midPoint.x += (float)Math.cos(theta) * halfRange * 0.6f;
        midPoint.y += (float)Math.sin(theta) * halfRange * 0.6f;

        target = null;
        float shortestDistance = Float.MAX_VALUE;
        List friendlies = new ArrayList();

        for(Iterator iter = CombatUtils.getShipsWithinRange(midPoint, halfRange * 1.3f).iterator(); iter.hasNext();) {
            ShipAPI ship = (ShipAPI)iter.next();

            if(!ship.isAlive()
                    || ship.isDrone()
                    || ship == weapon.getShip()
                    || (ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive())
                    ) continue;

            if(ship.getOwner() == weapon.getShip().getOwner()
                    && ship != weapon.getShip())
                friendlies.add(ship);

            float dist = MathUtils.getDistanceSquared(ship, midPoint);

            // Targeted ship more likely to be chosen
            if(ship == weapon.getShip().getShipTarget()) dist *= 0.5f;

            // Ships with high flux more likely to be chosen
            dist *= Math.min(1, (1 - ship.getFluxTracker().getFluxLevel()) + 0.8f);

            if(dist < shortestDistance) {
                shortestDistance = dist;
                target = ship;
            }
        }
        
        if(target == null) return;

        midPoint.x = (weapon.getLocation().x + target.getLocation().x) / 2f;
        midPoint.y = (weapon.getLocation().y + target.getLocation().y) / 2f;
        halfRange = MathUtils.getDistance(weapon.getLocation(), midPoint);

        for(Iterator iter  = friendlies.iterator(); iter.hasNext();) {
            ShipAPI ship = (ShipAPI)iter.next();
            if(MathUtils.getDistance(ship, midPoint) < halfRange) {
                target = null;
                return;
            }
        }
    }

    @Override
    public void advance(float amount) {
        if(previouslyCoolingDown && weapon.getCooldownRemaining() <= 0) {
            isOn = false;
            target = null;
            //IceUtils.print(weapon.getShip(), "DONE!");
        }
        
        if(timer.intervalElapsed()) {
            findTarget();
            //IceUtils.print(weapon.getShip(), "" + target);
        }
        
        previouslyCoolingDown = weapon.getCooldownRemaining() > 0;
    }

    @Override
    public void forceOff() {
        findTarget();
        //IceUtils.print(weapon.getShip(), "" + target);
    }

    @Override
    public Vector2f getTarget() {
        return (target == null)
                ? weapon.getShip().getMouseTarget()
                : target.getLocation();
    }

    @Override
    public ShipAPI getTargetShip() {
        return target;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public boolean shouldFire() {
        isOn = true;

        if(weapon.isDisabled()
                || weapon.getCooldownRemaining() > 0
                || target == null
            ) return false;
        
        float shouldFire = 0;

        // 0.3f - Target has high flux?
        shouldFire += Math.max(0, target.getFluxTracker().getFluxLevel() - 0.7f);
        
        // 0.3 - Target defenses ready?
        if(target.getShield() != null) {
            shouldFire += 0.3f * (1 - target.getShield().getActiveArc() / 360);
        } if(target.getPhaseCloak() != null) {
            shouldFire *= 2;
        } else shouldFire += 0.3f;
        
        // 0.2 - Target damaged enough to kill in one shot?
        shouldFire += 0.2f * (Math.min(1200f, target.getHitpoints()) / 1200);

        // 1.0f - Can we afford to use the flux?
        shouldFire += 1 - weapon.getShip().getFluxTracker().getFluxLevel();

        return shouldFire > SHOULD_FIRE_THRESHOLD;
    }

}
