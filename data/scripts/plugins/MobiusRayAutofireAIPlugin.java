
package data.scripts.plugins;

import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class MobiusRayAutofireAIPlugin implements AutofireAIPlugin {

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void forceOff() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Vector2f getTarget() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ShipAPI getTargetShip() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WeaponAPI getWeapon() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean shouldFire() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
