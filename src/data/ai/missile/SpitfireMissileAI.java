package data.ai.missile;

import com.fs.starfarer.api.combat.MissileAPI;
import data.tools.SunUtils;

public class SpitfireMissileAI extends BaseMissileAI {
    boolean fizzled = false;
    
    public SpitfireMissileAI() {}
    public SpitfireMissileAI(MissileAPI missile) {
        this.missile = missile;
        findTarget();
        
    }

    @Override
    public void evaluateCircumstances() {
        if(target == null) findTarget();
        
        if(fizzled && Math.random() < 0.1f) SunUtils.destroy(missile);
    }
    
    @Override
    public void advance(float amount) {
        super.advance(amount);
        
        if(target == null || fizzled) return;
        
        accelerate();
            
        if(missile.isFizzling()) fizzled = true;
        
        turnToward(target);
        strafeToward(target);
    }
}
