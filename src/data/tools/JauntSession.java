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
    
    public static void clearStaticData() {
        jauntSessions.clear();
    }
    public static void advanceAll(float amount) {
        for(JauntSession jaunt : jauntSessions.values()) jaunt.advance(amount);
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
    float progress, maxRange, alpha, warpTime;
    boolean returning = false;
    
    void setAlpha(float alpha) {
        int a = Math.min(255, Math.max(0, (int)(alpha * 255f)));
        Color c = ship.getSpriteAPI().getColor();
        c = new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
        ship.getSpriteAPI().setColor(c);
        for(WeaponAPI w : ship.getAllWeapons()) {
            w.getSprite().setColor(c);
            if(w.getBarrelSpriteAPI() != null) w.getBarrelSpriteAPI().setColor(c);
            if(w.getUnderSpriteAPI()!= null) w.getUnderSpriteAPI().setColor(c);
        }
    }
    boolean pointIsClear(Vector2f at) {
        for(ShipAPI s : Global.getCombatEngine().getShips()) {
            if(s != ship && s.getCollisionClass() == CollisionClass.SHIP
                    && MathUtils.getDistance(s, at) <= 0)
                return false;
        }

        return CombatUtils.getAsteroidsWithinRange(at, ship.getCollisionRadius() * 0.8f).isEmpty();
    }
    Vector2f getAIDestinationChoice() {
        Vector2f retVal = null, shipLoc = new Vector2f(ship.getLocation());
        boolean aggressing = AIUtils.getEnemiesOnMap(ship).size() > 0
                && ship.getFluxTracker().getFluxLevel() < 0.4 + Math.random() * 0.3;
        float range = maxRange + ship.getCollisionRadius();
        float weaponRange = IceUtils.estimateOptimalRange(ship) * 0.8f;
        float bestScore = -999999; // Float.MIN_VALUE won't work for some reason. Why?
        double theta = Math.random() * Math.PI * 2;
        double thetaIncrement = (Math.PI * 2) / DESTINATION_CANDIDATE_COUNT;
        ShipAPI enemy = null, bestScoringEnemy = null;
        boolean canTurn = IceUtils.getEngineFractionDisabled(ship) > 0;

        for (int i = 0; i <= DESTINATION_CANDIDATE_COUNT; ++i) {
            float fudge = (float)Math.random() * 0.5f + 0.5f;
            Vector2f candidate = new Vector2f(
                (float)Math.cos(theta) * range * fudge + shipLoc.x,
                (float)Math.sin(theta) * range * fudge + shipLoc.y);
            theta += thetaIncrement;
            
            ship.getLocation().set(candidate);
//            float score = IceUtils.getFPWorthOfSupport(ship, SUPPORT_RANGE)
//            //        - Math.max(0, IceUtils.getFPWorthOfHostility(ship, SUPPORT_RANGE) - IceUtils.getFP(ship) * ship.getFluxTracker().getFluxLevel())
//                    - IceUtils.estimateIncomingDamage(ship);
            float score = 0;
            
            if(aggressing) {
                enemy = AIUtils.getNearestEnemy(ship); // Don't move this out of the loop
                float rangeDist = weaponRange - MathUtils.getDistance(ship, enemy);
                if(rangeDist > 0) {
                    boolean shieldBlocked = enemy.getShield() != null && enemy.getShield().isWithinArc(candidate);
                    score += IceUtils.getFP(enemy)
                            * (1.0f + enemy.getFluxTracker().getFluxLevel())
                            * (shieldBlocked ? 0.25f : 1)
                            * (0.5f + 0.5f * (weaponRange - rangeDist) / weaponRange);
                    
                    if(!canTurn) {
                        float angleTo = VectorUtils.getAngle(ship.getLocation(),enemy.getLocation());
                        float degreesFromFacingTarget = Math.abs(MathUtils.getShortestRotation(angleTo, ship.getFacing()));
                        score *= 1 - degreesFromFacingTarget / 180f;
                    }
                }
            } else score -= IceUtils.getFPWorthOfHostility(ship, SUPPORT_RANGE);
            
            score -= IceUtils.estimateIncomingDamage(ship) * 0.05f;
            
            if(!pointIsClear(candidate)) score -= ship.getCollisionRadius() / 10;
            
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
    final void determineDestination() {
        destination = (ship.getShipAI() == null)
                ? new Vector2f(ship.getMouseTarget()) // For the player
                : getAIDestinationChoice(); // For AI
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
                new Vector2f(origin.x + 1234, origin.y + 1234),
                ship.getFacing(), new Vector2f());
    }
    void manageDoppelganger(float amount) {
        doppelganger.setFacing(ship.getFacing() + (float)(Math.random() - 0.5) * 1);
        doppelganger.getVelocity().set(0, 0);
        Vector2f at = IceUtils.getDirectionalVector(doppelganger.getFacing() + 180);
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
    public void advance(float amount) {
        if(ship.getFluxTracker().isOverloadedOrVenting()) goHome();
        if(warpTime == 0) {
            progress = returning ? 0 : 1;
        } else {
            progress += (amount / warpTime) * (returning ? -1 : 1);
            progress = Math.max(0, Math.min(1, progress));
        }
        setAlpha((float)Math.pow(Math.abs((0.5f - progress) * 2), 3));
        
        boolean isPhased = ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive();
        
        if(progress == 0 || !engine.isEntityInPlay(ship)) {
            ship.getLocation().set(origin);
            toClear.add(this);
        } else if(!isWarping()) {
            destination = new Vector2f(ship.getLocation());
            ship.setCollisionClass(isPhased ? CollisionClass.NONE : CollisionClass.SHIP);
        } else {
            float sign = Math.signum(progress - 0.5f);
            float scale = (float)Math.pow(Math.abs(Math.cos((1-progress) * Math.PI)), 0.7f);
            ship.getLocation().set(IceUtils.getMidpoint(origin, destination, (sign * scale + 1) / 2));
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
        
        manageDoppelganger(amount);
    }
    public void stopGoingHome() {
        returning = false;
        destination = new Vector2f(ship.getLocation());
        progress = 1;
        warpTime = 0;
        if(ship.getShipAI() != null) ship.resetDefaultAI();
    }
    public void goHome() {
        if(!jauntSessions.containsValue(this)) return;
        
        if(returning == false) {
            returning = true;
            warpTime = MathUtils.getDistance(origin, destination)
                    * SECONDS_IN_WARP_PER_SU + MIN_WARP_TIME;
            if(ship.getShipAI() != null) {
                ship.setShipAI(new JauntTurnTempAI(ship,
                        AIUtils.getNearestEnemy(doppelganger), this));
            }
        }
        
        if(ship.getFluxTracker().isVenting()) {
            ship.getMutableStats().getVentRateMult().modifyMult(ID, 0.5f);
            ship.getMutableStats().getFluxDissipation().unmodify(ID);
        } else {
            ship.getMutableStats().getFluxDissipation().modifyMult(ID, 0.0f);
            ship.getMutableStats().getVentRateMult().unmodify(ID);
        }
        
        ship.setShipSystemDisabled(true);
    }
    public void endNow() {
        boolean isPhased = ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive();
        ship.setCollisionClass(isPhased ? CollisionClass.NONE : CollisionClass.SHIP);
        ship.setShipSystemDisabled(false);
        jauntSessions.remove(ship);
        ship.getMutableStats().getVentRateMult().unmodify(ID);
        ship.getMutableStats().getFluxDissipation().unmodify(ID);
        if(ship.getShipAI() != null) ship.resetDefaultAI();
        if(doppelganger != null) engine.removeEntity(doppelganger);
        setAlpha(1);
    }
}
