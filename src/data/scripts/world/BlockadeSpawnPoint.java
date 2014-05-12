package data.scripts.world;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import data.scripts.world.BaseSpawnPoint;

@SuppressWarnings("unchecked")
public class BlockadeSpawnPoint extends BaseSpawnPoint {

	public BlockadeSpawnPoint(SectorAPI sector, LocationAPI location,
                float daysInterval, int maxFleets, SectorEntityToken anchor) {
        
		super(sector, location, daysInterval, maxFleets, anchor);
	}

	
	@Override
	public CampaignFleetAPI spawnFleet() {

        CampaignFleetAPI fleet = getSector().createFleet("sun_ici", "blockade");
        getAnchor().getContainingLocation().spawnFleet(getAnchor(), 0, 0, fleet);
		fleet.setPreferredResupplyLocation(getAnchor());

        fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, getAnchor(), 30);
		fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, getAnchor(), 1000);
		
		return fleet;
	}
	
}






