/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import java.util.Map;

/**
 *
 * @author Nate
 */
public class Data {
    public final static String PREFIX = "sun_ice_";
    
    public static SectorEntityToken
            IdoneusCitadel,
            PhantomEntity;
    
    public static MarketAPI
            ExileMarket;
    
    public static StarSystemAPI
            Ulterius;
    
    public static CampaignFleetAPI
            ExileFleet;
    
    public static Boolean
            SendTradeOffers;
    
    static Map<String, Object> map;
    
    static boolean isLoaded = false;
    public static void load() {
        if(isLoaded) return;
        
        map = Global.getSector().getPersistentData();
        
        IdoneusCitadel = (SectorEntityToken)get("IdoneusCitadel");
        PhantomEntity = (SectorEntityToken)get("PhantomEntity");
        ExileMarket = (MarketAPI)get("ExileMarket");
        Ulterius = (StarSystemAPI)get("Ulterius");
        ExileFleet = (CampaignFleetAPI)get("ExileFleet");
        SendTradeOffers = (Boolean)get("SendTradeOffers", true);
        
        isLoaded = true;
    }
    public static void save() {
        map = Global.getSector().getPersistentData();
        
        put("IdoneusCitadel", IdoneusCitadel);
        put("PhantomEntity", PhantomEntity);
        put("ExileMarket", ExileMarket);
        put("Ulterius", Ulterius);
        put("ExileFleet", ExileFleet);
        put("SendTradeOffers", SendTradeOffers);
    }
    static Object get(String key) {
        return get(key, null);
    }
    static Object get(String key, Object defaultValue) {
        key = PREFIX + key;
        if(map.containsKey(key)) return map.get(key);
        else return defaultValue;
    }
    static void put(String key, Object val) {
        map.put(PREFIX + key, val);
    }
}
