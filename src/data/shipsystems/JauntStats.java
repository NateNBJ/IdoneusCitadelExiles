package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.shipsystems.ai.JauntAI;
import data.tools.SunUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class JauntStats implements ShipSystemStatsScript {
    public static final float MAX_RANGE = 800;
    static final float REPELL_FORCE = 0.5f;
    static final float SUPPORT_RANGE = 2000f;
    static final int DESTINATION_CANDIDATE_COUNT = 11;
    
    ShipAPI ship = null;
    Vector2f destination = null;
    Vector2f origin = null;
    DamagingProjectileAPI doppelganger = null;
    boolean teleported = false;
    ShipSystemStatsScript.State previousState = ShipSystemStatsScript.State.OUT;
    
    boolean pointIsClear(Vector2f at) {
        for(ShipAPI s : Global.getCombatEngine().getShips()) {
            if(s != ship && s.getCollisionClass() == CollisionClass.SHIP
                    && MathUtils.getDistance(s, at) <= 0)
                return false;
        }

        return CombatUtils.getAsteroidsWithinRange(at, ship.getCollisionRadius() * 0.8f).isEmpty();
    }
    Vector2f chooseDestination() {
        Vector2f retVal = null, shipLoc = new Vector2f(ship.getLocation());
        boolean aggressing = ship.getFluxTracker().getFluxLevel() < 0.4 + Math.random() * 0.3;
        float range = MAX_RANGE + ship.getCollisionRadius();
        float weaponRange = SunUtils.estimateOptimalRange(ship);
        float bestScore = -999999; // Float.MIN_VALUE won't work for some reason. Why?
        double theta = Math.random() * Math.PI * 2;
        double thetaIncrement = (Math.PI * 2) / DESTINATION_CANDIDATE_COUNT;

        for (int i = 0; i <= DESTINATION_CANDIDATE_COUNT; ++i) {
            Vector2f candidate = new Vector2f(
                (float)Math.cos(theta) * range + shipLoc.x,
                (float)Math.sin(theta) * range + shipLoc.y);
            theta += thetaIncrement;
            
            ship.getLocation().set(candidate);
            float score = SunUtils.getFPWorthOfSupport(ship, SUPPORT_RANGE)
            //        - Math.max(0, SunUtils.getFPWorthOfHostility(ship, SUPPORT_RANGE) - SunUtils.getFP(ship) * ship.getFluxTracker().getFluxLevel())
                    - SunUtils.estimateIncomingDamage(ship);
            
            if(!pointIsClear(candidate)) score -= ship.getCollisionRadius() / 10;
            
            if(aggressing) {
                ShipAPI enemy = AIUtils.getNearestEnemy(ship);
                if(MathUtils.getDistance(ship, enemy) < weaponRange)
                    score += SunUtils.getFP(enemy) * (0.5f + enemy.getFluxTracker().getFluxLevel());
            } else score -= SunUtils.getFPWorthOfHostility(ship, SUPPORT_RANGE);
            
            if(score > bestScore) {
                bestScore = score;
                retVal = candidate;
            }
        }
        
        ship.getLocation().set(shipLoc);
        
        if(retVal == null) {
            retVal = MathUtils.getRandomPointInCircle(ship.getLocation(), range);
        }
        
        return retVal;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ship = (ShipAPI)stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        float amount = engine.getElapsedInLastFrame();
        float alpha = (float)Math.min(1, Math.max(0, Math.pow(Math.abs((0.5f - effectLevel) * 2), 3)));
        ship.getSpriteAPI().setColor(new Color(1, 1, 1, alpha));
        
        if(state == ShipSystemStatsScript.State.ACTIVE) {
            ship.setCollisionClass(CollisionClass.SHIP);
            stats.getMaxSpeed().unmodify(id);
            stats.getTurnAcceleration().modifyFlat(id, 50);
            stats.getMaxTurnRate().modifyFlat(id, 25);
            
            destination = new Vector2f(ship.getLocation());
            
            // Make sure the ship's rotation slows down to a reasonable rate
            float turnRate = ship.getAngularVelocity();
            float turnRateLimit = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
            if(Math.abs(turnRate) > turnRateLimit) {
                ship.setAngularVelocity(turnRate * (1 - amount * 2f));
            }
        } else {
            ship.setCollisionClass(CollisionClass.NONE);
            stats.getMaxSpeed().modifyFlat(id, -stats.getMaxSpeed().getModifiedValue());
            
            // Apply during-warp bonuses
            if(ship.getFluxTracker().isOverloadedOrVenting()) {
                stats.getMaxTurnRate().unmodify(id);
                stats.getTurnAcceleration().unmodify(id);
            } else {
                stats.getTurnAcceleration().modifyFlat(id, 300);
                stats.getMaxTurnRate().modifyFlat(id, 150);
            }
        
            // Executed only during the first frame of use
            if(destination == null) {
                // Determine teleport destination
                destination = (ship.getShipAI() == null)
                        ? new Vector2f(ship.getMouseTarget()) // For the player
                        : chooseDestination(); // For AI
                origin = new Vector2f(ship.getLocation());
                JauntAI.setOrigin(ship, origin);
                
                // Bring destination closer if it exceeds max range 
                float range = MAX_RANGE + ship.getCollisionRadius();
                float distance = MathUtils.getDistance(ship, destination);
                Vector2f dir = VectorUtils.getDirectionalVector(ship.getLocation(), destination);
                if(distance > range) {
                    distance = range - 100 - ship.getCollisionRadius();
                    destination = (Vector2f)new Vector2f(dir).scale(range);
                    Vector2f.add(destination, origin, destination);
                }
                
                // Bring destination progressively closer until it is clear of obsticles
                for(float length = distance + 100 + ship.getCollisionRadius();
                        !pointIsClear(destination); length -= 100) {
                    if(length <= 0) {
                        destination = ship.getLocation();
                        break;
                    }
                    destination = (Vector2f)new Vector2f(dir).scale(length);
                    Vector2f.add(destination, origin, destination);
                }
                
                // Create the doppelganger (placeholder double) at the origin
                doppelganger = (DamagingProjectileAPI)engine.spawnProjectile(
                        ship, null, "sun_ice_doppelganger", new Vector2f(origin.x + 1000, origin.y + 1000),
                        ship.getFacing(), new Vector2f());
            }
            
            // Move the ship toward the destination or back toward the origin
            float sign = Math.signum(effectLevel - 0.5f);
            float scale = (float)Math.pow(Math.abs(effectLevel - 0.5) * 2, 0.7f);
            ship.getLocation().set(SunUtils.getMidpoint(origin, destination, (sign * scale + 1) / 2));
        }
        
        // Manage the ship's doppelganger
        doppelganger.setFacing(ship.getFacing() + (float)(Math.random() - 0.5) * 1);
        doppelganger.getVelocity().set(0, 0);
        Vector2f at = SunUtils.getDirectionalVector(doppelganger.getFacing() + 180);
        at.scale(doppelganger.getCollisionRadius());
        Vector2f.add(at, origin, at);
        at.x += (float)(Math.random() - 0.5) * 5;
        at.y += (float)(Math.random() - 0.5) * 5;
        doppelganger.getLocation().set(at);
        
        // TODO - Make this move them directly instead of applying force
        
        // Push entities away from the doppelganger to keep that space available
        List<CombatEntityAPI> entities = new ArrayList();
        entities.addAll(engine.getShips());
        entities.addAll(engine.getAsteroids());
        //entities.remove(ship);
        for(CombatEntityAPI entity : entities) {
            if(entity instanceof ShipAPI && ((ShipAPI)entity).isFighter())
                continue;
            float distance = MathUtils.getDistance(entity, origin);
            float force = Math.min(1, 2 - distance / ship.getCollisionRadius());

            if(force > 0) {
                force *= amount * entity.getMass() * REPELL_FORCE;
                Vector2f direction = VectorUtils.getDirectionalVector(
                        origin, entity.getLocation());
                CombatUtils.applyForce(entity, direction, force);
            }
        }
        
        previousState = state;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ship = (ShipAPI)stats.getEntity();
        ship.setCollisionClass(CollisionClass.SHIP);
        
        destination = origin = null;
        previousState = ShipSystemStatsScript.State.OUT;
        
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        
        Global.getCombatEngine().removeEntity(doppelganger);
        doppelganger = null;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("increased rotation speed", false);
        }
        return null;
    }
}
