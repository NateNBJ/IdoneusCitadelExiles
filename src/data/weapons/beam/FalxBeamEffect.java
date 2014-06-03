package data.weapons.beam;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.SunUtils;

public class FalxBeamEffect implements BeamEffectPlugin {
    static final float MAX_ARC_REDUCTION_PER_SECOND = 80.0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        if(!(beam.getDamageTarget() instanceof ShipAPI)) return;

        ShipAPI target = ((ShipAPI)beam.getDamageTarget());
        ShieldAPI shield = target.getShield();

        if(shield == null || !shield.isWithinArc(beam.getTo())) return;

        float arc = shield.getActiveArc() - amount * beam.getBrightness()
                * (float)Math.pow(shield.getActiveArc() / shield.getArc(), 2)
                * MAX_ARC_REDUCTION_PER_SECOND;

        shield.setActiveArc(arc);
        
        // Refund shield upkeep in proportion to arc reducion
        
        //float upkeep = target.getMutableStats().getShieldUpkeepMult().getModifiedValue();
        //float upkeep = SunUtils.getShieldUpkeep(target);
        //target.getFluxTracker().decreaseFlux(amount * upkeep * (1 - arc / shield.getArc()));

        // TODO - Need Alex to implement a getShieldUpkeep method =[

        //SunUtils.print("" + upkeep);
    }
}