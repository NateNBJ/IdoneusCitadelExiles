package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.weapons.onhit.HypermassOnHitEffect;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class HypermassDriverEffect implements EveryFrameWeaponEffectPlugin {
    boolean onFireEffectIsReady = true;
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(onFireEffectIsReady && weapon.getCooldownRemaining() > 0) {
            float theta = (float)(weapon.getCurrAngle() / 360 * Math.PI * 2);
            weapon.getShip().getFluxTracker().forceOverload(0);
			Vector2f inverse = new Vector2f(
                    -(float)Math.cos(theta) * weapon.getProjectileSpeed(),
                    -(float)Math.sin(theta) * weapon.getProjectileSpeed());
			CombatUtils.applyForce(weapon.getShip(), inverse, HypermassOnHitEffect.FORCE);

            onFireEffectIsReady = false;
        } else if(!onFireEffectIsReady && weapon.getCooldownRemaining() == 0) {
            onFireEffectIsReady = true;
        }
    }
}