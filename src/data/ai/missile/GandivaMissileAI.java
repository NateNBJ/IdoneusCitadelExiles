package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class GandivaMissileAI extends BaseMissileAI {
    static final float[] STAGE_DURATION = { 2, 1.5f, 3600 };
    static final String SOUND_ID = "engine_accelerate";
    static final float SOUND_PITCH = 0.5f;
    static final float SOUND_VOLUME = 3.0f;
    static final float FAKE_TURN_MODIFIER = 10.5f;
    
    int stage = 0; // 0:Drift, 1:Burn, 2:Cruise
    float duration = STAGE_DURATION[stage];
    int tick = 0;

    public GandivaMissileAI() {}
    public GandivaMissileAI(MissileAPI missile) {
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

        if(target == null) return;

        duration -= amount;

        if(duration <= 0) {
            ++stage;
            duration += STAGE_DURATION[stage];

            if(stage == 1) {
                Global.getSoundPlayer().playSound(SOUND_ID, SOUND_PITCH,
                        SOUND_VOLUME, missile.getLocation(), missile.getVelocity());
            }
        }

        if(stage == 1) {
            accelerate();
            strafeToward(target);
        }

        if(stage < 2) {
            turnToward(target);
        } else if(!missile.isFizzling()) {
            float angleDif = MathUtils.getShortestRotation(missile.getFacing(),
                VectorUtils.getAngle(missile.getLocation(), target.getLocation()));

            float dAngle = Math.signum(angleDif) * FAKE_TURN_MODIFIER * amount;

            VectorUtils.rotate(missile.getVelocity(), dAngle, missile.getVelocity());
            missile.setFacing(MathUtils.clampAngle(missile.getFacing() + dAngle));
        }
    }
}
