
package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.SunUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class MinePodAutofireAIPlugin implements AutofireAIPlugin {
    static final float SHOULD_FIRE_THRESHOLD = 0.0f;

    WeaponAPI weapon;
    ShipAPI ship;
    Vector2f target = new Vector2f();

    public MinePodAutofireAIPlugin() { }
    public MinePodAutofireAIPlugin(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ship = weapon.getShip();
    }

    ShipAPI findTarget() { return null;  }

    @Override
    public void advance(float amount) {
        
    }
    @Override
    public void forceOff() {
        findTarget();
    }
    @Override
    public Vector2f getTarget() {
        return target;
    }
    @Override
    public ShipAPI getTargetShip() { return null; }
    @Override
    public WeaponAPI getWeapon() { return weapon; }
    @Override
    public boolean shouldFire() {
        float shouldFire = 1;
        
        // - distance from ship
        // - nearby enemies with PD
        // - distance from center
        // + near objectives

        return shouldFire > SHOULD_FIRE_THRESHOLD;
    }

}
