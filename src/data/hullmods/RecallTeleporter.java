package data.hullmods;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.tools.IntervalTracker;
import data.tools.RecallTracker;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import org.lazywizard.lazylib.combat.AIUtils;

public class RecallTeleporter extends BaseHullMod {
    static final float UPDATE_FREQUENCY = 0.3f;
    
    TreeMap<Integer, List<RecallTracker>> recallQueue;
    CombatEngineAPI engine;

    WeaponAPI weapon;
    ShipAPI ship;
    IntervalTracker startCheckTimer = new IntervalTracker(UPDATE_FREQUENCY);
    IntervalTracker doRecallTimer = new IntervalTracker(0.1f, 0.5f);
    
    void collectRecallRequests(WeaponAPI weapon) {
        recallQueue = new TreeMap();
        
        for(ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
            RecallTracker t = new RecallTracker(ally, weapon);
            if(t.getPriority() > 0) {
                if(!recallQueue.containsKey(t.getPriority())) {
                    recallQueue.put(t.getPriority(), new LinkedList());
                }
                
                recallQueue.get(t.getPriority()).add(t);
            }
        }
    }
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        
        this.ship = ship;
        //this.weapon = ship
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
    }
}