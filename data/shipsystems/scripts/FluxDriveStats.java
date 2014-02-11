package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.awt.Color;
import java.util.Iterator;
import org.lwjgl.util.vector.Vector2f;

public class FluxDriveStats implements ShipSystemStatsScript {
    @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 150f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 200f * effectLevel);
			stats.getMaxTurnRate().modifyPercent(id, -80f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, -80f * effectLevel);
		}

//        ShipAPI ship = (ShipAPI)stats.getEntity();
//
//        for(Iterator iter = ship.getEngineController().getShipEngines().iterator(); iter.hasNext();) {
//            ShipEngineAPI e = (ShipEngineAPI)iter.next();
//            Global.getCombatEngine().addSmoothParticle(e.getLocation(), new Vector2f(0, 0), 35, 1, 2, Color.yellow);
//            //Global.getCombatEngine().spawnExplosion(e.getLocation(), new Vector2f(0, 0),Color.yellow, 30, 2);
//        }
	}
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased engine power", false);
		}
		return null;
	}
}
