package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import data.tools.SunUtils;

public class SpitfireMissileAI extends BaseMissileAI {
    boolean fizzled = false;
    double waveOffset;
    
    public SpitfireMissileAI() {}
    public SpitfireMissileAI(MissileAPI missile) {
        this.missile = missile;
        findTarget();
        waveOffset = (Math.random() * Math.PI * 2);
        
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
        
        double wave = Math.sin(waveOffset + Global.getCombatEngine().getTotalElapsedTime(false) * 10);
        
        if(wave < 0.5) {
            strafeToward(target);
        } else if(wave > 0.8) {
            //turnAway(target);
            strafeAway(target);
        }
        
            turnToward(target);
    }
}
