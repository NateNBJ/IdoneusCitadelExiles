
package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.ai.missile.PdDroneMissileAI;
import data.tools.IntervalTracker;
import org.lwjgl.util.vector.Vector2f;

public class PdDroneAutofireAIPlugin implements AutofireAIPlugin {
    static final float UPDATE_FREQUENCY = 0.5f;

    WeaponAPI weapon;
    ShipAPI ship;
    ShipAPI target;
    IntervalTracker timer = new IntervalTracker(UPDATE_FREQUENCY);
    
    public PdDroneAutofireAIPlugin() { }
    public PdDroneAutofireAIPlugin(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();
    }

    int countDrones() {
        int accumulator = 0;
        
        for(MissileAPI missile : Global.getCombatEngine().getMissiles()) {
            if(missile.getWeapon() == weapon) ++accumulator;
        }
        
        return accumulator;
    }
    ShipAPI findTarget() {
        target = weapon.getShip();
        return target;
    }

    @Override
    public void advance(float amount) {
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
        return timer.intervalElapsed()
                && countDrones() < PdDroneMissileAI.MAX_ACTIVE_DRONES;
    }
}