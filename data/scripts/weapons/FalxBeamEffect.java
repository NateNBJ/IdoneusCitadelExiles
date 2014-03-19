package data.scripts.weapons;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class FalxBeamEffect implements BeamEffectPlugin {
    static final float MAX_ARC_REDUCTION_PER_SECOND = 50.0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        if(!(beam.getDamageTarget() instanceof ShipAPI)) return;

        ShieldAPI shield = ((ShipAPI)beam.getDamageTarget()).getShield();

        if(shield == null || !shield.isWithinArc(beam.getTo())) return;

        float arc = shield.getActiveArc() - amount * beam.getBrightness()
                * (float)Math.pow(shield.getActiveArc() / shield.getArc(), 2)
                * MAX_ARC_REDUCTION_PER_SECOND;

        shield.setActiveArc(arc);
    }
}