package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class NutritionResynth extends BaseHullMod {
    //long dayOfLastSupplyRefund = Global.getSector().getClock().getDay();

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
//        float pspd = member.getFleetData().getFleet().getLogistics().getPersonnelSuppliesPerDay();
//        long day = Global.getSector().getClock().getDay();
        
        CampaignFleetAPI fleet = member.getFleetData().getFleet();
        
        if(fleet == null || fleet.getLogistics() == null) return;
        
        float crewCost = fleet.getLogistics().getPersonnelSuppliesPerDay();
        fleet.getCargo().addSupplies(crewCost / 0.5f * (amount / Global.getSector().getClock().getSecondsPerDay()));
        
//        while(dayOfLastSupplyRefund < Global.getSector().getClock().getDay()) {
//            CampaignFleetAPI fleet = member.getFleetData().getFleet();
//            float crewCost = fleet.getLogistics().getPersonnelSuppliesPerDay();
//            fleet.getCargo().addSupplies(crewCost / 2f);
//            ++dayOfLastSupplyRefund;
//        }
    }
    
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}