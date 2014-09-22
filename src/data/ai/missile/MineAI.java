package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import static data.ai.missile.BaseMissileAI.DEFAULT_FLARE_VULNERABILITY_RANGE;
import data.tools.IceUtils;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class MineAI extends BaseMissileAI {
    public static final float ATTACK_RANGE = 750f;
    public static final float MAX_TIME_TO_STOP = 5f;
    public static final float TTL_AFTER_BURN = 3f;
    public static final float BASE_FUEL = 0.5f;
    public static final float LEAD_TIME_PER_DISTANCE = 1.2f / ATTACK_RANGE; // in seconds
    public static final Color PING_COLOR = new Color(0, 250, 220, 255);
    
    float fuel = BASE_FUEL;
    float timeOfDeployment;
    float timeOfBurn = Float.MAX_VALUE;
    boolean stopped = false;
    boolean attacking = false;
    
    void ping() {
        Global.getCombatEngine().addHitParticle(missile.getLocation(),
                missile.getVelocity(), 40, 0.8f, 0.1f, PING_COLOR);
    }
    
    public MineAI() {}
    public MineAI(MissileAPI missile) {
        this.missile = missile;
        timeOfDeployment = Global.getCombatEngine().getTotalElapsedTime(false);
    }
    
    @Override
    public CombatEntityAPI findTarget() {
        findFlareTarget(DEFAULT_FLARE_VULNERABILITY_RANGE);
        
        if(targetIsFlare()) return target;
        
        target = AIUtils.getNearestEnemy(missile);
        
        return target;
    }
    @Override
    public void advance(float amount) {
        float time = Global.getCombatEngine().getTotalElapsedTime(false);
        
        if(!stopped) {
            if(time - timeOfDeployment > MAX_TIME_TO_STOP
                    || (Math.abs(missile.getVelocity().x) < 0.00001
                    && Math.abs(missile.getVelocity().y) < 0.00001)) {
                stopped = true;
                missile.getVelocity().set(0, 0);
                ping();
            }
            
            decelerate();
            return;
        }
        
        super.advance(amount);
        
        if(Math.random() < amount * 0.9f) ping();
        
        if(target == null) {
            decelerate();
            return;
        }

        float distance = MathUtils.getDistance(missile, target);
        float leadTime = distance * LEAD_TIME_PER_DISTANCE;
        Vector2f leadPoint = (Vector2f)(new Vector2f(target.getVelocity()).scale(leadTime));
        Vector2f.add(leadPoint, target.getLocation(), leadPoint);
        ShipSystemAPI cloak = (target instanceof ShipAPI)
                ? ((ShipAPI)target).getPhaseCloak() : null;
        
        if(attacking) {
            if(fuel > 0) {
                accelerate();
                fuel -= amount;
            }
        } else if((cloak == null || !cloak.isActive())
                && (distance <= ATTACK_RANGE)
                && isFacing(leadPoint)) {
            attacking = true;
            timeOfBurn = time;
        } else {
            turnToward(leadPoint);
            decelerate();
        }
        
        if(time - timeOfBurn > TTL_AFTER_BURN) IceUtils.destroy(missile);
    }
}
