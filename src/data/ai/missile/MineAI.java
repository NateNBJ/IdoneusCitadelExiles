package data.ai.missile;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class MineAI extends BaseMissileAI {
    static final float ATTACK_RANGE = 600f;
    static final float BASE_FUEL = 0.5f;
    static final float LEAD_TIME_PER_DISTANCE = 2f / ATTACK_RANGE; // in seconds
    
    float fuel = BASE_FUEL;
    boolean stopped = false;
    boolean attacking = false;
    
    public MineAI() {}
    public MineAI(MissileAPI missile) {
        this.missile = missile;
    }
    
    @Override
    public void advance(float amount) {
        if(!stopped) {
            if(Math.abs(missile.getVelocity().x) < 0.0001
                    && Math.abs(missile.getVelocity().y) < 0.0001)
                stopped = true;
            
            decelerate();
            return;
        }
        
        super.advance(amount);
        
        if(!(target instanceof ShipAPI)) return;

        float distance = MathUtils.getDistance(missile, target);
        float leadTime = distance * LEAD_TIME_PER_DISTANCE;
        Vector2f leadPoint = (Vector2f)(new Vector2f(target.getVelocity()).scale(leadTime));
        Vector2f.add(leadPoint, target.getLocation(), leadPoint);
        ShipSystemAPI cloak = ((ShipAPI)target).getPhaseCloak();
        
        if(attacking) {
            if(fuel > 0) {
                accelerate();
                fuel -= amount;
            } else missile.flameOut();
        } else if((cloak == null || !cloak.isActive())
                && (distance <= ATTACK_RANGE || missile.getHullLevel() < 1)
                && isFacing(leadPoint)) {
            attacking = true;
        } else turnToward(leadPoint);
    }
}
