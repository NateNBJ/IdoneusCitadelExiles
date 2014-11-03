package data.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.EveryFramePlugin;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.lazywizard.lazylib.combat.AIUtils;

public class RecallStarter {
    TreeMap<Integer, List<RecallTracker>> recallQueue = new TreeMap();
    CombatEngineAPI engine;

    ShipAPI ship;
    IntervalTracker requestCheckTimer = new IntervalTracker(0.3f);
    IntervalTracker doRecallTimer = new IntervalTracker(0.1f, 0.5f);
    
    public RecallStarter(ShipAPI ship) {
        this.ship = ship;
        this.engine = Global.getCombatEngine();
    }
    
    void collectRecallRequests() {
        recallQueue = new TreeMap();
        
        for(ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
            RecallTracker t = new RecallTracker(ally, ship);
            if(t.getPriority() > 0) {
                if(!recallQueue.containsKey(t.getPriority())) {
                    recallQueue.put(t.getPriority(), new LinkedList());
                }
                
                recallQueue.get(t.getPriority()).add(t);
            }
        }
    }
    void selectAnAllyToRecall() {
        if(recallQueue.isEmpty()) return;
        
        List<RecallTracker> candidates = recallQueue.lastEntry().getValue();
        int index = (new Random()).nextInt(candidates.size());
        RecallTracker winner = candidates.get(index);
        
        EveryFramePlugin.beginRecall(winner);
        Global.getSoundPlayer().playSound("system_phase_cloak_activate", 2, 1,
                winner.getAlly().getLocation(), winner.getAlly().getVelocity());
        
        candidates.remove(winner);
        if(candidates.isEmpty()) {
            recallQueue.remove(recallQueue.lastKey());
        }
    }
    public void advance() {
        if(!ship.isAlive()) return;
        if(requestCheckTimer.intervalElapsed()) collectRecallRequests();
        if(doRecallTimer.intervalElapsed()) selectAnAllyToRecall();
    }
}
