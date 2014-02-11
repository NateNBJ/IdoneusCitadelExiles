package data.scripts.plugins;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class FissionBeamEffect implements EveryFrameWeaponEffectPlugin {
    static final float MAX_ROTATION_PER_SECOND = 360f;
    float timeOfFiring;
    int projectilesInPlay = 0;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(!weapon.isFiring() && projectilesInPlay == 0) {
            timeOfFiring = engine.getTotalElapsedTime(false);
            return;
        }

        ShipAPI ship = weapon.getShip();
        Vector2f target = new Vector2f(ship.getMouseTarget());
        DamagingProjectileAPI previous = null;
        float maxRotation = MAX_ROTATION_PER_SECOND * amount;
        projectilesInPlay = 0;

        if(ship.getShipAI() != null) {
            if(ship.getShipTarget() != null) target = new Vector2f(ship.getShipTarget().getLocation());
            float radius = (ship.getShipTarget() != null)
                    ? ship.getShipTarget().getCollisionRadius() * 4
                    : 600;
            float scale = Math.max(0, 1f - (engine.getTotalElapsedTime(false) - timeOfFiring));
            target.x += Math.sin(engine.getTotalElapsedTime(false) * Math.PI) * radius * scale;
            target.y += Math.sin(engine.getTotalElapsedTime(false) * Math.E) * radius * scale;

            //engine.addSmoothParticle(target, new Vector2f(), 70, 1, 0.1f, Color.MAGENTA);
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

            if(previous == null) maxRotation *= 4;

            previous = proj;
        }

    }
}