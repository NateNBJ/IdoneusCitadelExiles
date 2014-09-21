package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class SensorBoostStats implements ShipSystemStatsScript
{
    private static float RANGE_BOOST = 0.6f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getSightRadiusMod().modifyMult(id, 1 + RANGE_BOOST * effectLevel);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getSightRadiusMod().unmodify();
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("increased sensor range", false);
        }
        
        return null;
    }
}