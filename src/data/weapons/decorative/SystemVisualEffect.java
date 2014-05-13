package data.weapons.decorative;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.mission.FleetSide;

public class SystemVisualEffect implements EveryFrameWeaponEffectPlugin {
    private static float ACTIVATION_SPEED = 1;
    private static float DEACTIVATION_SPEED = 1;

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
        
        weapon.getAnimation().setAlphaMult(alpha);
	}}

