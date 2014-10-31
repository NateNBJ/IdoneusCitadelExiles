package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;

public class RecallTeleporter extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
    }
}