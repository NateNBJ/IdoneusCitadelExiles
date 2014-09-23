package data.weapons.decorative;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.ICEModPlugin;
import data.tools.JauntSession;
import java.util.Map;
import java.util.WeakHashMap;

public class LightsVisualEffect implements EveryFrameWeaponEffectPlugin {

    private static final float ACTIVATE_SPEED = 5.0f;
    private static final float DEACTIVATE_SPEED = 1.0f;
    private static final float STATIC_ALPHA = 0.35f;
    private static final Map<ShipAPI, Float> offsets = new WeakHashMap();

    private float alpha = STATIC_ALPHA;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;

        ShipAPI ship = weapon.getShip();

        boolean on = (ship.getSystem() == null || !ship.getSystem().isActive())
                && ship.isAlive()
                && !ship.getFluxTracker().isOverloaded()
                && !JauntSession.hasSession(ship)
                && (ship.getPhaseCloak() == null
                    || !(ship.getPhaseCloak().isActive()
                    || ship.getPhaseCloak().isCoolingDown()));

        if (alpha == 0 && !on) return;
        
        weapon.getSprite().setAdditiveBlend();

        if(weapon.getShip().getOriginalOwner() == -1 || ICEModPlugin.SMILE_FOR_CAMERA) {
            String id = weapon.getId();
            weapon.getAnimation().setAlphaMult(STATIC_ALPHA);
        } else {
            if(!offsets.containsKey(ship)) {
                offsets.put(ship, (float)(Math.random() * 1000));
            }
                
            float wave = (float) Math.cos(engine.getTotalElapsedTime(false) * Math.PI + offsets.get(ship));
            wave *= (float) Math.cos(engine.getTotalElapsedTime(false) * Math.E / 3);
            alpha += amount * (on ? ACTIVATE_SPEED : -DEACTIVATE_SPEED);
            alpha = Math.max(Math.min(alpha, 1), 0);
            weapon.getAnimation().setAlphaMult(alpha * (wave / 3 + 0.66f));
        }
    }
}
