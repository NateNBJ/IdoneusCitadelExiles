package data.weapons.onhit;

import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.SunUtils;
import org.lazywizard.lazylib.combat.DefenseType;
import org.lazywizard.lazylib.combat.DefenseUtils;

public class SurgeOnHitEffect implements OnHitEffectPlugin {
    @Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, CombatEngineAPI engine) {

        //shieldHit always returns false? Not in IonCannonOnHitEffect.java...
        if (target instanceof ShipAPI && !(DefenseUtils.getDefenseAtPoint((ShipAPI)target, point) == DefenseType.SHIELD)) {
            ((ShipAPI)target).getFluxTracker().forceOverload(0);

        }
        SunUtils.print("" + shieldHit);
	}
}