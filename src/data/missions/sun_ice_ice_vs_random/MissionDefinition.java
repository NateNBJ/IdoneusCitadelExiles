package data.missions.sun_ice_ice_vs_random;

import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.missions.BaseRandomMissionDefinition;

public class MissionDefinition extends BaseRandomMissionDefinition {    
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        chooseFactions("sun_ice", null);
        super.defineMission(api);
    }
}