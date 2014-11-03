
package data.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import data.hullmods.RecallTeleporter;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class RecallTracker {
    public static final float CHARGE_TIME = 2f;  
    public static final float MAX_RANGE = 1200f;
    public static boolean isRecalling(ShipAPI ship) {
        return RECALL_COUNT.containsKey(ship) && RECALL_COUNT.get(ship) > 0;
    }
    public static void clearStaticData() {
        PREVIOUS_DESTINATION.clear();
        ALREADY_TELEPORTING.clear();
        RECALL_COUNT.clear();
        RecallTeleporter.Starters.clear();
    }
    static Map<ShipAPI, Vector2f> PREVIOUS_DESTINATION = new HashMap();
    static Map<ShipAPI, Integer> RECALL_COUNT = new HashMap();
    static Set<ShipAPI> ALREADY_TELEPORTING = new HashSet();
    
    int priority = 0;
    ShipAPI ally, teleporter;
    Vector2f destination, idealRecallLoc, recallLoc;
    float progress = -1;

    public RecallTracker(ShipAPI ally, ShipAPI teleporter) {
        this.ally = ally;
        this.teleporter = teleporter;
        
        if(ALREADY_TELEPORTING.contains(ally)) return;

        CombatFleetManagerAPI.AssignmentInfo asgnmt = Global.getCombatEngine().getFleetManager(
                ally.getOwner()).getAssignmentFor(ally);

        if(asgnmt == null) return;
        
        if(ally.isRetreating()) {
            destination = getRetreatLocation();
        } else if(asgnmt.getTarget() == null) {
            return;
        } else {
            destination = asgnmt.getTarget().getLocation();
        }

        boolean destinationChanged = !PREVIOUS_DESTINATION.containsKey(ally)
                ||  MathUtils.getDistance(destination, PREVIOUS_DESTINATION.get(ally)) > 100;
        
        float minimumDistanceGain = destinationChanged ? 500 : 2500;
        
//        if(destinationChanged) {
//            IceUtils.print(ally, "Recalculating");
//        }
        
        PREVIOUS_DESTINATION.put(ally, destination);

        idealRecallLoc = VectorUtils.getDirectionalVector(teleporter.getLocation(), destination);
        idealRecallLoc.scale(Math.min(MAX_RANGE, MathUtils.getDistance(teleporter, destination)));
        Vector2f.add(idealRecallLoc, teleporter.getLocation(), idealRecallLoc);

        float distFromCurrent = MathUtils.getDistance(ally, destination);
        float distFromRecalled = MathUtils.getDistance(idealRecallLoc, destination);

        priority = (int)Math.ceil((distFromCurrent - distFromRecalled - minimumDistanceGain) / 5000 * IceUtils.getFP(ally));
        priority = Math.max(0, priority);
        
        
        //IceUtils.print(ally, ((destinationChanged) ? "Changed - " : "") + priority);
    }
    public void start() {
        ALREADY_TELEPORTING.add(ally);
        RECALL_COUNT.put(teleporter, RECALL_COUNT.containsKey(teleporter) ? RECALL_COUNT.get(teleporter) + 1 : 1);
    }
    public void end() {
        ALREADY_TELEPORTING.remove(ally);
        RECALL_COUNT.put(teleporter, RECALL_COUNT.get(teleporter) - 1);
        ally.getMutableStats().getMaxSpeed().unmodify("sun_ice_recall");
    }
    public void advance(float amount) {
        if(progress > 0 && recallLoc == null) {
            Vector2f midPoint = new Vector2f(
                    (idealRecallLoc.x + teleporter.getLocation().x) / 2,
                    (idealRecallLoc.y + teleporter.getLocation().y) / 2
            );

            for(float range = 100; range < MAX_RANGE + 101; range += MAX_RANGE / (priority + 4)) {
                Vector2f candidate = MathUtils.getRandomPointInCircle(midPoint, range);

                if(!isShipObstructingArea(candidate, ally.getCollisionRadius() + 75)
                        || ally.isFighter()) {
                    recallLoc = candidate;
                    break;
                }
            }

            if(recallLoc == null) recallLoc = ally.getLocation();
            else {
                playTeleportSound();
                ally.getLocation().set(recallLoc);
                ally.getVelocity().scale(0);
                ally.getMutableStats().getMaxSpeed().modifyMult("sun_ice_recall", progress);
                ally.setAngularVelocity(0);
                ally.setFacing(VectorUtils.getAngle(teleporter.getLocation(), recallLoc));

                if(JauntSession.hasSession(ally)) {
                    JauntSession.getSession(ally).endNow();
                }

                for(CombatEntityAPI roid : CombatUtils.getAsteroidsWithinRange(recallLoc, ally.getCollisionRadius())) {
                    Vector2f vel = new Vector2f(1, 0);

                    if(roid.getLocation() != ally.getLocation()) {
                        vel = VectorUtils.getDirectionalVector(recallLoc, roid.getLocation());
                    }

                    vel.scale(ally.getCollisionRadius() / CHARGE_TIME);
                    roid.getVelocity().set(vel);
                }
            }
        }


        progress += amount / CHARGE_TIME;
        ally.getSpriteAPI().setColor(new Color(1, 1, 1,
                (float)Math.min(1, Math.pow(Math.abs(progress), 2))));

        Vector2f jitterAnchor = (progress <= 0) ? ally.getLocation() : recallLoc;
        Vector2f jitterLoc = MathUtils.getRandomPointInCircle(jitterAnchor,
                (1 - Math.abs(progress)) * (ally.getCollisionRadius() / 30 + 5));
        ally.getLocation().set(jitterLoc);
        ally.setFacing((float)(ally.getFacing() + (0.5f - Math.random()) * 4 * Math.abs(progress)));
        ally.setCollisionClass(isComplete()
                ? (ally.isFighter() ? CollisionClass.FIGHTER : CollisionClass.SHIP)
                : CollisionClass.NONE);
    }
    public boolean isComplete() {
        return progress >= 1;
    }
    public int getPriority() {
        return priority;
    }
    public ShipAPI getAlly() {
        return ally;
    }
    
    Vector2f getRetreatLocation() {
        Vector2f retVal = new Vector2f(
                (ally.getLocation().x + teleporter.getLocation().x) / 2,
                Global.getCombatEngine().getMapHeight() * 2
        );

        // The only time the escape direction is down is when the player
        // fleet is retreating after it attacks
        int owner = ally.getOwner();
        FleetGoal goal = Global.getCombatEngine().getContext().getPlayerGoal();
        if(ally.getOwner() == 0 && (goal == FleetGoal.ATTACK || goal == null)) {
            retVal.y *= -1;
        }

        return retVal;
    }
    void playTeleportSound() {
        Global.getSoundPlayer().playSound("system_phase_skimmer", 1, 1,
                ally.getLocation(), ally.getVelocity());
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