package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.events.CampaignEventTarget;

public class RefugeeSpawnPoint extends BaseSpawnPoint {

    public RefugeeSpawnPoint(SectorAPI sector, LocationAPI location,
            float daysInterval, int maxFleets, SectorEntityToken anchor) {

        super(sector, location, daysInterval, maxFleets, anchor);
    }
    @Override
    public CampaignFleetAPI spawnFleet() {
        CampaignFleetAPI fleet = getSector().createFleet("sun_ice", "refugees");
        getLocation().spawnFleet(getAnchor(), 0, 0, fleet);
        

        Data.ExileFleet = fleet;
//        Data.ExileMarket.getConnectedEntities().clear();
//        Data.ExileMarket.setPrimaryEntity(fleet);
//        fleet.setMarket(Data.ExileMarket);
//        fleet.setInteractionImage("illustrations", "cargo_loading");
        
//        Global.getSector().getEventManager().startEvent(
//                new CampaignEventTarget(fleet), "sun_ice_exodus", null);
        
        return fleet;
    }

}
