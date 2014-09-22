
package data.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import static data.weapons.beam.RecallBeamEffect.CHARGE_TIME;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class RecallTracker {
    int priority = 0;
    ShipAPI ally;
    Vector2f destination, idealRecallLoc, recallLoc;
    float progress = -1;
    WeaponAPI teleporter; 

    public RecallTracker(ShipAPI ally, WeaponAPI teleporter) {
        this.ally = ally;
        this.teleporter = teleporter;

        ShipAPI ship = teleporter.getShip();

        CombatFleetManagerAPI.AssignmentInfo asgnmt = Global.getCombatEngine().getFleetManager(
                ally.getOwner()).getAssignmentFor(ally);

        if(asgnmt == null) return;
        
//        if(Global.getCombatEngine().getFleetManager(ally.getOwner()).) {
//            
//        } else
//        if(asgnmt.getType() == CombatAssignmentType.RETREAT) {
        if(ally.isRetreating()) {
            destination = getRetreatLocation();
        } else if(asgnmt.getTarget() == null) {
            return;
        } else {
            destination = asgnmt.getTarget().getLocation();
        }

        idealRecallLoc = VectorUtils.getDirectionalVector(ship.getLocation(), destination);
        idealRecallLoc.scale(Math.min(teleporter.getRange(), MathUtils.getDistance(ship, destination)));
        Vector2f.add(idealRecallLoc, ship.getLocation(), idealRecallLoc);

        float distFromCurrent = MathUtils.getDistance(ally, destination);
        float distFromRecalled = MathUtils.getDistance(idealRecallLoc, destination);

        priority = (int)((distFromCurrent - distFromRecalled - 2500) / 5000 * IceUtils.getFP(ally));
    }
    public void advance(float amount) {
        if(progress > 0 && recallLoc == null) {
            Vector2f midPoint = new Vector2f(
                    (idealRecallLoc.x + teleporter.getLocation().x) / 2,
                    (idealRecallLoc.y + teleporter.getLocation().y) / 2
            );

            for(float range = 100; range < teleporter.getRange() + 101; range += teleporter.getRange() / (priority + 4)) {
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
                ally.setAngularVelocity(0);
                ally.setFacing(VectorUtils.getAngle(teleporter.getLocation(), recallLoc));
                playTeleportSound();

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
                (ally.getLocation().x + teleporter.getShip().getLocation().x) / 2,
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

