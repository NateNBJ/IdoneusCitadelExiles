package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import static data.ai.missile.MineAI.PING_COLOR;
import data.tools.SunUtils;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class MinePodAI extends BaseMissileAI {
    static final float DEPLOY_DISTANCE = 40;
    static final float AVOID_DISTANCE = 150;
    
    CombatEntityAPI entityToAvoid = null;
    
    Vector2f deployPoint;
    
    public MinePodAI() {}
    public MinePodAI(MissileAPI missile) {
        this.missile = missile;
        if(missile.getSource() == Global.getCombatEngine().getPlayerShip()) {
            deployPoint = new Vector2f(missile.getSource().getMouseTarget());
        }
        
    }

    @Override
    public void evaluateCircumstances() {
        float dist, bestDist = Float.MAX_VALUE;
        CombatEntityAPI winner = AIUtils.getNearestShip(missile);
        
        float angle = (float)Math.toRadians(missile.getFacing());
        Vector2f leadPoint = new Vector2f(
                (float)Math.cos(angle) * AVOID_DISTANCE + missile.getLocation().x,
                (float)Math.sin(angle) * AVOID_DISTANCE + missile.getLocation().y);
        
        Global.getCombatEngine().addHitParticle(leadPoint,
                missile.getVelocity(), 60, 1.5f, 0.2f, PING_COLOR);
        
        if(winner != null && (winner.getOwner() != missile.getOwner() || !((ShipAPI)winner).isAlive()))
                bestDist = MathUtils.getDistance(winner, leadPoint);
        
        List obstacles = Global.getCombatEngine().getShips();
        obstacles.addAll(Global.getCombatEngine().getAsteroids());
        
        for (CombatEntityAPI roid : CombatUtils.getAsteroidsWithinRange(leadPoint, AVOID_DISTANCE)) {
            dist = MathUtils.getDistance(roid, leadPoint);
            
            if(dist < bestDist) {
                bestDist = dist;
                winner = roid;
            }
        }
        
        entityToAvoid = (bestDist < AVOID_DISTANCE) ? winner : null;
    }
//    @Override
//    public CombatEntityAPI findFlareTarget(float range) { return null; }
    
//    @Override
//    public void evaluateCircumstances() {
//        float dist, bestDist = Float.MAX_VALUE;
//        CombatEntityAPI winner = null;
//        
//        float angle = (float)Math.toRadians(missile.getFacing());
//        Vector2f leadPoint = new Vector2f(
//                (float)Math.cos(angle) * AVOID_DISTANCE + missile.getLocation().x,
//                (float)Math.sin(angle) * AVOID_DISTANCE + missile.getLocation().y);
//        
////        Global.getCombatEngine().addHitParticle(leadPoint,
////                missile.getVelocity(), 60, 1.5f, 0.2f, PING_COLOR);
//        
////        if(winner != null && (winner.getOwner() != missile.getOwner() || !((ShipAPI)winner).isAlive()))
////                bestDist = MathUtils.getDistance(winner, leadPoint);
//        
//        List obstacles = Global.getCombatEngine().getShips();
//        obstacles.addAll(Global.getCombatEngine().getAsteroids());
//        
//        for (CombatEntityAPI entity : (List<CombatEntityAPI>)obstacles) {
//            // Skip living allies since the pod will pass over them anyway.
//            if(entity instanceof ShipAPI
//                    && entity.getOwner() == missile.getOwner()
//                    && ((ShipAPI)entity).isAlive())
//                continue;
//            
//            dist = MathUtils.getDistanceSquared(entity, leadPoint);
//            
//            if(dist < bestDist) {
//                bestDist = dist;
//                winner = entity;
//            }
//        }
//        
//        entityToAvoid = (Math.sqrt(bestDist) < AVOID_DISTANCE) ? winner : null;
//    }
//    
    @Override
    public void advance(float amount) {
        super.advance(amount);
        
        accelerate();
        
        if(entityToAvoid == null) {
            if(Math.abs(getAngleTo(deployPoint)) < 5) accelerate();
            else decelerate();
            
            turnToward(deployPoint);
            strafeToward(deployPoint);
        } else if(Math.abs(getAngleTo(entityToAvoid)) > 75) {
            turnToward(deployPoint);
            strafeToward(deployPoint);
            accelerate();
        } else {
            turnAway(entityToAvoid);
            strafeAway(entityToAvoid);
            accelerate();
            
            Global.getCombatEngine().addHitParticle(missile.getLocation(),
                missile.getVelocity(), 60, 1.5f, 0.2f, Color.red);
        }
        
        if(MathUtils.getDistance(missile, deployPoint) <= DEPLOY_DISTANCE
                || missile.isFizzling()
                || missile.getHullLevel() < 0.5f) {
            
            SunUtils.destroy(missile);
            
            float angle = 360f * (float)Math.random();
            
            for(int i = 0; i < 18; ++i) {
                angle += 60f + Math.random() * 60f;
                
                CombatEntityAPI mine = Global.getCombatEngine().spawnProjectile(
                        missile.getSource(), null, "sun_ice_mine",
                        missile.getLocation(), angle, new Vector2f());
                
                mine.getVelocity().scale((float)Math.random() * 1.6f + 0.5f);
            }
        }
    }
}
