package data.weapons.decorative;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.EveryFramePlugin;

public class SystemVisualEffect implements EveryFrameWeaponEffectPlugin {
    final static float ACTIVATION_SPEED = 1;
    final static float DEACTIVATION_SPEED = 1;

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
        
        weapon.getSprite().setAdditiveBlend();
        weapon.getAnimation().setFrame(1);
        
        alpha += EveryFramePlugin.getElapsed() * (on ? ACTIVATION_SPEED : -DEACTIVATION_SPEED);
        alpha = Math.max(Math.min(alpha, 1), 0);
        
        weapon.getAnimation().setAlphaMult(alpha);
	}}

