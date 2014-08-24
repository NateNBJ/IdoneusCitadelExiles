package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class PhaseShiftStats implements ShipSystemStatsScript {
//    Vector2f destination = null;
//    Vector2f origin = null;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
//        if (state == ShipSystemStatsScript.State.OUT) {
//            //stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
//            stats.getMaxTurnRate().unmodify();
//        } else {
//            stats.getMaxSpeed().modifyFlat(id, 2500f * effectLevel);
//            stats.getAcceleration().modifyFlat(id, 20000f * effectLevel);
//            stats.getDeceleration().modifyFlat(id, 500f * effectLevel);
            
            stats.getTurnAcceleration().modifyFlat(id, 1000 * effectLevel);
            stats.getMaxTurnRate().modifyFlat(id, 100 * effectLevel);
            
//        ShipAPI ship = (ShipAPI)stats.getEntity();
//
//        //ship.getSpriteAPI().setAlphaMult(1 - effectLevel);
//
//        if(destination == null) {
//            origin = new Vector2f(ship.getLocation());
//            destination = new Vector2f(ship.getMouseTarget());
//        }
//        
//        //ship.getLocation().set(SunUtils.getMidpoint(origin, destination, effectLevel));
//
//        if(effectLevel >= 1) {
//            //ship.setFacing(VectorUtils.getAngle(destination, ship.getMouseTarget()));
//            //ship.getLocation().set(destination);
//            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
//        }
//        //}
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        //destination = origin = null;
	stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
//        stats.getMaxSpeed().unmodify(id);
//        stats.getAcceleration().unmodify(id);
//        stats.getDeceleration().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
//        if (index == 0) {
//            return new StatusData("increased turn speed", false);
//        }
        return null;
    }
}
