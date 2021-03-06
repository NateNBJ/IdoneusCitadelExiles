package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class MunitionsAutoFac extends BaseHullMod {

    final Map dontRestoreAmmoUntil = new WeakHashMap();

    private static final Map mag = new HashMap();

    static {
        mag.put(HullSize.FIGHTER, 32f);
        mag.put(HullSize.FRIGATE, 16f);
        mag.put(HullSize.DESTROYER, 8f);
        mag.put(HullSize.CRUISER, 4f);
        mag.put(HullSize.CAPITAL_SHIP, 2f);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        HullSize size;
        
        switch (index) {
            case 0: size = HullSize.FRIGATE; break;
            case 1: size = HullSize.DESTROYER; break;
            case 2: size = HullSize.CRUISER; break;
            case 3: size = HullSize.CAPITAL_SHIP; break;
            default: return null;
        }
        
        return "" + ((Float)mag.get(size)).intValue();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if ((Float) dontRestoreAmmoUntil.get(ship) > Global.getCombatEngine().getTotalElapsedTime(false)
                || !ship.isAlive() || ship.getFluxTracker().isOverloadedOrVenting()) {
            return;
        }

        WeaponAPI winner = null;
        float lowestAmmo = 1;

        for (Iterator iter = ship.getAllWeapons().iterator(); iter.hasNext();) {
            WeaponAPI weapon = (WeaponAPI) iter.next();

            if (!weapon.usesAmmo() || weapon.getSpec().getAmmoPerSecond() > 0) {
                continue;
            }

            float ammo = weapon.getAmmo() / (float) weapon.getMaxAmmo();

            if (ammo < lowestAmmo) {
                lowestAmmo = ammo;
                winner = weapon;
            }
        }

        if (winner == null) {
            dontRestoreAmmoUntil.put(ship, Global.getCombatEngine().getTotalElapsedTime(false) + 1);
            return;
        }

        float op = winner.getSpec().getOrdnancePointCost(null);
        int ammoToRestore = (int) Math.max(1, Math.floor(winner.getMaxAmmo() / op));
        ammoToRestore = Math.min(ammoToRestore, winner.getMaxAmmo() - winner.getAmmo());
        winner.setAmmo(winner.getAmmo() + ammoToRestore);
        dontRestoreAmmoUntil.put(ship, Global.getCombatEngine().getTotalElapsedTime(false)
                + (Float) mag.get(ship.getHullSpec().getHullSize())
                * ((ammoToRestore / (float) winner.getMaxAmmo()) * op));
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        dontRestoreAmmoUntil.put(stats.getEntity(), 0.0f);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getHullSpec().getHullId().startsWith("sun_ice_");
    }
}
