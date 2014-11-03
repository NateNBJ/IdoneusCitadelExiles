package data.weapons.decorative;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.EveryFramePlugin;
import data.tools.RecallTracker;
import org.lwjgl.util.vector.Vector2f;

public class RecallVisualEffect implements EveryFrameWeaponEffectPlugin  {
    final static float ACTIVATION_SPEED = 1;
    final static float DEACTIVATION_SPEED = 1;

    float alpha = 0;
    Vector2f center;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        
        if(center == null) center = new Vector2f(weapon.getSprite().getCenterX(),
                weapon.getSprite().getCenterY());
            
        boolean on = weapon.getShip().isAlive() && RecallTracker.isRecalling(weapon.getShip());

        if (alpha == 0 && !on) {
            weapon.getAnimation().setFrame(0);
            return;
        }
        
        weapon.getSprite().setCenterX(((float)Math.random() - 0.5f) * 7 + weapon.getSprite().getWidth() / 2);
        weapon.getSprite().setCenterY(((float)Math.random() - 0.5f) * 7 + weapon.getSprite().getHeight() / 2);

        weapon.getSprite().setAdditiveBlend();
        weapon.getAnimation().setFrame(1);

        alpha += EveryFramePlugin.getElapsed() * (on ? ACTIVATION_SPEED : -DEACTIVATION_SPEED);
        alpha = Math.max(Math.min(alpha, 1), 0);

        weapon.getAnimation().setAlphaMult(alpha);
        
        Global.getSoundPlayer().playLoop("high_intensity_laser_loop", weapon.getShip(),
                1.5f, alpha,  weapon.getShip().getLocation(), weapon.getShip().getVelocity());
    }
    
}
