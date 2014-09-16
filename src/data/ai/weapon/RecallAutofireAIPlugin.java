
package data.ai.weapon;

import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.IntervalTracker;
import data.weapons.beam.RecallBeamEffect;
import java.util.LinkedList;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class RecallAutofireAIPlugin implements AutofireAIPlugin {
    static final float UPDATE_FREQUENCY = 0.3f;

    WeaponAPI weapon;
    ShipAPI ship;
    IntervalTracker timer = new IntervalTracker(UPDATE_FREQUENCY);
    boolean shouldFire = false;
    
    public RecallAutofireAIPlugin() { }
    public RecallAutofireAIPlugin(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();
    }

    @Override
    public void advance(float amount) {
        if(timer.intervalElapsed()) {
            shouldFire = ship.getFluxTracker().getCurrFlux() + weapon.getFluxCostToFire() < ship.getFluxTracker().getMaxFlux()
                    && RecallBeamEffect.getCumulativeRecallPriority(weapon) > 0;
        }
    }
    @Override
    public void forceOff() { }
    @Override
    public Vector2f getTarget() {
        return ship.getMouseTarget();
    }
    @Override
    public ShipAPI getTargetShip() { return null; }
    @Override
    public WeaponAPI getWeapon() { return weapon; }
    @Override
    public boolean shouldFire() {
        return shouldFire;
    }
}