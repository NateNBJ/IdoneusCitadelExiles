package data.scripts.plugins;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class TractorBeamEffect implements BeamEffectPlugin {
    private IntervalUtil tracker = new IntervalUtil(0.03f, 0.06f);
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        tracker.advance(amount);
        
        CombatEntityAPI target = beam.getDamageTarget();
        ShipAPI ship = beam.getSource();

        if(target == null || !tracker.intervalElapsed())
            return;

        Vector2f from = beam.getFrom();
        float force = 50f * beam.getBrightness();
        Vector2f direction = MathUtils.getDirectionalVector(from, beam.getTo());
        

        if(beam.getSource().getHullSpec().getHullId().contains("sun_ice_kelpie")) {
            Vector2f.add(from, (Vector2f)new Vector2f(direction).scale(30), from);
            float dist = Math.max(0, 30f - MathUtils.getDistance(beam.getFrom(), beam.getTo()));
            if(dist > 0 ) {
                Vector2f v = (Vector2f)target.getVelocity();
                float speed = (float)Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.y, 2));
                v.x /= speed;
                v.y /= speed;
                direction = v;
                force = 0.001f * dist * target.getMass();

//                target.getVelocity().scale(1 - beam.getBrightness() * 0.3f);
//                return;
            }
        }
        
        CombatUtils.applyForce(ship, direction, force);
        direction.scale(-1);
        CombatUtils.applyForce(target, direction, force);
    }	
}