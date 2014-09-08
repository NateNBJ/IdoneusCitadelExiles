package data.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.ai.ship.JauntTurnTempAI;
import data.shipsystems.ai.JauntAI;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class JauntSession {
    static final float REPELL_FORCE = 150f;
    static final float SUPPORT_RANGE = 2000f;
    static final float SECONDS_IN_WARP_PER_SU = 0.001f;
    static final float MIN_WARP_TIME = 0.5f;
    static final int DESTINATION_CANDIDATE_COUNT = 17;
    static final String ID = "sun_ice_jaunt_prevent_vent_exploit";
    static Map<ShipAPI, JauntSession> jauntSessions = new WeakHashMap();
    static List<JauntSession> toClear = new LinkedList();
    
    public static void advanceAll() {
        for(JauntSession jaunt : jauntSessions.values()) jaunt.advance();
        for(JauntSession jaunt : toClear) jaunt.endNow();
        toClear.clear();
    }
    public static boolean hasSession(ShipAPI ship) {
        return jauntSessions.containsKey(ship);
    }
    public static JauntSession getSession(ShipAPI ship) {
        return jauntSessions.containsKey(ship)
                ? jauntSessions.get(ship)
                : getSession(ship, 0);
    }
    public static JauntSession getSession(ShipAPI ship, float range) {
        return jauntSessions.containsKey(ship)
                ? jauntSessions.get(ship)
                : new JauntSession(ship, range);
    }
    
    ShipAPI ship = null;
    CombatEngineAPI engine;
    Vector2f destination = null;
    Vector2f origin = null;
    Vector2f lastLoc = null;
    DamagingProjectileAPI doppelganger = null;
    float progress, maxRange, amount, alpha, warpTime;
    boolean returning = false;
    
    void setAlpha(float alpha) {
        int a = Math.min(255, Math.max(0, (int)(alpha * 255f)));
        Color c = ship.getSpriteAPI().getColor();
        c = new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
        ship.getSpriteAPI().setColor(c);
        for(WeaponAPI w : ship.getAllWeapons()) w.getSprite().setColor(c);
    }
    boolean pointIsClear(Vector2f at) {
        for(ShipAPI s : Global.getCombatEngine().getShips()) {
            if(s != ship && s.getCollisionClass() == CollisionClass.SHIP
                    && MathUtils.getDistance(s, at) <= 0)
                return false;
        }

        return CombatUtils.getAsteroidsWithinRange(at, ship.getCollisionRadius() * 0.8f).isEmpty();
    }
    Vector2f getChosenAIDestination() {
        Vector2f retVal = null, shipLoc = new Vector2f(ship.getLocation());
        boolean aggressing = AIUtils.getEnemiesOnMap(ship).size() > 0
                && ship.getFluxTracker().getFluxLevel() < 0.4 + Math.random() * 0.3;
        float range = maxRange + ship.getCollisionRadius();
        float weaponRange = SunUtils.estimateOptimalRange(ship);
        float bestScore = -999999; // Float.MIN_VALUE won't work for some reason. Why?
        double theta = Math.random() * Math.PI * 2;
        double thetaIncrement = (Math.PI * 2) / DESTINATION_CANDIDATE_COUNT;
        ShipAPI enemy = null, bestScoringEnemy = null;

        for (int i = 0; i <= DESTINATION_CANDIDATE_COUNT; ++i) {
            float fudge = (float)Math.random() * 0.5f + 0.5f;
            Vector2f candidate = new Vector2f(
                (float)Math.cos(theta) * range * fudge + shipLoc.x,
                (float)Math.sin(theta) * range * fudge + shipLoc.y);
            theta += thetaIncrement;
            
            ship.getLocation().set(candidate);
            float score = SunUtils.getFPWorthOfSupport(ship, SUPPORT_RANGE)
            //        - Math.max(0, SunUtils.getFPWorthOfHostility(ship, SUPPORT_RANGE) - SunUtils.getFP(ship) * ship.getFluxTracker().getFluxLevel())
                    - SunUtils.estimateIncomingDamage(ship);
            
            if(!pointIsClear(candidate)) score -= ship.getCollisionRadius() / 10;
            
            if(aggressing) {
                enemy = AIUtils.getNearestEnemy(ship); // Don't move this out of the loop
                if(MathUtils.getDistance(ship, enemy) < weaponRange)
                    score += SunUtils.getFP(enemy) * (1.0f + enemy.getFluxTracker().getFluxLevel());
            } else score -= SunUtils.getFPWorthOfHostility(ship, SUPPORT_RANGE);
            
            if(score > bestScore) {
                bestScore = score;
                retVal = candidate;
                bestScoringEnemy = enemy;
            }
        }
        
        ship.getLocation().set(shipLoc);
        
        if(retVal == null) {
            retVal = MathUtils.getRandomPointInCircle(ship.getLocation(), range);
        }
        
        if(bestScoringEnemy != null) {
            ship.setShipAI(new JauntTurnTempAI(ship, bestScoringEnemy, this));
        }
        
        return retVal;
    }
    final void determineDestination() {                // Determine teleport destination
        destination = (ship.getShipAI() == null)
                ? new Vector2f(ship.getMouseTarget()) // For the player
                : getChosenAIDestination(); // For AI
        origin = new Vector2f(ship.getLocation());

        // Bring destination closer if it exceeds max range 
        float range = maxRange + ship.getCollisionRadius();
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
    }
    final void createDoppelganger() {
        // Create the doppelganger (placeholder double) at the origin
        doppelganger = (DamagingProjectileAPI)engine.spawnProjectile(
                ship, null, ship.getHullSpec().getHullId().concat("_doppelganger"),
                new Vector2f(origin.x + 12345, origin.y + 12345),
                ship.getFacing(), new Vector2f());
    }
    void manageDoppelganger() {
        doppelganger.setFacing(ship.getFacing() + (float)(Math.random() - 0.5) * 1);
        doppelganger.getVelocity().set(0, 0);
        Vector2f at = SunUtils.getDirectionalVector(doppelganger.getFacing() + 180);
        at.scale(doppelganger.getCollisionRadius() + 20);
        Vector2f.add(at, origin, at);
        at.x += (float)(Math.random() - 0.5) * 5;
        at.y += (float)(Math.random() - 0.5) * 5;
        doppelganger.getLocation().set(at);
        
        // Push entities away from the doppelganger to keep that space available
        List<CombatEntityAPI> entities = new ArrayList();
        entities.addAll(engine.getShips());
        entities.addAll(engine.getAsteroids());
        entities.remove(ship);
        for(CombatEntityAPI entity : entities) {
            if(entity instanceof ShipAPI && ((ShipAPI)entity).isFighter())
                continue;
            
            float distance = MathUtils.getDistance(entity, origin);
            float force = Math.min(1, 2 - distance / ship.getCollisionRadius());

            if(force > 0) {
                force *= amount * REPELL_FORCE;
                Vector2f direction = (Vector2f)VectorUtils.getDirectionalVector(
                        origin, entity.getLocation()).scale(force);
                Vector2f.add(entity.getLocation(), direction, entity.getLocation());
            }
        }
    }
    
    JauntSession(ShipAPI ship, float range) {
        this.ship = ship;
        maxRange = range;
        engine = Global.getCombatEngine();
        
        if(maxRange == 0) {
            origin = destination = new Vector2f( ship.getLocation());
            warpTime = 0;
        } else {
            determineDestination();
            warpTime = MathUtils.getDistance(origin, destination)
                    * SECONDS_IN_WARP_PER_SU + MIN_WARP_TIME;
        }
        
        
        createDoppelganger();
        JauntAI.setOrigin(ship, origin);
        jauntSessions.put(ship, this);
    }
    
    public Vector2f getOrigin() {
        return origin;
    }
    public Vector2f getDestination() {
        return destination;
    }
    public boolean isReturning() {
        return returning;
    }
    public boolean isWarping() {
        return progress != 1;
    }
    public void advance() {
        amount = engine.getElapsedInLastFrame();
        if(warpTime == 0) {
            progress = returning ? 0 : 1;
        } else {
            progress += (amount / warpTime) * (returning ? -1 : 1);
            progress = Math.max(0, Math.min(1, progress));
        }
        setAlpha((float)Math.pow(Math.abs((0.5f - progress) * 2), 3));
        ship.getMutableStats().getFluxDissipation().modifyMult(ID, 0.25f);
        
        boolean isPhased = ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive();
        
        if(!isWarping()) {
            destination = new Vector2f(ship.getLocation());
            ship.setCollisionClass(isPhased ? CollisionClass.NONE : CollisionClass.SHIP);
        } else if(progress == 0) {
            ship.getLocation().set(origin);
            toClear.add(this);
        } else {
            float sign = Math.signum(progress - 0.5f);
            float scale = (float)Math.pow(Math.abs(Math.cos((1-progress) * Math.PI)), 0.7f);
            ship.getLocation().set(SunUtils.getMidpoint(origin, destination, (sign * scale + 1) / 2));
            ship.setCollisionClass(CollisionClass.NONE);
            
            if(lastLoc != null && amount > 0) {
                float dist = MathUtils.getDistance(lastLoc, ship.getLocation());
                float speed = Math.min(dist / amount,
                        ship.getMutableStats().getMaxSpeed().getModifiedValue() * 2);
                ship.getVelocity().set((Vector2f)VectorUtils.getDirectionalVector(
                        lastLoc, ship.getLocation()));
                ship.getVelocity().scale(speed);
            }
            
            lastLoc = new Vector2f(ship.getLocation());
        }
        
        manageDoppelganger();
    }
    public void stopGoingHome() {
        returning = false;
        destination = new Vector2f( ship.getLocation());
        progress = 1;
        warpTime = 0;
    }
    public void goHome() {
        if(returning == false) {
            returning = true;
            warpTime = MathUtils.getDistance(origin, destination)
                    * SECONDS_IN_WARP_PER_SU + MIN_WARP_TIME;
        }
    }
    public void endNow() {
        if(doppelganger != null) engine.removeEntity(doppelganger);
        ship.setCollisionClass(CollisionClass.SHIP);
        jauntSessions.remove(ship);
        ship.getMutableStats().getFluxDissipation().unmodify(ID);
        setAlpha(1);
    }
}
