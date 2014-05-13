package data.weapons.onhit;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class HypermassOnHitEffect implements OnHitEffectPlugin {
    public static final float FORCE = 1500f;
    
    @Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {
		CombatUtils.applyForce(target, (Vector2f)projectile.getVelocity(), FORCE);
//
//        engine.spawnEmpArc((ShipAPI)projectile.getSource(),
//                new Vector2f(target.getLocation().x + 1, target.getLocation().y),
//                target, target, DamageType.KINETIC, FORCE, FORCE, 1000, null, 20, Color.orange, Color.red);
	}
}