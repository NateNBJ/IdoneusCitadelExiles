package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class PhaseWarpEngineStats implements ShipSystemStatsScript {
    @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
            
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id);
        } else if (state == ShipSystemStatsScript.State.IN && effectLevel < 0.9f) {
            stats.getEntity().setCollisionClass(CollisionClass.SHIP);
		} else {
            stats.getEntity().setCollisionClass(CollisionClass.NONE);

			stats.getMaxSpeed().modifyFlat(id, 500f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 2000f * effectLevel);
			stats.getDeceleration().modifyFlat(id, 1000f * effectLevel);
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
			return new StatusData("increased speed", false);
		}
		return null;
	}
}