package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class EvasionJetsStats implements ShipSystemStatsScript {
    boolean started = false;
    
    @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        
        stats.getMaxSpeed().modifyFlat(id, 1000f * effectLevel);
        stats.getAcceleration().modifyFlat(id, 5000f * (float)Math.pow(effectLevel, 2));
        stats.getDeceleration().modifyFlat(id, 5000f * (float)Math.pow(effectLevel, 2));
        
        ShipAPI ship = (ShipAPI)stats.getEntity();
        ship.setCollisionClass(CollisionClass.SHIP);

        //(ship.getNumFlameouts() == ship.getEngineController().getShipEngines().size());

        if(!started) {
            Global.getSoundPlayer().playSound("engine_accelerate", 1.6f, 4,
                    ship.getLocation(), ship.getVelocity());
            started = true;
        }
	}
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

        started = false;
	}
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}
