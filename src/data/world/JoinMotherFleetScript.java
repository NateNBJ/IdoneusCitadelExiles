package data.world;

import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class JoinMotherFleetScript implements Script {
    CampaignFleetAPI motherFleet, childFleet;
    
    public JoinMotherFleetScript(CampaignFleetAPI motherFleet, CampaignFleetAPI childFleet) {
        this.motherFleet = motherFleet;
        this.childFleet = childFleet;
    }
    
    @Override
    public void run() {
        if(motherFleet == null || childFleet == null || !motherFleet.isAlive()) return;
        
        motherFleet.getCargo().addSupplies(childFleet.getCargo().getSupplies());
        motherFleet.getCargo().addFuel(childFleet.getCargo().getFuel());
        motherFleet.getCargo().addMarines(childFleet.getCargo().getMarines());
        motherFleet.getCargo().addCrew(CrewXPLevel.GREEN, childFleet.getCargo().getCrew(CrewXPLevel.GREEN));
        motherFleet.getCargo().addCrew(CrewXPLevel.REGULAR, childFleet.getCargo().getCrew(CrewXPLevel.REGULAR));
        motherFleet.getCargo().addCrew(CrewXPLevel.VETERAN, childFleet.getCargo().getCrew(CrewXPLevel.VETERAN));
        motherFleet.getCargo().addCrew(CrewXPLevel.ELITE, childFleet.getCargo().getCrew(CrewXPLevel.ELITE));
        
        for(FleetMemberAPI m : childFleet.getFleetData().getMembersListCopy()) {
            motherFleet.getFleetData().addFleetMember(m);
        }
        
        motherFleet.updateCounts();
        motherFleet.getFleetData().sort();
        
        childFleet.despawn(FleetDespawnReason.REACHED_DESTINATION, null);
    }
}
