package data.missions.sun_ice_sacrifice;

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
        api.setFleetTagline(FleetSide.ENEMY, "Idoneus Citadel Purger task force");
        api.addToFleet(FleetSide.ENEMY, "sun_ice_soulbane_Standard", FleetMemberType.SHIP, true);
        
        api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "sun_ice_athame_Standard", FleetMemberType.SHIP, false);
        
        api.addToFleet(FleetSide.ENEMY, "sun_ice_specter_Standard", FleetMemberType.SHIP, false);
        
        api.addToFleet(FleetSide.ENEMY, "sun_ice_seraph_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "sun_ice_seraph_Standard", FleetMemberType.SHIP, false);
        
        api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false, 0);
        api.setFleetTagline(FleetSide.PLAYER, "The ISS Willow Seed and heavy escort");
        api.addToFleet(FleetSide.PLAYER, "conquest_Elite", FleetMemberType.SHIP, "ISS Willow Seed", true, CrewXPLevel.ELITE);
        api.addToFleet(FleetSide.PLAYER, "hammerhead_Balanced", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "brawler_Elite", FleetMemberType.SHIP, false);
        
        api.defeatOnShipLoss("ISS Willow Seed");
        
        float width = 13000f;
        float height = 13000f;
        api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);
        
        api.setHyperspaceMode(true);
        
        api.addBriefingItem("Dissabling an Athame's weapon while it's charging will force it to overload");
        api.addBriefingItem("Shields may not help against Athames, but manueverability may");
        api.addBriefingItem("The ISS Willow Seed must survive");
    }
}
