package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.tools.JauntSession;

public class JauntLightStats implements ShipSystemStatsScript {
    public static final float MAX_RANGE = 1500;
    
    JauntSession session;
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if(session == null) {
            session = JauntSession.getSession((ShipAPI)stats.getEntity(), MAX_RANGE);
        } else if(state == ShipSystemStatsScript.State.OUT) {
            session.goHome();
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        ShipSystemAPI cloak = ship.getPhaseCloak();
        
        if(session != null && (cloak == null || !cloak.isActive())) {
            session.goHome();
        }
        
        session = null;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}
