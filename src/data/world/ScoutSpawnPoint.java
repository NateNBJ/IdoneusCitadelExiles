package data.world;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import java.util.Random;

@SuppressWarnings("unchecked")
public class ScoutSpawnPoint extends BaseSpawnPoint {
    static final Random rand = new Random();
    static final String[] types = new String[] {
        "duelist",
        "recon",
        "foragers"
    };

	public ScoutSpawnPoint(SectorAPI sector, LocationAPI location,
            float daysInterval, int maxFleets, SectorEntityToken anchor) {

		super(sector, location, daysInterval, maxFleets, anchor);

        //this.destination = destination;
	}

	
	@Override
	public CampaignFleetAPI spawnFleet() {
        String type = types[rand.nextInt(types.length)];
		CampaignFleetAPI fleet = getSector().createFleet("sun_ice", type);
        fleet.setPreferredResupplyLocation(getAnchor());

		getLocation().spawnFleet(getAnchor(), 0, 0, fleet);

        fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, ((StarSystemAPI)getLocation()).getStar(), 15);
		fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, getAnchor(), 1000);

		return fleet;
	}
	
}






