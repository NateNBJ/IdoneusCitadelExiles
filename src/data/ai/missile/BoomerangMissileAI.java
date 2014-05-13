package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import java.util.Random;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class BoomerangMissileAI implements MissileAIPlugin {
    MissileAPI missile;

    public BoomerangMissileAI() {}
    public BoomerangMissileAI(MissileAPI missile) {
        this.missile = missile;
    }

    @Override
    public void advance(float amount) {
        missile.giveCommand(ShipCommand.ACCELERATE);

        if(missile.isFizzling()) {
            Global.getCombatEngine().applyDamage(missile, missile.getLocation(),
                    999, DamageType.FRAGMENTATION, 0, true, true,
                    missile.getSource());

            ShipAPI target = missile.getSource().getShipTarget();

            if(target == null
                    || !target.isAlive()
                    || target.getOwner() == missile.getOwner()) {
                target = AIUtils.getNearestEnemy(missile);
            }
            
            if(target == null) return;

            Global.getCombatEngine().spawnProjectile(missile.getSource(),
                    null, "sun_ice_boomeranghack", missile.getLocation(),
                    VectorUtils.getAngle(missile.getLocation(), target.getLocation()),
                    (Vector2f)missile.getVelocity().scale(0.25f));
        }
    }

}
