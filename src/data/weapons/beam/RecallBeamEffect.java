package data.weapons.beam;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.EveryFramePlugin;
import data.tools.IntervalTracker;
import data.tools.JauntSession;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class RecallBeamEffect implements BeamEffectPlugin {
    public class RecallTracker {
        int priority = 0;
        ShipAPI ally;
        Vector2f destination, idealRecallLoc, recallLoc;
        float progress = -1;
        WeaponAPI teleporter;
        
        public RecallTracker(ShipAPI ally, WeaponAPI teleporter) {
            this.ally = ally;
            this.teleporter = teleporter;
            
            AssignmentInfo asgnmt = Global.getCombatEngine().getFleetManager(
                    ally.getOwner()).getAssignmentFor(ally);

            if(asgnmt == null || asgnmt.getTarget() == null) return;

            destination = asgnmt.getTarget().getLocation();
            idealRecallLoc = VectorUtils.getDirectionalVector(ship.getLocation(), destination);
            idealRecallLoc.scale(Math.min(MAX_RANGE, MathUtils.getDistance(ship, destination)));
            Vector2f.add(idealRecallLoc, ship.getLocation(), idealRecallLoc);

            float distFromCurrent = MathUtils.getDistance(ally, destination);
            float distFromRecalled = MathUtils.getDistance(idealRecallLoc, destination);
            
            priority = (int)((distFromCurrent - distFromRecalled - 1000) / 500);
        }
        public void advance(float amount) {
            if(progress > 0 && recallLoc == null) {
                Vector2f midPoint = new Vector2f(
                        (idealRecallLoc.x + teleporter.getLocation().x) / 2,
                        (idealRecallLoc.y + teleporter.getLocation().y) / 2
                );
                
                float maxRange = 2000;
                
                for(float range = 100; range < maxRange + 101; range += maxRange / (priority + 4)) {
                    Vector2f candidate = MathUtils.getRandomPointInCircle(midPoint, range);
                    
                    if(!isShipObstructingArea(candidate, ally.getCollisionRadius())
                            || ally.isFighter()) {
                        recallLoc = candidate;
                        break;
                    }
                }
                
                if(recallLoc == null) recallLoc = ally.getLocation();
                else {
                    ally.getLocation().set(recallLoc);
                    ally.getVelocity().scale(0);
                    ally.setAngularVelocity(0);
                    
                    if(JauntSession.hasSession(ally)) {
                        JauntSession.getSession(ally).endNow();
                    }
                    
                    // push away asteroids
                }
            }
            
            
            progress += amount / 3;
            ally.getSpriteAPI().setColor(new Color(1,1,1, Math.min(1, Math.abs(progress))));
            
            Vector2f jitterAnchor = (progress <= 0) ? ally.getLocation() : recallLoc;
            Vector2f jitterLoc = MathUtils.getRandomPointInCircle(jitterAnchor,
                    (1 - Math.abs(progress)) * (ally.getCollisionRadius() / 15 + 5));
            ally.getLocation().set(jitterLoc);
            ally.setFacing((float)(ally.getFacing() + (0.5f - Math.random()) * 4 * Math.abs(progress)));
            ally.setCollisionClass(isComplete()
                    ? (ally.isFighter() ? CollisionClass.FIGHTER : CollisionClass.SHIP)
                    : CollisionClass.NONE);
        }
        public boolean isComplete() {
            return progress >= 1;
        }
        
        boolean isShipObstructingArea(Vector2f at, float range) {
        for(ShipAPI s : CombatUtils.getShipsWithinRange(at, range)) {
            if(!s.isFighter()) {
                return true;
            }
        }
        
        return false;
    }
    
    }
    
    static final float MAX_RANGE = 1000f;  
    
    ShipAPI ship;
    TreeMap<Integer, List<RecallTracker>> recallQueue;
    IntervalTracker timer = new IntervalTracker(0.1f, 0.2f);

    void collectRecallRequests(WeaponAPI weapon) {
        recallQueue = new TreeMap();
        
        for(ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
            RecallTracker t = new RecallTracker(ally, weapon);
            if(t.priority > 0) {
                if(!recallQueue.containsKey(t.priority)) {
                    recallQueue.put(t.priority, new LinkedList());
                }
                
                recallQueue.get(t.priority).add(t);
            }
        }
    }
    void selectAnAllyToRecall() {
        if(recallQueue.isEmpty()) return;
        
        List<RecallTracker> candidates = recallQueue.firstEntry().getValue();
        int index = (new Random()).nextInt(candidates.size());
        RecallTracker winner = candidates.get(index);
        
        EveryFramePlugin.beginRecall(winner);
        
        candidates.remove(winner);
        if(candidates.isEmpty()) {
            recallQueue.remove(recallQueue.firstKey());
        }
        
        
//        for(Integer key : recallQueue.descendingKeySet()) {
//            for(RecallTracker t : recallQueue.get(key)) {
//                //t.recallLoc = t.idealRecallLoc;
//                EveryFramePlugin.beginRecall(t);
//            }
//        }
    }
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        ship = beam.getSource();
        
        if(recallQueue == null) collectRecallRequests(beam.getWeapon());
        
        if(timer.intervalElapsed()) selectAnAllyToRecall();
    }
}
