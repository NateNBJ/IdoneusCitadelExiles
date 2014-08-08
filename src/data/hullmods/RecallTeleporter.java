package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.IntervalTracker;

public class RecallTeleporter extends BaseHullMod {
    IntervalTracker timer = new IntervalTracker(1, 2);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        
        
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(!timer.intervalElapsed()) return;
        
        
    }
}