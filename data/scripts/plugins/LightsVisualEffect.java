
package data.scripts.plugins;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.mission.FleetSide;

public class LightsVisualEffect implements EveryFrameWeaponEffectPlugin {
    private static float SECONDS_TO_ACTIVATE = 1;
    private static float SECONDS_TO_DEACTIVATE = 1;

    private float alpha = 0.7f;
	
    @Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(engine.isPaused()) return;
        
		boolean on = !weapon.getShip().getSystem().isActive()
                && weapon.getShip().isAlive()
                && !weapon.getShip().getFluxTracker().isOverloaded();

        if (alpha == 0 && !on) return;

        float wave = (float)Math.cos(engine.getTotalElapsedTime(false) * Math.PI);
        wave *= (float)Math.cos(engine.getTotalElapsedTime(false) * Math.E / 3);
        alpha += engine.getElapsedInLastFrame() * (on ? SECONDS_TO_ACTIVATE : -SECONDS_TO_DEACTIVATE);
        alpha = Math.max(Math.min(alpha, 1), 0);
        
        weapon.getAnimation().setAlphaMult(alpha * (wave / 3 + 0.66f));
	}}

