package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import data.ai.weapon.*;
import data.tools.IceUtils;

public class MobiusRayEffect implements EveryFrameWeaponEffectPlugin {
    static final float MAX_ROTATION_PER_SECOND = 270f;
    float timeOfFiring;
    int projectilesInPlay = 0;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(!weapon.isFiring() && projectilesInPlay == 0) {
            timeOfFiring = engine.getTotalElapsedTime(false);
            return;
        }

        ShipAPI ship = weapon.getShip();
        MobiusRayAutofireAIPlugin autofire = MobiusRayAutofireAIPlugin.get(weapon);
        Vector2f target = autofire.isOn()
                ? new Vector2f(autofire.getTarget())
                : new Vector2f(ship.getMouseTarget());
        DamagingProjectileAPI previous = null;
        float maxRotation = MAX_ROTATION_PER_SECOND * amount;
        projectilesInPlay = 0;

        if(ship.getShipAI() != null || autofire.isOn()) {
            // Select exact target ship location rather than default AI cursor
            if(ship.getShipTarget() != null && !autofire.isOn()) {
                target = new Vector2f(ship.getShipTarget().getLocation());
            }

            float radius = Math.min(
                    weapon.getRange() * 0.8f,
                    MathUtils.getDistance(weapon.getLocation(), target));
            float scale = Math.max(0, 0.7f - (engine.getTotalElapsedTime(false) - timeOfFiring));
            target.x += Math.sin(engine.getTotalElapsedTime(false) * Math.PI) * radius * scale;
            target.y += Math.sin(engine.getTotalElapsedTime(false) * Math.E) * radius * scale;

            //SunUtils.blink(target);
        }
        
        for(Iterator iter = engine.getProjectiles().iterator(); iter.hasNext();) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI)iter.next();

            if(proj.getWeapon() != weapon) continue;

            ++projectilesInPlay;

            if(previous != null) target = previous.getLocation();

            float d = VectorUtils.getAngle(proj.getLocation(), target);

            d = MathUtils.getShortestRotation(proj.getFacing(), d);
            d = Math.signum(d) * Math.min(maxRotation, Math.abs(d));
            proj.setFacing(MathUtils.clampAngle(proj.getFacing() + d));

            if(previous == null) maxRotation *= 3;

            previous = proj;
        }

        autofire.setIsOn(false);
    }
}