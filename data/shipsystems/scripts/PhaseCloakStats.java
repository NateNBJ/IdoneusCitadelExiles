package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class PhaseCloakStats implements ShipSystemStatsScript {
    @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 150f * effectLevel);
			stats.getAcceleration().modifyMult(id, 0.2f * effectLevel);
			stats.getDeceleration().modifyMult(id, 0.2f * effectLevel);
		}
	}
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
			return new StatusData("increased max speed", false);
		} else if (index == 1) {
			return new StatusData("decreased acceleration", false);
		}
		return null;
	}
}
