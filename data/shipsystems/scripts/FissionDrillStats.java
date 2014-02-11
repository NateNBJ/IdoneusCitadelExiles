package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.plugins.Utils;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class FissionDrillStats implements ShipSystemStatsScript {
	boolean within = false;

    @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 300f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 400f * effectLevel);
			stats.getMaxTurnRate().modifyPercent(id, -50f * effectLevel);
			stats.getTurnAcceleration().modifyPercent(id, -50f * effectLevel);
			
            if(!within) stats.getWeaponDamageTakenMult().modifyMult(id, 8);
			within = false;
                    
            ShipAPI ship = (ShipAPI)stats.getEntity();
            WeaponAPI drill = (WeaponAPI)ship.getAllWeapons().get(0);

            if(drill.isDisabled() && !within) {
                //Utils.print("DISSABLED!");
                ship.getFluxTracker().forceOverload(1f);
                return;
            } else {
                ship.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
                ship.giveCommand(ShipCommand.USE_SELECTED_GROUP, ship.getMouseTarget(), 0);
                ship.setCollisionClass(CollisionClass.FIGHTER);
                ship.setHitpoints(Math.max(1, ship.getHitpoints() - 1));
            }

            Vector2f at = drill.getLocation();
            at.x += (float)Math.random() * 40 - 20;
            at.y += (float)Math.random() * 40 - 20;
            List targets = AIUtils.getNearbyEnemies(ship, 600);

            for(Iterator iter = targets.iterator(); iter.hasNext();) {
                ShipAPI target = (ShipAPI)iter.next();

                if(target.getPhaseCloak() != null && target.getPhaseCloak().isActive()) continue;

                if(CollisionUtils.isPointWithinBounds(at, target)) {
                    float damage = 20000f * Global.getCombatEngine().getElapsedInLastFrame();
                    CombatUtils.applyForce(target, (Vector2f)ship.getVelocity().scale(0.98f), 100f);
                    Global.getCombatEngine().applyDamage(target, at, damage, DamageType.HIGH_EXPLOSIVE, 0, true, true, ship);
                    stats.getWeaponDamageTakenMult().modifyMult(id, 0);
                    stats.getEngineDamageTakenMult().modifyMult(id, 0);
                    Global.getCombatEngine().applyDamage(ship, at, damage / 10, DamageType.HIGH_EXPLOSIVE, 0, true, true, ship);
                    //stats.getWeaponDamageTakenMult().unmodify(id);
					within = true;
					
                    Global.getCombatEngine().spawnExplosion(
                            at, // Location
                            target.getVelocity(), // Velocity
                            Color.white, // How to get faction color?
                            50 + (float)Math.random() * 100, // Size
                            2 + (float)Math.random() * 2); // Duration
                    Global.getSoundPlayer().playSound("collision_ships", 1, 1, ship.getLocation(), target.getVelocity());
                }
            }
		}
	}
	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        if(ship != null) ship.setCollisionClass(CollisionClass.SHIP);

		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
        stats.getWeaponDamageTakenMult().unmodify(id);
        stats.getEngineDamageTakenMult().unmodify(id);
	}	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased engine power", false);
		} else if (index == 1) {
			return new StatusData("can drill through ships", false);
		}
		return null;
	}
}
