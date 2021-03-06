package data.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
        
        variants.put("brdy_imaginos", createVariantList(new String[]{"brdy_imaginos_elite", "brdy_imaginos_shock"}));
        variants.put("brdy_convergence", createVariantList(new String[]{"brdy_convergence_frontier", "brdy_convergence_standard"}));
        variants.put("brdy_asura", createVariantList(new String[]{"brdy_asura_strike", "brdy_asura_assault"}));
        variants.put("brdy_cetonia", createVariantList(new String[]{"brdy_cetonia_standard"}));
        variants.put("brdy_karkinos", createVariantList(new String[]{"brdy_karkinos_assault", "brdy_karkinos_prototype", "brdy_karkinos_suppression", "brdy_karkinos_elite", "brdy_karkinos_obliterator"}));
        variants.put("brdy_kurmaraja", createVariantList(new String[]{"brdy_kurmaraja_elite", "brdy_kurmaraja_heavy", "brdy_kurmaraja_suppression"}));
        variants.put("brdy_locust", createVariantList(new String[]{"brdy_locust_patrol", "brdy_locust_strike", "brdy_locust_wing", "brdy_locust_hunter"}));
        variants.put("brdy_mantis", createVariantList(new String[]{"brdy_mantis_elite", "brdy_mantis_attack", "brdy_mantis_strike", "brdy_mantis_barrage", "brdy_mantis_shatterer"}));
        variants.put("brdy_revenant", createVariantList(new String[]{"brdy_revenant_carrier", "brdy_revenant_mark1", "brdy_revenant_mark2", "brdy_revenant_modernized", "brdy_revenant_anticap"}));
        variants.put("brdy_robberfly", createVariantList(new String[]{"brdy_robberfly_cs", "brdy_robberfly_light", "brdy_robberfly_strike", "brdy_robberfly_barrage"}));
        variants.put("brdy_stenos", createVariantList(new String[]{"brdy_stenos_exploration", "brdy_stenos_militarized", "brdy_stenos_weap"}));
        variants.put("brdy_typheus", createVariantList(new String[]{"brdy_typheus_elite", "brdy_typheus_support", "brdy_typheus_defender"}));
        variants.put("brdy_gonodactylus", createVariantList(new String[]{"gonodactylus_assault", "gonodactylus_CS", "gonodactylus_elite", "gonodactylus_cc"}));
        variants.put("brdy_nevermore", createVariantList(new String[]{"nevermore_advanced", "nevermore_assault", "nevermore_tac", "nevermore_shock"}));
        variants.put("brdy_eschaton", createVariantList(new String[]{"brdy_eschaton_armed"}));
        variants.put("brdy_desdinova", createVariantList(new String[]{"desdinova_assault", "desdinova_cs", "desdinova_fastattack", "desdinova_HK"}));
        variants.put("brdy_scorpion", createVariantList(new String[]{"brdy_scorpion_adv", "brdy_scorpion_fs", "brdy_scorpion_standard"}));
        variants.put("brdy_scarab", createVariantList(new String[]{"scarab_closesupport", "scarab_firesupport", "scarab_pd", "scarab_strike", "scarab_attack", "scarab_hunter", "scarab_barrage", "scarab_hkineticwp"}));

        variants.put("ms_charybdis", createVariantList(new String[]{"ms_charybdis_Attack", "ms_charybdis_Balanced", "ms_charybdis_CS", "ms_charybdis_PD", "ms_charybdis_Standard"}));
        variants.put("ms_elysium", createVariantList(new String[]{"ms_elysium_Assault", "ms_elysium_CS", "ms_elysium_PD", "ms_elysium_Standard", "ms_elysium_Strike"}));
        variants.put("ms_enlil", createVariantList(new String[]{"ms_enlil_AF", "ms_enlil_Attack", "ms_enlil_Balanced", "ms_enlil_CS", "ms_enlil_LRM", "ms_enlil_PD", "ms_enlil_Standard", "ms_enlil_Strike"}));
        variants.put("ms_inanna", createVariantList(new String[]{"ms_inanna_Assault", "ms_inanna_CS", "ms_inanna_EMP", "ms_inanna_Standard", "ms_inanna_Strike"}));
        variants.put("ms_mimir", createVariantList(new String[]{"ms_mimir_Assault", "ms_mimir_CS", "ms_mimir_PD", "ms_mimir_Standard"}));
        variants.put("ms_morningstar", createVariantList(new String[]{"ms_morningstar_AF", "ms_morningstar_Assault", "ms_morningstar_CS", "ms_morningstar_PD", "ms_morningstar_Standard", "ms_morningstar_Strike"}));
        variants.put("ms_sargasso", createVariantList(new String[]{"ms_sargasso_Assault", "ms_sargasso_Balanced", "ms_sargasso_EMP", "ms_sargasso_LRM", "ms_sargasso_Standard"}));
        variants.put("ms_potnia", createVariantList(new String[]{"ms_potnia_Standard"}));
        variants.put("ms_potniaBis", createVariantList(new String[]{"ms_potniaBis_AS", "ms_potniaBis_Attack", "ms_potniaBis_CS", "ms_potniaBis_FS"}));
        variants.put("ms_seski", createVariantList(new String[]{"ms_seski_Attack", "ms_seski_BR", "ms_seski_CS", "ms_seski_Standard"}));
        variants.put("ms_shamash", createVariantList(new String[]{"ms_shamash_Attack", "ms_shamash_CS", "ms_shamash_EMP", "ms_shamash_Standard"}));
        variants.put("ms_tartarus", createVariantList(new String[]{"ms_tartarus_AF", "ms_tartarus_Assault", "ms_tartarus_CS", "ms_tartarus_Standard"}));
        variants.put("ms_scylla", createVariantList(new String[]{"ms_scylla_Assault", "ms_scylla_Beam", "ms_scylla_Standard"}));
        variants.put("ms_solidarity", createVariantList(new String[]{"ms_solidarity_Fast", "ms_solidarity_Standard"}));
        
        variants.put("tem_archbishop", createVariantList(new String[]{"tem_archbishop_sal", "tem_archbishop_mit", "tem_archbishop_man", "tem_archbishop_est", "tem_archbishop_def", "tem_archbishop_ati"}));
        variants.put("tem_paladin", createVariantList(new String[]{"tem_paladin_agi", "tem_paladin_sal", "tem_paladin_ati", "tem_paladin_capi", "tem_paladin_est", "tem_paladin_mit", "tem_paladin_reti"}));
        variants.put("tem_jesuit", createVariantList(new String[]{"tem_jesuit_ati", "tem_jesuit_capi", "tem_jesuit_def", "tem_jesuit_est", "tem_jesuit_reti", "tem_jesuit_sal"}));
        variants.put("tem_crusader", createVariantList(new String[]{"tem_crusader_agi", "tem_crusader_ati", "tem_crusader_capi", "tem_crusader_def", "tem_crusader_est", "tem_crusader_reti", "tem_crusader_sal"}));
        
        variants.put("ii_titan", createVariantList(new String[]{"ii_titan_pb"}));
        variants.put("ii_sebastos", createVariantList(new String[]{"ii_sebastos_ass", "ii_sebastos_bal", "ii_sebastos_eli", "ii_sebastos_sta", "ii_sebastos_art"}));
        variants.put("ii_princeps", createVariantList(new String[]{"ii_princeps_ass", "ii_princeps_def", "ii_princeps_eli", "ii_princeps_sta", "ii_princeps_art"}));
        variants.put("ii_praetorian", createVariantList(new String[]{"ii_praetorian_ass", "ii_praetorian_bal", "ii_praetorian_sta", "ii_praetorian_str", "ii_praetorian_sup", "ii_praetorian_art"}));
        variants.put("ii_mirv", createVariantList(new String[]{"ii_mirv_sb"}));
        variants.put("ii_olympus_t", createVariantList(new String[]{"ii_olympus_t_sta"}));
        variants.put("ii_olympus_a", createVariantList(new String[]{"ii_olympus_a_sta"}));
        variants.put("ii_maximus", createVariantList(new String[]{"ii_maximus_ass", "ii_maximus_att", "ii_maximus_bea", "ii_maximus_bul", "ii_maximus_def", "ii_maximus_shi", "ii_maximus_str", "ii_maximus_arm", "ii_maximus_art"}));
        variants.put("ii_jupiter", createVariantList(new String[]{"ii_jupiter_mob", "ii_jupiter_orb", "ii_jupiter_shi", "ii_jupiter_sie", "ii_jupiter_art", "ii_jupiter_ass"}));
        variants.put("ii_invictus", createVariantList(new String[]{"ii_invictus_ass", "ii_invictus_eli", "ii_invictus_sta", "ii_invictus_str", "ii_invictus_sup", "ii_invictus_arm", "ii_invictus_art"}));
        variants.put("ii_interrex", createVariantList(new String[]{"ii_interrex_bal", "ii_interrex_eli", "ii_interrex_sta", "ii_interrex_str", "ii_interrex_sup", "ii_interrex_ass"}));
        variants.put("ii_dominus", createVariantList(new String[]{"ii_dominus_eli", "ii_dominus_sie", "ii_dominus_sta", "ii_dominus_str", "ii_dominus_art", "ii_dominus_ass", "ii_dominus_bal"}));
        variants.put("ii_dictator", createVariantList(new String[]{"ii_dictator_eli", "ii_dictator_roc", "ii_dictator_sta", "ii_dictator_sup", "ii_dictator_art", "ii_dictator_ass", "ii_dictator_def"}));
        variants.put("ii_decurion", createVariantList(new String[]{"ii_decurion_roc", "ii_decurion_sni", "ii_decurion_sta", "ii_decurion_str", "ii_decurion_sup", "ii_decurion_art", "ii_decurion_ass"}));
        variants.put("ii_centurion", createVariantList(new String[]{"ii_centurion_def", "ii_centurion_eli", "ii_centurion_sta", "ii_centurion_art", "ii_centurion_ass", "ii_centurion_bal"}));
        variants.put("ii_caesar", createVariantList(new String[]{"ii_caesar_sie", "ii_caesar_sta", "ii_caesar_str", "ii_caesar_sup", "ii_caesar_bal", "ii_caesar_eli", "ii_caesar_for"}));
        variants.put("ii_basileus", createVariantList(new String[]{"ii_basileus_ass", "ii_basileus_def", "ii_basileus_sta", "ii_basileus_str", "ii_basileus_sup"}));
        variants.put("ii_basileus_f", createVariantList(new String[]{"ii_basileus_f_eli", "ii_basileus_f_mis", "ii_basileus_f_shi", "ii_basileus_f_sta", "ii_basileus_f_arm", "ii_basileus_f_art"}));
        variants.put("ii_auriga", createVariantList(new String[]{"ii_auriga_ass", "ii_auriga_eli", "ii_auriga_sta", "ii_auriga_arm", "ii_auriga_art"}));
    }

    static List<String> createVariantList(String[] variants) {
        return Collections.unmodifiableList(Arrays.asList(variants));
    }
    static String getRandomVariantID(String shipID) {
        return (String)variants.get(shipID).toArray()[rand.nextInt(variants.get(shipID).size())];
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
            //Global.getLogger(Faction.class).error("JSONException", e);
            all = null;
            return null;
        } catch (IOException e) {
            //Global.getLogger(Faction.class).error("IOException", e);
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
    float mostFP = 0;
    Set<String> warships = new HashSet();
    Set<String> fighters = new HashSet();
    Set<String> civilians = new HashSet();
    
    final void incorporateVariantID(String id) {
        if(id.equals("fallback")) return;
        
        FleetMemberType type = id.endsWith("_wing")
                ? FleetMemberType.FIGHTER_WING : FleetMemberType.SHIP;

        FleetMemberAPI member = Global.getFactory().createFleetMember(type, id);

        if(member.isFighterWing()) {
            fighters.add(id);
        } else if(member.getHullSpec().getHints().contains(ShipTypeHints.CIVILIAN)) {
            civilians.add(member.getHullId());
            addVariant(member.getHullId(), id);
        } else {
            warships.add(member.getHullId());
            addVariant(member.getHullId(), id);
        }

        if(member.getFleetPointCost() > mostFP) {
            mostFP = member.getFleetPointCost();
        } 
    }
    void addVariant(String shipID, String variantID) {
        if(variantID.endsWith("_Hull")) return;
        
        if(!variants.containsKey(shipID)) variants.put(shipID, new ArrayList());
        
        if(!variants.get(shipID).contains(variantID))
            variants.get(shipID).add(variantID);
    }
    
    public String getRandomWingID() {
        int index = (int) (Math.random() * fighters.size());
        
        return (String) fighters.toArray()[index];
    }
    public String getRandomWarshipID() {
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
        
        if(id.equals("templars")) {
            fighters.add("tem_teuton_fighter_wing");
            fighters.add("tem_teuton_assault_wing");
            fighters.add("tem_teuton_support_wing");
            fighters.add("tem_teuton_bomber_wing");
            
            warships.add("tem_jesuit");
            warships.add("tem_crusader");
            warships.add("tem_paladin");
            warships.add("tem_archbishop");
        } else if(id.equals("interstellarimperium")) {
            fighters.add("ii_flamen_wing");
            fighters.add("ii_draconarius_wing");
            fighters.add("ii_vicarius_b_wing");
            fighters.add("ii_vicarius_a_wing");
            fighters.add("ii_vicarius_s_wing");
            fighters.add("ii_scutum_wing");
            
            warships.add("ii_basileus");
            warships.add("ii_basileus_f");
            warships.add("ii_decurion");
            warships.add("ii_invictus");
            warships.add("ii_maximus");
            warships.add("ii_auriga");
            warships.add("ii_praetorian");
            warships.add("ii_interrex");
            warships.add("ii_centurion");
            warships.add("ii_princeps");
            warships.add("ii_sebastos");
            warships.add("ii_dictator");
            warships.add("ii_jupiter");
            warships.add("ii_dominus");
            warships.add("ii_caesar");
            warships.add("ii_olympus_t");
            warships.add("ii_olympus_a");
            warships.add("ii_mirv");
            warships.add("ii_titan");
        }
        
        
        if(json.has("shipRoles")) {
            for(JSONObject roles : getFleets(json.getJSONObject("shipRoles"))) {
                for(String variant : getKeys(roles)) {
                    incorporateVariantID(variant);
                }
            }   
        } else if(json.has("fleetCompositions")) {
            for(JSONObject fleet : getFleets(json.getJSONObject("fleetCompositions"))) {
                for(String variant : getKeys(fleet.getJSONObject("ships"))) {
                    incorporateVariantID(variant);
                }
            }
        }
        
        
        
        if(warships.isEmpty() || fighters.isEmpty()) return;
        
        for(String hullID : warships) {
            if(!variants.containsKey(hullID)) return;
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

        while (currFP < maxFP) {
            try {
                String variantID;

                if(wingsToAdd > 0 && !fighters.isEmpty()) {
                    variantID = getRandomWingID();
                    --wingsToAdd;
                } else {
                    variantID = getRandomWarshipID();
                    float decks = Global.getFactory().createFleetMember(
                                FleetMemberType.SHIP, variantID).getNumFlightDecks();
                    wingsToAdd += decks * 2.3f;
                }


                int fp = api.getFleetPointCost(variantID);

                if(wingsToAdd <= 0 && (fp + 3) / (mostFP * 1.4f + 3) > rand.nextFloat())
                    continue;

                currFP += fp;

                if (!fpMap.containsKey(api.getFleetPointCost(variantID))) {
                    fpMap.put(fp, new HashMap());
                }

                HashMap ids = (HashMap) fpMap.get(fp);

                if (!ids.containsKey(variantID)) {
                    ids.put(variantID, 0);
                }

                ids.put(variantID, ((Integer) ids.get(variantID)) + 1);
            } catch (Exception e) {
                
            }
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
                }
            }
        }

        return currFP;
    }
}
