package data.hullmods;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.tools.IceUtils;
import java.util.List;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

public class GravitonDeflector extends BaseHullMod {
    static final float FORCE_MULTIPLIER = 300f;
    static final float MAX_ANGLE_DIFFERENCE = 20f;
    static final float DAMAGE_WINDOW_SECONDS = 2f;
    static final float CLOAK_DAMAGE_THRESHHOLD = 150f;

    int framesOfPhase = 0;
    List<DamagingProjectileAPI> undiflectedOrdnance;

    boolean shouldDenyPhase(ShipAPI ship) {
        float accumulator = 0f;

        accumulator += IceUtils.estimateIncomingBeamDamage(ship, DAMAGE_WINDOW_SECONDS);

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
        
        //SunUtils.print(ship, "" + accumulator);
        
        return accumulator < CLOAK_DAMAGE_THRESHHOLD;
    }
    
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(ship.getPhaseCloak() == null || !ship.getPhaseCloak().isOn()) {
            framesOfPhase = 0;
        } else ++framesOfPhase;
        
        if(!ship.isAlive()
                || framesOfPhase > 1
                || (ship.getSystem() != null && ship.getSystem().isOn())
                || ship.getFluxTracker().isOverloadedOrVenting()
                ) return;
        
        boolean checkingPhase = framesOfPhase == 1 && ship.getShipAI() != null;
        
        undiflectedOrdnance = IceUtils.curveBullets(ship.getLocation(),
                ship.getFacing(), MAX_ANGLE_DIFFERENCE, FORCE_MULTIPLIER);
                
        if(checkingPhase && shouldDenyPhase(ship)) {
            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

}