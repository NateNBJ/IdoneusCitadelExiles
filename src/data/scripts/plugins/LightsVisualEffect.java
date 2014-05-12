
package data.scripts.plugins;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.util.Random;

public class LightsVisualEffect implements EveryFrameWeaponEffectPlugin {
    private static float ACTIVATE_SPEED = 5.0f;
    private static float DEACTIVATE_SPEED = 1.0f;
    private static final Random RAND = new Random();

    private float alpha = 0.7f;
    private final float offset = RAND.nextFloat() * 1000;
	
    @Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(engine.isPaused()) return;
        
        ShipAPI ship = weapon.getShip();
        
		boolean on = (ship.getSystem() == null
                    || !ship.getSystem().isActive())
                && ship.isAlive()
                && !ship.getFluxTracker().isOverloaded()
                && (ship.getPhaseCloak() == null
                    || !(ship.getPhaseCloak().isActive()
                        || ship.getPhaseCloak().isCoolingDown()));

        if (alpha == 0 && !on) return;

        float wave = (float)Math.cos(engine.getTotalElapsedTime(false) * Math.PI + offset);
        wave *= (float)Math.cos(engine.getTotalElapsedTime(false) * Math.E / 3);
        alpha += engine.getElapsedInLastFrame() * (on ? ACTIVATE_SPEED : -DEACTIVATE_SPEED);
        alpha = Math.max(Math.min(alpha, 1), 0);
        
        weapon.getAnimation().setAlphaMult(alpha * (wave / 3 + 0.66f));
	}}

