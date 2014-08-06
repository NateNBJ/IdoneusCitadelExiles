package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.SunUtils;
import java.util.ArrayList;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class MinePodAI extends BaseMissileAI {
    static final float DEPLOY_DELEY_TIME = 3;
    static final float DEPLOY_DISTANCE = 40;
    static final float AVOID_DISTANCE = 150;
    
    CombatEntityAPI entityToAvoid = null;
    float deployDelay = DEPLOY_DELEY_TIME;
    Vector2f deployPoint;
    CombatEngineAPI engine;
    
    public MinePodAI() {}
    public MinePodAI(MissileAPI missile) {
        this.missile = missile;
        engine = Global.getCombatEngine();
        deployPoint = new Vector2f(missile.getSource().getMouseTarget());
        List<BattleObjectiveAPI> objectives = Global.getCombatEngine().getObjectives();
        
        if(missile.getSource().getShipAI() == null) {
            Global.getCombatEngine().addHitParticle(deployPoint,
                new Vector2f(), 60, 1, 0.7f, MineAI.PING_COLOR);
            Global.getSoundPlayer().playUISound("sun_ice_deploy_mine_pod", 1, 1);
        } else if(!objectives.isEmpty()) {
            float bestRecord = missile.getWeapon().getRange();
            
            for(BattleObjectiveAPI objective : objectives) {
                float distance = MathUtils.getDistance(objective, missile);
                
                if(distance < bestRecord) {
                    bestRecord = distance;
                    deployPoint = objective.getLocation();
                }
            }
        }
    }
    
    void destroyOldMines() {
        for(MissileAPI m : engine.getMissiles())
            if(m.getWeapon() == missile.getWeapon() && m.getProjectileSpecId().equals("sun_ice_mine"))
                SunUtils.destroy(m);
    }
    void deployNewMines() {
        float angle = 360f * (float)Math.random();

        for(int i = 0; i < 24; ++i) {
            angle += 60f + Math.random() * 60f;

            CombatEntityAPI mine = engine.spawnProjectile(
                    missile.getSource(), missile.getWeapon(), "sun_ice_mine",
                    missile.getLocation(), angle, new Vector2f());

            mine.getVelocity().scale((float)Math.random() * 1.4f + 0.5f);
        }        
    }
    
    @Override
    public CombatEntityAPI findFlareTarget(float range) { return null; }
    @Override
    public void evaluateCircumstances() {
        float dist, bestDist = Float.MAX_VALUE;
        CombatEntityAPI winner = null;
        
        float angle = (float)Math.toRadians(missile.getFacing());
        Vector2f leadPoint = new Vector2f(
                (float)Math.cos(angle) * AVOID_DISTANCE + missile.getLocation().x,
                (float)Math.sin(angle) * AVOID_DISTANCE + missile.getLocation().y);
        
        List obstacles = new ArrayList(engine.getShips());
        obstacles.addAll(engine.getAsteroids());
        
        for (CombatEntityAPI entity : (List<CombatEntityAPI>)obstacles) {
            // Skip living allies since the pod will pass over them anyway.
            if(entity instanceof ShipAPI
                    && entity.getOwner() == missile.getOwner()
                    && ((ShipAPI)entity).isAlive())
                continue;
            
            dist = MathUtils.getDistanceSquared(entity, leadPoint);
            
            if(dist < bestDist) {
                bestDist = dist;
                winner = entity;
            }
        }
        
        entityToAvoid = (Math.sqrt(bestDist) < AVOID_DISTANCE) ? winner : null;
    }
    @Override
    public void advance(float amount) {
        super.advance(amount);
        
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
            
//            Global.getCombatEngine().addHitParticle(missile.getLocation(),
//                missile.getVelocity(), 60, 1.5f, 0.2f, Color.red);
        }
        
        if((deployDelay -= amount) < 0
                && (MathUtils.getDistance(missile, deployPoint) <= DEPLOY_DISTANCE
                || missile.isFizzling()
                || missile.getHullLevel() < 0.5f)) {
            
            SunUtils.destroy(missile);
            destroyOldMines();
            deployNewMines();

        }
    }
}
