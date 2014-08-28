package data.weapons.beam;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.EveryFramePlugin;

public class NovaBeamEffect implements BeamEffectPlugin {
    static final float MAX_ARC_REDUCTION_PER_SECOND = 150.0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        beam.getSource().getShield().setActiveArc(beam.getSource().getShield().getActiveArc() * (1 - beam.getBrightness()));
            
        EveryFramePlugin.tagForShieldUpkeepRefund(beam.getSource());
        
        if(!(beam.getDamageTarget() instanceof ShipAPI)) return;

        ShipAPI target = ((ShipAPI)beam.getDamageTarget());
        ShieldAPI shield = target.getShield();

        if(shield == null) return;

        float arc = Math.max(-30, shield.getActiveArc() - amount * beam.getBrightness()
                * MAX_ARC_REDUCTION_PER_SECOND);

        shield.setActiveArc(arc);
        
        EveryFramePlugin.tagForShieldUpkeepRefund(target);
    }
}