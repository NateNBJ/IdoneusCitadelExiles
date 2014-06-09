package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import data.tools.SunUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class MinePodAI extends BaseMissileAI {
    static final float DEPLOY_DISTANCE = 10;
    
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
    }
    
    @Override
    public void advance(float amount) {
        float angleDif = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), deployPoint));

        if(Math.abs(angleDif) < 5) accelerate();
        else decelerate();
        
        turnToward(deployPoint);
        
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
