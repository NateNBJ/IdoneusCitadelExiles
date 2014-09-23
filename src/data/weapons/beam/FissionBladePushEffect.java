package data.weapons.beam;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import data.EveryFramePlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class FissionBladePushEffect implements BeamEffectPlugin {
    static final float FORCE_MULTIPLIER = 300.0f;
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        CombatEntityAPI target = beam.getDamageTarget();
        ShipAPI ship = beam.getSource();
        float range = beam.getWeapon().getRange();

        if(target == null) return;

        Vector2f from = beam.getFrom();
        float force = FORCE_MULTIPLIER
                * beam.getBrightness()
                * amount
                * (ship.getCollisionRadius() / target.getCollisionRadius())
                * (range / (MathUtils.getDistance(beam.getFrom(), beam.getTo()) + (range * 0.2f)));
        Vector2f direction = VectorUtils.getDirectionalVector(from, beam.getTo());
        
        CombatUtils.applyForce(target, direction, force);
        direction.scale(-1);
        CombatUtils.applyForce(ship, direction, force);
        
//        // Brace ourselves if we're about to run into our target 
//        if(ship.getShipAI() != null
//                && !ship.getSystem().isActive()
//                && !ship.getSystem().isCoolingDown()) {
//            
////            float collisionSpeed = Vector2f.sub(ship.getVelocity(), target.getVelocity(), new Vector2f()).length();
////            float massRatio = target.getMass() / Math.max(1, ship.getMass());
////            
////            if(collisionSpeed * massRatio > 300) {
//                EveryFramePlugin.delayCommand(ship, ShipCommand.USE_SYSTEM);
////            }
//        }
    }	
}