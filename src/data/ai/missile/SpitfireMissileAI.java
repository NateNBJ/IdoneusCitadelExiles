package data.ai.missile;

import com.fs.starfarer.api.combat.MissileAPI;

public class SpitfireMissileAI extends BaseMissileAI {
    public SpitfireMissileAI() {}
    public SpitfireMissileAI(MissileAPI missile) {
        this.missile = missile;
        findTarget();
    }

    @Override
    public void evaluateCircumstances() {
        if(target == null) findTarget();
    }
    
    @Override
    public void advance(float amount) {
        super.advance(amount);
        
        accelerate();
        
        if(target == null || missile.isFading()) return;
        
        turnToward(target);
        strafeToward(target);
    }
}
