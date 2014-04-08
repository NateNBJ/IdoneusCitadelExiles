package data.missions.vanilla_vs_ice;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;

public class MissionDefinition implements MissionDefinitionPlugin {

    List ice = new ArrayList();
    List vanilla = new ArrayList();
    Random rand = new Random();
	List ships;
    String[] flagshipChoices;

    static final String[] OBJECTIVE_TYPES = {
        "sensor_array", "nav_buoy", "comm_relay"
    };
    
	void addShip(String variant, int weight) {
		for (int i = 0; i < weight; i++) {
			ships.add(variant);
		}
	}

	int generateFleet(int maxFP, FleetSide side, List ships, MissionDefinitionAPI api) {
		int currFP = 0;

		if (side == FleetSide.PLAYER) {
			String flagship = flagshipChoices[(int) (Math.random() * (float) flagshipChoices.length)];
			api.addToFleet(side, flagship, FleetMemberType.SHIP, true);
			currFP += api.getFleetPointCost(flagship);
		}
        
        TreeMap fpMap = new TreeMap();

		while (true) {
			int index = (int)(Math.random() * ships.size());
			String id = (String) ships.get(index);
            int fp = api.getFleetPointCost(id);
			currFP += fp;

			if (currFP > maxFP) {
                currFP -= fp;
                break;
            }

            if(!fpMap.containsKey(api.getFleetPointCost(id)))
                fpMap.put(fp, new HashMap());

            HashMap ids = (HashMap)fpMap.get(fp);

            if(!ids.containsKey(id)) ids.put(id, 0);

            ids.put(id, ((Integer)ids.get(id)) + 1);
		}

        for(Iterator fpIter = fpMap.descendingKeySet().iterator(); fpIter.hasNext();) {
            HashMap idMap = (HashMap)fpMap.get(fpIter.next());

            for(Iterator idIter = idMap.keySet().iterator(); idIter.hasNext();) {
                String variantID = (String)idIter.next();
                int count = (Integer)idMap.get(variantID);

                for(int i = 0; i < count; ++i) {
                    if (variantID.endsWith("_wing")) {
                        api.addToFleet(side, variantID, FleetMemberType.FIGHTER_WING, false);
                    } else {
                        api.addToFleet(side, variantID, FleetMemberType.SHIP, false);
                    }
                }
            }
        }

        return currFP;
	}

    @Override
	public void defineMission(MissionDefinitionAPI api) {
        int factionPick = rand.nextInt(3);
        
        ships = vanilla;
        
        if(factionPick == 0) { // High Tech
            flagshipChoices = new String[] {
                "paragon_Elite",
                "doom_Strike",
                "hyperion_Strike"
            };
            addShip("doom_Strike", 3);
            addShip("shade_Assault", 7);
            addShip("afflictor_Strike", 7);
            addShip("hyperion_Attack", 2);
            addShip("hyperion_Strike", 2);
            addShip("astral_Elite", 3);
            addShip("paragon_Elite", 1);
            addShip("apogee_Balanced", 5);
            addShip("aurora_Balanced", 5);
            addShip("aurora_Balanced", 5);
            addShip("odyssey_Balanced", 2);
            addShip("medusa_Attack", 15);
            addShip("tempest_Attack", 15);
            addShip("wasp_wing", 10);
            addShip("xyphos_wing", 10);
            addShip("longbow_wing", 10);
            addShip("dagger_wing", 10);
            addShip("tempest_Attack", 2);
        } else if (factionPick == 1) { // Midline
            flagshipChoices = new String[] {
                "conquest_Elite",
                "eagle_Assault",
                "hammerhead_Elite"
            };
            addShip("conquest_Elite", 3);
            addShip("eagle_Assault", 5);
            addShip("falcon_Attack", 5);
            addShip("hammerhead_Balanced", 10);
            addShip("hammerhead_Elite", 5);
            addShip("sunder_CS", 10);
            addShip("gemini_Standard", 8);
            addShip("brawler_Assault", 15);
            addShip("wolf_CS", 2);
            addShip("vigilance_Standard", 10);
            addShip("vigilance_FS", 15);
            addShip("brawler_Assault", 10);
            addShip("thunder_wing", 5);
            addShip("gladius_wing", 15);
            addShip("warthog_wing", 5);
            addShip("broadsword_wing", 10);
        } else { // Low Tech
            flagshipChoices = new String[] {
                "onslaught_Elite",
                "dominator_Assault",
                "onslaught_Standard"
            };
            addShip("onslaught_Standard", 3);
            addShip("onslaught_Outdated", 3);
            addShip("onslaught_Elite", 1);
            addShip("dominator_Assault", 5);
            addShip("dominator_Support", 5);
            addShip("condor_Strike", 15);
            addShip("venture_Balanced", 5);
            addShip("condor_FS", 15);
            addShip("enforcer_Assault", 15);
            addShip("enforcer_CS", 15);
            addShip("buffalo2_FS", 20);
            addShip("lasher_CS", 20);
            addShip("lasher_Standard", 20);
            addShip("hound_Assault", 15);
            addShip("piranha_wing", 15);
            addShip("talon_wing", 20);
            addShip("mining_drone_wing", 10);
        }
        
        ships = ice;
		
        addShip("sun_ice_voidreaver_Standard", 1);
        addShip("sun_ice_eidolon_Standard", 2);
        addShip("sun_ice_soulbane_Standard", 3);
        addShip("sun_ice_specter_Standard", 5);
        addShip("sun_ice_flashghast_Standard", 4);
        addShip("sun_ice_poltergeist_wing", 3);
        addShip("sun_ice_phantom_wing", 3);
        addShip("sun_ice_athame_Standard", 1);
        addShip("sun_ice_nightseer_Standard", 1);
        addShip("sun_ice_pentagram_Standard", 3);
        addShip("sun_ice_kelpie_Standard", 1);

		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "ICS", FleetGoal.ATTACK, true, 5);

