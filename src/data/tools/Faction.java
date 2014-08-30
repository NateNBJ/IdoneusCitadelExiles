package data.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Faction {    
    static final Random rand = new Random();
    static final Set<String> EXCLUDED_FACTIONS = new HashSet();
    static Map<String, Faction> all = null;
    static Map<String, List<String>> variants = new HashMap();
    static {
        EXCLUDED_FACTIONS.add("data/world/factions/player.faction");
        EXCLUDED_FACTIONS.add("data/world/factions/neutral.faction");
        EXCLUDED_FACTIONS.add("data/world/factions/ici.faction");
        EXCLUDED_FACTIONS.add("data/world/factions/pirateAnar.faction");
    }

    static String getRandomVariantID(String shipID) {
        return (String)variants.get(shipID).toArray()[rand.nextInt(variants.get(shipID).size())];
    }
    static void addVariant(String shipID, String variantID) {
        if(!variants.containsKey(shipID)) variants.put(shipID, new ArrayList());
        
        if(!variants.get(shipID).contains(variantID))
            variants.get(shipID).add(variantID);
    }
    static List<String> getKeys(JSONObject json) {
        List<String> retVal = new LinkedList();
        
        Iterator<?> keys = json.keys();
        while( keys.hasNext()) retVal.add((String)keys.next());
        
        return retVal;
    }
    static List<JSONObject> getFleets(JSONObject json) throws JSONException {
        List<JSONObject> retVal = new LinkedList();
        
        Iterator<?> keys = json.keys();
        while( keys.hasNext())
            retVal.add(json.getJSONObject((String)keys.next()));
        
        return retVal;
    }
    
    public static Collection<Faction> getAllFactions() {
        if(all != null) return all.values();
        
        if(Global.getFactory() == null) {
            all = null;
            return null;
        }
        
        try {
            all = new HashMap();

            JSONArray arr = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "faction", "data/world/factions/factions.csv", "starsector-core");

            for(int i = 0; i < arr.length(); ++i) {
                JSONObject s = arr.getJSONObject(i);
                String path = s.getString("faction");
                if(path.equals("") || EXCLUDED_FACTIONS.contains(path)) continue;
                //all.add(new Faction(path));
                Faction faction = new Faction(path);
                if(faction.isValid) all.put(faction.id, faction);
            }
        } catch (JSONException e) {
            Global.getLogger(Faction.class).error("JSONException", e);
            all = null;
            return null;
        } catch (IOException e) {
            Global.getLogger(Faction.class).error("IOException", e);
            all = null;
            return null;
        }
        
        return all.values();
    }
    public static Faction getFactionById(String id) {
        getAllFactions();
        return all != null && all.containsKey(id) ? all.get(id) : null;
    }
    
    String id, displayName, shipNamePrefix, description;
    boolean isValid = false;
    Set<String> warships = new HashSet();
    Set<String> fighters = new HashSet();
    Set<String> civilians = new HashSet();
    
    String getRandomWing() {
        int index = (int) (Math.random() * fighters.size());
        
        return (String) fighters.toArray()[index];
    }
    String getRandomWarship() {
        int index = (int) (Math.random() * warships.size());
        return getRandomVariantID((String) warships.toArray()[index]);
    }
    
    Faction(String path) throws JSONException, IOException {
        JSONObject json = Global.getSettings().loadJSON(path);
        
        try { id = json.getString("id"); }
        catch (JSONException e) { return; }
        
        try { displayName = json.getString("displayName"); }
        catch (JSONException e) { displayName = ""; }
        
        try { shipNamePrefix = json.getString("shipNamePrefix"); }
        catch (JSONException e) { shipNamePrefix = ""; }
        
        try { description = json.getString("description"); }
        catch (JSONException e) { description = ""; }
        
        if(!json.has("fleetCompositions")) { return; }
        
        for(JSONObject fleet : getFleets(json.getJSONObject("fleetCompositions"))) {
            for(String variant : getKeys(fleet.getJSONObject("ships"))) {
                FleetMemberType type = variant.endsWith("_wing")
                        ? FleetMemberType.FIGHTER_WING : FleetMemberType.SHIP;
                
                FleetMemberAPI member = Global.getFactory().createFleetMember(type, variant);
                
                if(member.isFighterWing()) {
                    fighters.add(variant);
                } else if(member.getHullSpec().getHints().contains(ShipTypeHints.CIVILIAN)) {
                    civilians.add(member.getHullId());
                    addVariant(member.getHullId(), variant);
                } else {
                    warships.add(member.getHullId());
                    addVariant(member.getHullId(), variant);
                }
            }
        }
        
        isValid = true;
    }
    public String getId() {
        return id;
    }
    public String getDisplayName() {
        return displayName;
    }
    public String getShipNamePrefix() {
        return shipNamePrefix;
    }
    public String getDescription() {
        return description;
    }
    public Set<String> getPreferedWarships() {
        return warships;
    }
    public Set<String> getPreferedFighters() {
        return fighters;
    }
    public Set<String> getPreferedCivilianShips() {
        return civilians;
    }
    public Set<String> getPreferedShips() {
        Set<String> ships = new HashSet();
        ships.addAll(warships);
        ships.addAll(fighters);
        ships.addAll(civilians);
        return ships;
    }
    public int generateFleet(MissionDefinitionAPI api, FleetSide side, int maxFP) {
        int currFP = 0;
        boolean needChooseFlagship = true;
        TreeMap fpMap = new TreeMap();
        float wingsToAdd = 0;

        while (true) {
            String variantID;
            
            if(wingsToAdd > 0 && !fighters.isEmpty()) {
                variantID = getRandomWing();
                --wingsToAdd;
            } else {
                variantID = getRandomWarship();
                float decks = Global.getFactory().createFleetMember(
                            FleetMemberType.SHIP, variantID).getNumFlightDecks();
                wingsToAdd += decks * 2.3f;
            }
            
            
            int fp = api.getFleetPointCost(variantID);
            
            if(wingsToAdd <= 0 && (Math.min(30, fp)+3) / (rand.nextInt(35)+3) > rand.nextFloat())
                continue;
            
            currFP += fp;

            if (currFP > maxFP) {
                currFP -= fp;
                break;
            }

            if (!fpMap.containsKey(api.getFleetPointCost(variantID))) {
                fpMap.put(fp, new HashMap());
            }

            HashMap ids = (HashMap) fpMap.get(fp);

            if (!ids.containsKey(variantID)) {
                ids.put(variantID, 0);
            }

            ids.put(variantID, ((Integer) ids.get(variantID)) + 1);
        }

        for (Iterator fpIter = fpMap.descendingKeySet().iterator(); fpIter.hasNext();) {
            HashMap idMap = (HashMap) fpMap.get(fpIter.next());

            for (Iterator idIter = idMap.keySet().iterator(); idIter.hasNext();) {
                String variantID = (String)idIter.next();
                int count = (Integer) idMap.get(variantID);
                FleetMemberType type = variantID.endsWith("_wing")
                        ? FleetMemberType.FIGHTER_WING : FleetMemberType.SHIP;

                for (int i = 0; i < count; ++i) {
                    api.addToFleet(side, variantID, type, needChooseFlagship);
                    if(type == FleetMemberType.SHIP) needChooseFlagship = false;
                    
//                    float decks = Global.getFactory().createFleetMember(
//                            FleetMemberType.SHIP, variantID).getNumFlightDecks();
//                    
//                    decks *= rand.nextFloat() + 2;
//                    for(int j = 0; j < decks; ++j) {
//                        api.addToFleet(side, getRandomWing(),
//                                FleetMemberType.FIGHTER_WING, false);
//                    }
                }
            }
        }

        return currFP;
    }
}
