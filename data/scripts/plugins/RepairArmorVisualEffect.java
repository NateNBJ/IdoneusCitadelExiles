
package data.scripts.plugins;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.shipsystems.scripts.RepairArmorStats;

public class RepairArmorVisualEffect implements EveryFrameWeaponEffectPlugin {
    private static float ACTIVATION_SPEED = 0.3f;
    private static float DEACTIVATION_SPEED = 0.5f;

    private float alpha = 0;
	
    @Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(engine.isPaused()) return;
        
		boolean on = weapon.getShip().getSystem().isOn()
                && weapon.getShip().isAlive();

        if (alpha == 0 && !on) {
            weapon.getAnimation().setFrame(0);
            return;
        }

        weapon.getAnimation().setFrame(1);
        
        alpha += engine.getElapsedInLastFrame() * (on ? ACTIVATION_SPEED : -DEACTIVATION_SPEED);
        alpha = Math.max(Math.min(alpha, 1), 0);
        
        weapon.getAnimation().setAlphaMult(
                alpha * (0.4f + RepairArmorStats.getPotency(weapon.getShip()) * 0.6f));
	}
}

