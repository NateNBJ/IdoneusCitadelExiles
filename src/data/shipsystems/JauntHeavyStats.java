package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.EveryFramePlugin;
import data.tools.JauntSession;

public class JauntHeavyStats implements ShipSystemStatsScript {
    public static final float MAX_RANGE = 800;
    
    JauntSession session;
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        float amount = EveryFramePlugin.getElapsed();
        
        if(session == null) {
            session = JauntSession.getSession((ShipAPI)stats.getEntity(), MAX_RANGE);
        }
        
        if(state == ShipSystemStatsScript.State.ACTIVE) {
            stats.getTurnAcceleration().modifyFlat(id, 50);
            stats.getMaxTurnRate().modifyFlat(id, 25);
            
            // Make sure the ship's rotation slows down to a reasonable rate
            float turnRate = ship.getAngularVelocity();
            float turnRateLimit = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
            if(Math.abs(turnRate) > turnRateLimit) {
                ship.setAngularVelocity(turnRate * (1 - amount * 2f));
            }
        } else {
            // Apply during-warp bonuses
            if(ship.getFluxTracker().isOverloadedOrVenting()) {
                stats.getMaxTurnRate().unmodify(id);
                stats.getTurnAcceleration().unmodify(id);
            } else {
                stats.getTurnAcceleration().modifyFlat(id, 300);
                stats.getMaxTurnRate().modifyFlat(id, 150);
            }
            
            if(state == ShipSystemStatsScript.State.OUT) session.goHome();
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        
        if(session != null) {
            session.goHome();
            session = null;
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("increased rotation speed", false);
        }
        return null;
    }
}
