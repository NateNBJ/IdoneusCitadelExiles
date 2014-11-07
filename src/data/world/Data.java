package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import java.util.Map;

public class Data {
    public final static String PREFIX = "sun_ice_";
    
    public static SectorEntityToken
            IdoneusCitadel,
            PhantomEntity;
    
    public static MarketAPI
            ExileMarket,
            CitadelMarket;
    
    public static CampaignFleetAPI
            ExileFleet;
    
    public static FactionAPI
            Exiles,
            Garrison;
    
    public static Boolean
            SendTradeOffers = true;
    
    static Map<String, Object> map;
    
    public static void load() {
        map = Global.getSector().getPersistentData();
        
        IdoneusCitadel = getEntity("IdoneusCitadel");
        PhantomEntity = getEntity("PhantomEntity");
        
        ExileMarket = getMarket("ExileMarket");
        CitadelMarket = getMarket("CitadelMarket");
        
        ExileFleet = getFleet("ExileFleet");
        
        Exiles = getFaction("Exiles");
        Garrison = getFaction("Garrison");
        
        SendTradeOffers = getBool("SendTradeOffers", true);
    }
    public static void save() {
        map = Global.getSector().getPersistentData();
        
        put("IdoneusCitadel", IdoneusCitadel);
        put("PhantomEntity", PhantomEntity);
        
        put("ExileMarket", ExileMarket);
        put("CitadelMarket", CitadelMarket);
        
        put("ExileFleet", ExileFleet);
        
        put("Exiles", Exiles);
        put("Garrison", Garrison);
        
        put("SendTradeOffers", SendTradeOffers);
    }
    static CampaignFleetAPI getFleet(String key) {
        return (CampaignFleetAPI)Global.getSector().getEntityById(getString(key));
    }
    static SectorEntityToken getEntity(String key) {
        return Global.getSector().getEntityById(getString(key));
    }
    static FactionAPI getFaction(String key) {
        return Global.getSector().getFaction(getString(key));
    }
    static MarketAPI getMarket(String key) {
        return Global.getSector().getEconomy().getMarket(getString(key));
    }
    static String getString(String key) {
        key = PREFIX + key;
        return map.containsKey(key) ? (String)map.get(key) : "";
    } 
    static Boolean getBool(String key, Boolean defaultValue) {
        key = PREFIX + key;
        if(map.containsKey(key)) return (Boolean)map.get(key);
        else return defaultValue;
    }

    static void put(String key, FactionAPI faction) {
        put(key, faction == null ? "" : faction.getId());
    }
    static void put(String key, MarketAPI market) {
        put(key, market == null ? "" : market.getId());
    }
    static void put(String key, SectorEntityToken entity) {
        put(key, entity == null ? "" : entity.getId());
    }
    static void put(String key, String id) {
        map.put(PREFIX + key, id);
    }
    static void put(String key, boolean bool) {
        map.put(PREFIX + key, bool);
    }
    static void put(String key, float num) {
        map.put(PREFIX + key, num);
    }
    static void put(String key, int num) {
        map.put(PREFIX + key, num);
    }
}
