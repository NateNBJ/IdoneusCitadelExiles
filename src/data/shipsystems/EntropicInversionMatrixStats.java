package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.ICEModPlugin;
import data.tools.IceUtils;
import java.awt.Color;

public class EntropicInversionMatrixStats implements ShipSystemStatsScript {

    float[][] prev = null;
    float initialFlux = Float.MIN_VALUE;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();

        float[][] now = ship.getArmorGrid().getGrid();
        float max = ship.getArmorGrid().getMaxArmorInCell();
        int gridWidth = now.length;
        int gridHeight = now[0].length;

        if(initialFlux == Float.MIN_VALUE)
            initialFlux = ship.getFluxTracker().getCurrFlux();
        
        if(prev == null) prev = new float[gridWidth][gridHeight];
        
        for(int x = 0; x < gridWidth; ++x) {
            for(int y = 0; y < gridHeight; ++y) {
                float val = now[x][y];
                float repairAmount = prev[x][y] - val;
                val += (prev[x][y] == 0) ? 0 : repairAmount * 2;
                val = (float)Math.max(max * 0.1 * effectLevel, Math.min(val, max));
                ship.getArmorGrid().setArmorValue(x, y, val);
                prev[x][y] = val;

                IceUtils.showHealText(ship, IceUtils.getCellLocation(ship, x, y), repairAmount);
                
//                Global.getCombatEngine().addFloatingDamageText(
//                        IceUtils.getCellLocation(ship, x, y), repairAmount,
//                        ICEModPlugin.HEAL_TEXT_COLOR, ship, ship);
            }
        }

        ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux() * 0.9f);
        stats.getHullDamageTakenMult().modifyMult(id, 0);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHullDamageTakenMult().unmodify(id);
        prev = null;
        ((ShipAPI)stats.getEntity()).getFluxTracker().setCurrFlux(initialFlux);
        initialFlux = Float.MIN_VALUE;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) return new StatusData("damage repairs armor", false);
        
        return null;
    }
}