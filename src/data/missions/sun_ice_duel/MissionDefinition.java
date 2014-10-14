package data.missions.sun_ice_duel;

import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        api.initFleet(FleetSide.ENEMY, "ICS", FleetGoal.ATTACK, true, 0);
        api.setFleetTagline(FleetSide.ENEMY, "The Flagship of an Idoneus Isolationist blockade fleet");
        api.addToFleet(FleetSide.ENEMY, "sun_ice_abraxas_Standard", FleetMemberType.SHIP, "ICS Samuel", true, CrewXPLevel.VETERAN);
        
        api.initFleet(FleetSide.PLAYER, "ICS", FleetGoal.ATTACK, false, 0);
        api.setFleetTagline(FleetSide.PLAYER, "The ICS Tiny Dancer");
        api.addToFleet(FleetSide.PLAYER, "sun_ice_nightseer_Standard", FleetMemberType.SHIP, "ICS Tiny Dancer", true, CrewXPLevel.ELITE);
        
        float width = 10000f;
        float height = 10000f;
        api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);
        
        api.addBriefingItem("The enemy ship has a weak point near the center where the armor doesn't reconstruct");
        api.addBriefingItem("The weak point can be hit from behind with careful aim");
        api.addBriefingItem("Make the most of the few minutes before your CR timer runs out");
    }
}
