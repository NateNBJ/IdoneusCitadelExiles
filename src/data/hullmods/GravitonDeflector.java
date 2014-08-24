package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.tools.SunUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class GravitonDeflector extends BaseHullMod {
    static final float FORCE_MULTIPLIER = 300f;
    static final float MAX_ANGLE_DIFFERENCE = 20f;
    static final float DAMAGE_WINDOW_SECONDS = 2f;

    int framesOfPhase = 0;
    LinkedList<DamagingProjectileAPI> undiflectedOrdnance;

    boolean shouldDenyPhase(ShipAPI ship) {
        float accumulator = 0f;

        accumulator += SunUtils.estimateIncomingBeamDamage(ship, DAMAGE_WINDOW_SECONDS);

        for (DamagingProjectileAPI proj : undiflectedOrdnance) {

            if(proj.getOwner() == ship.getOwner()) continue;

            Vector2f endPoint = new Vector2f(proj.getVelocity());
            endPoint.scale(DAMAGE_WINDOW_SECONDS);
            Vector2f.add(endPoint, proj.getLocation(), endPoint);

            if((ship.getShield() != null && ship.getShield().isWithinArc(proj.getLocation()))
                    || !CollisionUtils.getCollides(proj.getLocation(), endPoint,
                        new Vector2f(ship.getLocation()), ship.getCollisionRadius()))
                continue;

            accumulator += proj.getDamageAmount();
        }
        
        SunUtils.print(ship, "" + accumulator);
        
        return accumulator < 100;
    }
    
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(ship.getPhaseCloak() == null || !ship.getPhaseCloak().isOn()) {
            framesOfPhase = 0;
        } else ++framesOfPhase;
        
        if(!ship.isAlive()
                //|| (ship.getPhaseCloak() != null && ship.getPhaseCloak().isOn())
                || framesOfPhase > 1
                || (ship.getSystem() != null && ship.getSystem().isOn())
                || ship.getFluxTracker().isOverloadedOrVenting()
                ) return;
        
        boolean checkingPhase = framesOfPhase == 1 && ship.getShipAI() != null;

        
        Collection<DamagingProjectileAPI> projectiles =
                Global.getCombatEngine().getProjectiles();
                //CombatUtils.getProjectilesWithinRange(ship.getLocation(), ship.getCollisionRadius() * 5);
        
        if(checkingPhase) undiflectedOrdnance = new LinkedList(projectiles);

        //for(DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
        for(DamagingProjectileAPI proj : projectiles) {
            // Make sure the projectile is moving in the opposite direction of the ship's facing
            float angleDif = MathUtils.getShortestRotation(ship.getFacing(),
                    MathUtils.clampAngle(proj.getFacing() + 180));
            if(Math.abs(angleDif) >= MAX_ANGLE_DIFFERENCE) continue;

            // Make sure the projectile is in front of the ship
            angleDif = MathUtils.getShortestRotation(ship.getFacing(),
                    VectorUtils.getAngle(ship.getLocation(), proj.getLocation()));
            if(Math.abs(angleDif) >= MAX_ANGLE_DIFFERENCE) continue;
            
            // Deflected projectiles will not be checked during phase approval
            if(checkingPhase) undiflectedOrdnance.remove(proj);

            // Calculate the angle by which to rotate the projectile
            float distance = MathUtils.getDistance(ship.getLocation(), proj.getLocation());
            float force = (float)Math.pow(1 - Math.abs(angleDif) / MAX_ANGLE_DIFFERENCE, 2)
                    * (ship.getCollisionRadius() / distance) * FORCE_MULTIPLIER;
            float dAngle = -Math.signum(angleDif) * force * amount;

            // Rotate the facing and velocity of the projectile
            VectorUtils.rotate(proj.getVelocity(), dAngle, proj.getVelocity());
            proj.setFacing(MathUtils.clampAngle(proj.getFacing() + dAngle));
        }
                
        if(checkingPhase && shouldDenyPhase(ship)) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

}