package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import data.scripts.IntervalTracker;
import java.util.Iterator;
import java.util.List;

public class TriangulationRelay extends BaseHullMod {
    static final float RANGE_BONUS = 100.0f;
    static final String EFFECT_ID = "sun_ice_triangulation_relay_mod";

    IntervalTracker tracker = new IntervalTracker(30f / 60f);
    boolean shipIsDead = false;


    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        tracker.reset();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(shipIsDead || !tracker.intervalElapsed()) return;

        String effectId = EFFECT_ID + ship.hashCode();
        List ships = Global.getCombatEngine().getShips();

        for(Iterator iter = ships.iterator(); iter.hasNext();) {
            ShipAPI ally = (ShipAPI)iter.next();
            StatBonus rangeBonus = ally.getMutableStats().getBallisticWeaponRangeBonus();

            if(Math.abs(ship.getOwner() - ally.getOwner()) == 1) continue;

            if(ship.isAlive() && !ship.isRetreating()) {
                rangeBonus.modifyFlat(effectId, RANGE_BONUS);
            } else {
                rangeBonus.unmodify(effectId);
                shipIsDead = true;
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

}