		api.setFleetTagline(FleetSide.PLAYER, "Your forces");
		api.setFleetTagline(FleetSide.ENEMY, "Enemy forces");

        int size = 30 + (int)((float) Math.random() * 170);

		int playerFP = generateFleet(size, FleetSide.PLAYER, vanilla, api);
		int enemyFP = generateFleet(size, FleetSide.ENEMY, ice, api);

		float width = 13000f + 13000f * (size / 200);
		float height = 13000f + 13000f * (size / 200);
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);

		float minX = -width/2;
		float minY = -height/2;

        float nebulaCount = 10 + (float)Math.random() * 30;
        float nebulaSize = (float)Math.random();

		for (int i = 0; i < nebulaCount; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = (400f + (float) Math.random() * 1600f) * nebulaSize;
			api.addNebula(x, y, radius);
		}

        int objectiveCount = (int)Math.floor(size / 35f);

        //api.addBriefingItem("Objectives:  " + objectiveCount);

        while(objectiveCount > 0) {
            String type = OBJECTIVE_TYPES[rand.nextInt(3)];

            if(objectiveCount == 1) {
                api.addObjective(0, 0, type);
                objectiveCount -= 1;
            } else {
                float theta = (float)(Math.random() * Math.PI);
                double radius = Math.min(width, height);
                radius = radius * 0.1 + radius * 0.3 * Math.random();
                int x = (int)(Math.cos(theta) * radius);
                int y = (int)(Math.sin(theta) * radius);
                api.addObjective(x, -y, type);
                api.addObjective(-x, y, type);
                objectiveCount -= 2;
            }
        }

        int minAsteroidSpeed = (int)(Math.pow(Math.random(), 3) * 300);
        int asteroidCount = size + (int)(size * 4 * Math.pow(Math.random(), 2));

		api.addAsteroidField(
                minX + width * 0.5f, // X
                minY + height * 0.5f, // Y
                rand.nextInt(90) - 45 + (rand.nextInt() % 2) * 180, // Angle
                100 + (int)(Math.random() * height / 2), // Width
                minAsteroidSpeed, // Min speed
                minAsteroidSpeed * 1.1f, // Max speed
                asteroidCount); // Count

		String [] planets = {"barren", "terran", "gas_giant", "ice_giant", "cryovolcanic", "frozen", "jungle", "desert", "arid"};
		String planet = planets[(int) (Math.random() * (double) planets.length)];
		float radius = 100f + (float) Math.random() * 150f;
		api.addPlanet(0, 0, radius, planet, 200f, true);

        api.addBriefingItem("Nebulosity:  " + (int)(((nebulaCount * nebulaSize) / 40f) * 100) + "%");
        api.addBriefingItem("Asteroid Density:  " + (int)((asteroidCount / 1000f) * 100) + "%");
        api.addBriefingItem("Asteroid Speed:  " + minAsteroidSpeed);
        api.addBriefingItem("Deployment Point Ratio:  (You " + playerFP + " : " + enemyFP + " Them)");
	}
}