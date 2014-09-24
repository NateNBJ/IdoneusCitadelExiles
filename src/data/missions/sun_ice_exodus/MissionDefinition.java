package data.missions.sun_ice_exodus;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import java.util.Random;

public class MissionDefinition implements MissionDefinitionPlugin {

    static final String[] OBJECTIVE_TYPES = {
        "sensor_array", "nav_buoy", "comm_relay"
    };

    MissionDefinitionAPI api;
    FleetSide side;
    Random rand = new Random(0);

    void addShip(String variantID, int count) {
        for (int i = 0; i < count; ++i) {
            if (variantID.endsWith("_wing")) {
                api.addToFleet(side, variantID, FleetMemberType.FIGHTER_WING, false);
            } else {
                api.addToFleet(side, variantID, FleetMemberType.SHIP, false);
            }
        }
    }

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        this.api = api;

        side = FleetSide.ENEMY;
        api.initFleet(side, "ISS", FleetGoal.ATTACK, true, 5);
        api.setFleetTagline(side, "Isolationists");
        api.addToFleet(side, "sun_ice_abraxas_Standard", FleetMemberType.SHIP, true);

        addShip("sun_ice_voidreaver_Standard", 1);
        addShip("sun_ice_kelpie_Standard", 1);
        addShip("sun_ice_eidolon_Standard", 1);

        addShip("sun_ice_soulbane_Standard", 2);
//        addShip("sun_ice_shiekwraith_Standard", 2);

        addShip("sun_ice_nightseer_Standard", 1);
        addShip("sun_ice_athame_Standard", 3);
        addShip("sun_ice_pentagram_Standard", 3);
//        addShip("sun_ice_specter_Standard", 2);
        addShip("sun_ice_flashghast_Standard", 2);
//        addShip("sun_ice_seraph_Standard", 3);
//        addShip("sun_ice_palantir_Standard", 3);

        addShip("sun_ice_stormwhisp_wing", 3);
        addShip("sun_ice_umbra_wing", 3);
//        addShip("sun_ice_poltergeist_wing", 3);
//        addShip("sun_ice_phantom_wing", 3);
        

        side = FleetSide.PLAYER;
        api.initFleet(side, "ICS", FleetGoal.ESCAPE, false, 5);
        api.setFleetTagline(side, "Exiles");
        api.addToFleet(side, "sun_ice_apocrypha_Standard", FleetMemberType.SHIP, true);
        api.addToFleet(side, "sun_ice_shalom_Standard", FleetMemberType.SHIP, "Progeny", false);
        api.defeatOnShipLoss("Progeny");
        
        addShip("sun_ice_voidreaver_Standard", 1);
//        addShip("sun_ice_kelpie_Standard", 1);
        addShip("sun_ice_eidolon_Standard", 2);

        addShip("sun_ice_soulbane_Standard", 1);
        addShip("sun_ice_shiekwraith_Standard", 1);

        addShip("sun_ice_nightseer_Standard", 1);
        addShip("sun_ice_athame_Standard", 1);
//        addShip("sun_ice_pentagram_Standard", 3);
        addShip("sun_ice_specter_Standard", 2);
        addShip("sun_ice_flashghast_Standard", 1);
        addShip("sun_ice_seraph_Standard", 1);
        addShip("sun_ice_palantir_Standard", 1);

        addShip("sun_ice_stormwhisp_wing", 1);
        addShip("sun_ice_umbra_wing", 2);
        addShip("sun_ice_poltergeist_wing", 1);
        addShip("sun_ice_phantom_wing", 1);

        
        
        
        api.addPlugin(new MissionScript());
        
        float width = 24000f;
        float height = 48000f;
        api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;

        api.addNebula(minX + width * 0.66f, minY + height * 0.5f, 2000);
        api.addNebula(minX + width * 0.25f, minY + height * 0.6f, 1000);
        api.addNebula(minX + width * 0.25f, minY + height * 0.4f, 1000);

        for (int i = 0; i < 5; i++) {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = 100f + (float) Math.random() * 400f;
            api.addNebula(x, y, radius);
        }
        
        api.addObjective(minX + width * 0.25f + 2000f, minY + height * 0.5f, "sensor_array");
        api.addObjective(minX + width * 0.75f - 2000f, minY + height * 0.5f, "comm_relay");
        api.addObjective(minX + width * 0.33f + 2000f, minY + height * 0.4f, "nav_buoy");
        api.addObjective(minX + width * 0.66f - 2000f, minY + height * 0.6f, "nav_buoy");

        api.addAsteroidField(-(minY + height), minY + height, -45, 2000f, 20f, 70f, 100);
        
        api.addBriefingItem("TODO");
    }
}
