package data.missions.sun_ice_random_vs_ice;

import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.missions.BaseRandomMissionDefinition;

public class MissionDefinition extends BaseRandomMissionDefinition {    
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        chooseFactions(null, "sun_ice");
        super.defineMission(api);
    }
}