package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import data.tools.RecallStarter;
import java.util.HashMap;
import java.util.Map;

public class RecallTeleporter extends BaseHullMod {
    public static Map<ShipAPI, RecallStarter> Starters = new HashMap();
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        
        Starters.put(ship, new RecallStarter(ship));
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        Starters.get(ship).advance();
    }
}