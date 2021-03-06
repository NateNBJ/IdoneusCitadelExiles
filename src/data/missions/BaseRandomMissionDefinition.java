package data.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BaseRandomMissionDefinition implements MissionDefinitionPlugin {
    static final String[] OBJECTIVE_TYPES = {
        "sensor_array", "nav_buoy", "comm_relay"
    };
    
    protected static final Map<String, Float> QUALITY_FACTORS = new HashMap<>(16);
    protected static final float QUALITY_FACTOR_DEFAULT = 0.7f;
    protected Random rand = new Random();
    protected boolean flagshipChosen = false;
    protected FactionAPI player, enemy;
    
    static
    {
        QUALITY_FACTORS.put(Factions.DIKTAT, 0.5f);
        QUALITY_FACTORS.put(Factions.HEGEMONY, 0.5f);
        QUALITY_FACTORS.put(Factions.INDEPENDENT, 0.5f);
        QUALITY_FACTORS.put(Factions.KOL, 0.5f);
        QUALITY_FACTORS.put(Factions.LIONS_GUARD, 0.75f);
        QUALITY_FACTORS.put(Factions.LUDDIC_CHURCH, 0.25f);
        QUALITY_FACTORS.put(Factions.LUDDIC_PATH, 0f);
        QUALITY_FACTORS.put(Factions.PIRATES, 0f);
        QUALITY_FACTORS.put(Factions.TRITACHYON, 0.85f);
        QUALITY_FACTORS.put("exigency", 1f);
        QUALITY_FACTORS.put("exipirated", 0.6f);
        QUALITY_FACTORS.put("tiandong", 0.55f);
        QUALITY_FACTORS.put("sun_ice", 0.85f);
        QUALITY_FACTORS.put("sun_ici", 0.75f);
        QUALITY_FACTORS.put("spire", 0.8f);
        QUALITY_FACTORS.put("darkspire", 0.8f);
    }
    
    protected void chooseFactions(String playerFactionId, String enemyFactionId) {
        player = Global.getSector().getFaction(playerFactionId);
        enemy = Global.getSector().getFaction(enemyFactionId);
                
        List<FactionAPI> acceptableFactions = new ArrayList();
        for(FactionAPI faction : Global.getSector().getAllFactions()) {
            if(!faction.isNeutralFaction() && !faction.isPlayerFaction()) {
                String id = faction.getId();
                if (id.equals("player_npc")) continue;
                if (id.equals("merc_hostile")) continue;
                if (id.equals("famous_bounty")) continue;
                acceptableFactions.add(faction);
            }
        }
                
        player = player != null ? player :
                acceptableFactions.get(rand.nextInt(acceptableFactions.size()));
        
        acceptableFactions.remove(player);
        
        enemy = enemy != null ? enemy :
                acceptableFactions.get(rand.nextInt(acceptableFactions.size()));
    }
    int generateFleet(FactionAPI faction, MissionDefinitionAPI api, FleetSide side, int minFP) {
        float qf = QUALITY_FACTOR_DEFAULT;
        if (QUALITY_FACTORS.containsKey(player.getId()))
            qf = QUALITY_FACTORS.get(player.getId());
        
        CampaignFleetAPI fleet = FleetFactory.createGenericFleet(faction.getId(), side.toString(), qf, minFP);
        fleet.getFleetData().sort();
        
        for(FleetMemberAPI m : fleet.getFleetData().getMembersListCopy()) {
            String variant = m.isFighterWing()
                    ? m.getSpecId()
                    : m.getVariant().getHullVariantId();
            api.addToFleet(side, variant, m.getType(), m.getShipName(),
                    (!m.isFighterWing() && !flagshipChosen), m.getCrewXPLevel());
                    
            if(!m.isFighterWing() && !flagshipChosen) flagshipChosen = true;
        }
        
        return fleet.getFleetPoints();
    }
    
    @Override
    public void defineMission(MissionDefinitionAPI api) {
        if(player == null || enemy == null) chooseFactions(null, null);
        
        api.initFleet(FleetSide.PLAYER, "ICS", FleetGoal.ATTACK, false, 5);
        api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true, 5);

        api.setFleetTagline(FleetSide.PLAYER, "Your forces");
        api.setFleetTagline(FleetSide.ENEMY, "Enemy forces");

        int size = 30 + (int)((float) Math.random() * 170);
        float difficulty = 0.7f + rand.nextFloat() * 0.3f;
        
        int playerFP = generateFleet(player, api, FleetSide.PLAYER, (int)(size * difficulty));
        int enemyFP = generateFleet(enemy, api, FleetSide.ENEMY, size);

        float width = 10000f + 10000f * (size / 100);
        float height = 10000f + 10000f * (size / 100);
        api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;

        float nebulaCount = 10 + (float) Math.random() * 30;
        float nebulaSize = (float) Math.random();

        for (int i = 0; i < nebulaCount; i++) {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = (400f + (float) Math.random() * 1600f) * nebulaSize;
            api.addNebula(x, y, radius);
        }

        int objectiveCount = (int) Math.floor(size / 35f);

        while (objectiveCount > 0) {
            String type = OBJECTIVE_TYPES[rand.nextInt(3)];

            if (objectiveCount == 1) {
                api.addObjective(0, 0, type);
                objectiveCount -= 1;
            } else {
                float theta = (float) (Math.random() * Math.PI);
                double radius = Math.min(width, height);
                radius = radius * 0.1 + radius * 0.3 * Math.random();
                int x = (int) (Math.cos(theta) * radius);
                int y = (int) (Math.sin(theta) * radius);
                api.addObjective(x, -y, type);
                api.addObjective(-x, y, type);
                objectiveCount -= 2;
            }
        }

        int minAsteroidSpeed = (int) (Math.pow(Math.random(), 3) * 300);
        int asteroidCount = size + (int) (size * 4 * Math.pow(Math.random(), 2));

        api.addAsteroidField(
                minX + width * 0.5f, // X
                minY + height * 0.5f, // Y
                rand.nextInt(90) - 45 + (rand.nextInt() % 2) * 180, // Angle
                100 + (int) (Math.random() * height / 2), // Width
                minAsteroidSpeed, // Min speed
                minAsteroidSpeed * 1.1f, // Max speed
                asteroidCount); // Count

        String[] planets = {"barren", "terran", "gas_giant", "ice_giant", "cryovolcanic", "frozen", "jungle", "desert", "arid"};
        String planet = planets[(int) (Math.random() * (double) planets.length)];
        float radius = 100f + (float) Math.random() * 150f;
        api.addPlanet(0, 0, radius, planet, 200f, true);

        api.addBriefingItem(player.getDisplayName() + "  (" + playerFP + ")   vs.  " + enemy.getDisplayName() + "  (" + enemyFP + ")");
        api.addBriefingItem("Nebulosity:  " + (int) (((nebulaCount * nebulaSize) / 40f) * 100) + "%");
        api.addBriefingItem("Asteroid Density:  " + (int) ((asteroidCount / 1000f) * 100) + "%");
        api.addBriefingItem("Asteroid Speed:  " + minAsteroidSpeed);
    }
}
