package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.plugins.SunUtils;
import java.util.HashMap;
import java.util.Iterator;

public class TacticalAnsible extends BaseHullMod {
    static final float CP_REGEN_PER_SECOND = 0.05f;
    static final String EFFECT_ID = "sun_ice_tactical_ansible_mod";

    static final HashMap OBJECTIVE_CP_BONUS = new HashMap();
    static {
        OBJECTIVE_CP_BONUS.put("nav_buoy", 1);
        OBJECTIVE_CP_BONUS.put("sensor_array", 1);
        OBJECTIVE_CP_BONUS.put("comm_relay", 3);
    }

    float bonus = 0;
    float tick = 0;
    float initialCP;

    int countObjectiveCpBonus(int fleetSide) {
        int count = 0;

        for(Iterator iter = Global.getCombatEngine().getObjectives().iterator(); iter.hasNext();) {
            BattleObjectiveAPI obj = (BattleObjectiveAPI)iter.next();

            if(obj.getOwner() != fleetSide || !OBJECTIVE_CP_BONUS.containsKey(obj.getType()))
                continue;

            count += (Integer)OBJECTIVE_CP_BONUS.get(obj.getType());
        }

        return count;
    }
    int countCommRelays(int fleetSide) {
        int count = 0;

        for(Iterator iter = Global.getCombatEngine().getObjectives().iterator(); iter.hasNext();) {
            BattleObjectiveAPI obj = (BattleObjectiveAPI)iter.next();

            if(obj.getOwner() != fleetSide || !obj.getType().equals("comm_relay"))
                continue;

            ++count;
        }

        return count;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        tick = 0;
        bonus = 0;
        initialCP = Global.getCombatEngine().getFleetManager(ship.getOwner())
                .getCommandPointsStat().getModifiedValue();
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        float cpLimit = initialCP + countObjectiveCpBonus(ship.getOwner());
        
        tick += amount * (0.5f + countCommRelays(ship.getOwner()))
                * CP_REGEN_PER_SECOND;
        
        if(tick > 1) {
            tick -= 1;

            CombatFleetManagerAPI fleet = Global.getCombatEngine().getFleetManager(ship.getOwner());
            float cp = fleet.getCommandPointsStat().getModifiedValue();

            SunUtils.print(""+cpLimit);
            if(cp < cpLimit) {
                fleet.getCommandPointsStat().modifyFlat(EFFECT_ID, ++bonus);
            }
        }
    }
}