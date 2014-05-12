package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class FissionBladePushEffect implements BeamEffectPlugin {
    static final float FORCE_MULTIPLIER = 3.0f;
    IntervalTracker tracker = new IntervalTracker(0.03f);
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        CombatEntityAPI target = beam.getDamageTarget();
        ShipAPI ship = beam.getSource();
        float range = beam.getWeapon().getRange();

        if(target == null || !tracker.intervalElapsed())
            return;

        Vector2f from = beam.getFrom();
        float force = FORCE_MULTIPLIER
                * beam.getBrightness()
                * (ship.getCollisionRadius() / target.getCollisionRadius())
                * (range / (MathUtils.getDistance(beam.getFrom(), beam.getTo()) + (range * 0.2f)));
        Vector2f direction = VectorUtils.getDirectionalVector(from, beam.getTo());
        
        CombatUtils.applyForce(target, direction, force);
        direction.scale(-1);
        CombatUtils.applyForce(ship, direction, force);
    }	
}