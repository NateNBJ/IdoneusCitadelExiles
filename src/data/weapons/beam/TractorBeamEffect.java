package data.weapons.beam;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.ai.ship.MeleeTempAI;
import data.tools.IntervalTracker;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class TractorBeamEffect implements BeamEffectPlugin {
    static final float FORCE_MULTIPLIER = 20.0f;
    //static final float FORCE_MULTIPLIER = 5.0f;
    IntervalTracker tracker = new IntervalTracker(0.03f);
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        CombatEntityAPI target = beam.getDamageTarget();
        ShipAPI ship = beam.getSource();

        if(target == null || !tracker.intervalElapsed())
            return;

        Vector2f from = beam.getFrom();
        float force = FORCE_MULTIPLIER * beam.getBrightness();
        Vector2f direction = VectorUtils.getDirectionalVector(from, beam.getTo());
        
        String id = beam.getSource().getHullSpec().getHullId();

//        if(id.contains("sun_ice_kelpie")) {
//            Vector2f.add(from, (Vector2f)new Vector2f(direction).scale(30), from);
//            float dist = Math.max(0, 30f - MathUtils.getDistance(beam.getFrom(), beam.getTo()));
//            if(dist > 0 ) {
//                Vector2f v = (Vector2f)target.getVelocity();
//                float speed = (float)Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.y, 2));
//                v.x /= speed;
//                v.y /= speed;
//                direction = v;
//                force = 0.001f * dist * target.getMass();
//            }
//        }

        if((id.contains("sun_ice_pentagram") || id.contains("sun_ice_kelpie"))
                && !ship.getFluxTracker().isOverloadedOrVenting()
                && !ship.getPhaseCloak().isActive()
                && ship.getShipAI() != null
                && ship.getFluxTracker().getFluxLevel() < 0.8f
                //&& !MeleeTempAI.isBeingUsedBy(ship)
                //&& !(ship.getShipAI() instanceof MeleeTempAI)
                && !ship.getShipAI().needsRefit() // MeleeTempAI returns true...
                && target instanceof ShipAPI
                && target.getOwner() != ship.getOwner()
                && ((ShipAPI)target).isAlive()) {
            
            ship.setShipAI(new MeleeTempAI(ship, beam.getWeapon()));
            ship.setShipTarget((ShipAPI)target);
        }
        
        CombatUtils.applyForce(ship, direction, force);
        direction.scale(-1);
        CombatUtils.applyForce(target, direction, force);
    }	
}