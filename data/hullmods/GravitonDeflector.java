package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.IntervalTracker;
import java.util.Iterator;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class GravitonDeflector extends BaseHullMod {
    static final float FORCE_MULTIPLIER = 0.5f;
    static final float MAX_ANGLE_DIFFERENCE = 20f;
    static final IntervalTracker tracker = new IntervalTracker(5f / 60f);

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(!ship.isAlive()// || !tracker.intervalElapsed()
                || (ship.getPhaseCloak() != null && ship.getPhaseCloak().isOn())
                || (ship.getSystem() != null && ship.getSystem().isOn())
                || ship.getFluxTracker().isOverloadedOrVenting()
                ) return;

        //for(Iterator iter = CombatUtils.getProjectilesWithinRange(ship.getLocation(), ship.getCollisionRadius() * 2).iterator(); iter.hasNext();) {
        for(Iterator iter = Global.getCombatEngine().getProjectiles().iterator(); iter.hasNext();) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI)iter.next();
            float angleDif = MathUtils.getShortestRotation(ship.getFacing(), MathUtils.clampAngle(proj.getFacing() + 180));

            if(Math.abs(angleDif) >= MAX_ANGLE_DIFFERENCE) continue;

            angleDif = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), proj.getLocation()));

            if(Math.abs(angleDif) >= MAX_ANGLE_DIFFERENCE) continue;

            float distance = MathUtils.getDistance(ship.getLocation(), proj.getLocation());
            float force = (float)Math.pow(1 - Math.abs(angleDif) / MAX_ANGLE_DIFFERENCE, 2)
                    * (ship.getCollisionRadius() / distance) * FORCE_MULTIPLIER;
            float dAngle = -angleDif * tracker.getMinimumInterval() * force
                    * (0.3f + (1 - ship.getFluxTracker().getFluxLevel()) * 0.7f)
                    ;


            VectorUtils.rotate(proj.getVelocity(), dAngle, proj.getVelocity());
            proj.setFacing(MathUtils.clampAngle(proj.getFacing() + dAngle* (float)(180 / Math.PI)));
        }
    }

    @Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

}