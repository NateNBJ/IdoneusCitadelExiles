
package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import data.scripts.BaseMissileAI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class AttackDroneMissileAI extends BaseMissileAI {
    Vector2f destOffset = new Vector2f();
    Vector2f dest = new Vector2f();

    int ammo = 100;

    public AttackDroneMissileAI(MissileAPI missile) {
        super(missile);
        
        findTarget();
        this.circumstanceEvaluationTimer.setInterval(0.6f, 1.4f);


    }

    @Override
    public void findTarget() {
        target = missile.getSource().getShipTarget();
        
        if(target == null || target.getOwner() != missile.getOwner())
            target = missile.getSource();

        if(!target.isAlive())  target = AIUtils.getNearestAlly(missile);
    }

    @Override
    public void evaluateCircumstances() {
        super.evaluateCircumstances();

        if(target == null || !target.isAlive()) findTarget();
        
        Vector2f.sub(MathUtils.getRandomPointInCircle(target.getLocation(),
                target.getCollisionRadius()), target.getLocation(), destOffset);
        
        if(missile.isFading() || ammo <= 0) selfDestruct();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        Vector2f.add(destOffset, target.getLocation(), dest);
        
        accelerate();
        turnToward(dest);
    }

    public void selfDestruct() {
        Global.getCombatEngine().applyDamage(missile, missile.getLocation(),
                missile.getMaxHitpoints(), DamageType.FRAGMENTATION, 0, true,
                true, missile.getSource());
    }
}
