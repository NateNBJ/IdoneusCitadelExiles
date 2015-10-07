package data.world;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import java.util.HashMap;
import java.util.Map;

public class Ulterius {
    public static final Map VENDOR_SHIPS = new HashMap();
    public static final Map VENDOR_WINGS = new HashMap();
    public static final Map VENDOR_WEAPONS = new HashMap();
    public static final Map VENDOR_CREW = new HashMap();
    public static final int VENDOR_SUPPLIES = 2000;
    public static final int VENDOR_FUEL = 3000;
    public static final int VENDOR_MARINES = 100;
    public static final int EXILE_MARKET_SIZE = 3;

    public static void resetColonyFleetMarket() {
        Data.ExileMarket.getConnectedEntities().clear();
        Data.ExileMarket.setPrimaryEntity(Data.PhantomEntity);
        Data.PhantomEntity.setMarket(Data.ExileMarket);
        //Data.ExileMarket.setSize(0);    // too screwy at present
        Data.ExileMarket.reapplyConditions();
    }
    public void createColonyFleetMarket() {
        Data.ExileMarket = Global.getFactory().createMarket("sun_ice_colony_fleet_market", 
                "The Exiled Idoneus Colony Fleet", EXILE_MARKET_SIZE);
        Data.ExileMarket.setFactionId("sun_ice");
        
        Data.ExileMarket.addSubmarket("open_market");
        Data.ExileMarket.addSubmarket("generic_military");
        Data.ExileMarket.addSubmarket("black_market");
        
        Data.ExileMarket.addCondition("population_3");
        Data.ExileMarket.addCondition("large_refugee_population");
        Data.ExileMarket.addCondition("free_market");
        //Data.ExileMarket.addCondition("trade_center");
        Data.ExileMarket.addCondition("sun_ice_colony_fleet");
        //Data.ExileMarket.addCondition("sun_ice_exotic_tech");
        
        Data.ExileMarket.getTariff().modifyFlat("sun_ice_colony_fleet_market", 0.25f);
        
        resetColonyFleetMarket();
        
        Global.getSector().getEconomy().addMarket(Data.ExileMarket);
    }    
    public void createIdoneusCitadelMarket() {
        Data.CitadelMarket = Global.getFactory().createMarket(
                "sun_ice_idoneus_citadel_market", "Idoneus Citadel", 5);
        Data.CitadelMarket.setFactionId("sun_ici");
        
        Data.CitadelMarket.addSubmarket("open_market");
        Data.CitadelMarket.addSubmarket("generic_military");
        Data.CitadelMarket.addSubmarket("black_market");
        Data.CitadelMarket.addSubmarket("storage");
        
        Data.CitadelMarket.addCondition("population_5");
        //Data.CitadelMarket.addCondition("sun_ice_exotic_tech");
        Data.CitadelMarket.addCondition("orbital_station");
        Data.CitadelMarket.addCondition("spaceport");
        Data.CitadelMarket.addCondition("urbanized_polity");
        Data.CitadelMarket.addCondition("military_base");
        Data.CitadelMarket.addCondition("stealth_minefields");
        Data.CitadelMarket.addCondition("headquarters");
        
        Data.CitadelMarket.getTariff().modifyFlat("sun_ice_idoneus_citadel_market", 0.35f);
        
        Data.CitadelMarket.setPrimaryEntity(Data.IdoneusCitadel);
        Data.IdoneusCitadel.setMarket(Data.CitadelMarket);
        
        Global.getSector().getEconomy().addMarket(Data.CitadelMarket);
    }
    public StarSystemAPI createUlterius() {
        SectorAPI sector = Global.getSector();
        StarSystemAPI ulterius = sector.createStarSystem("Ulterius");
        ulterius.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        PlanetAPI star = ulterius.initStar("sun_ice_ulterius", "brown_dwarf_star", // id in planets.json
                200f, // radius (in pixels at default zoom)
                -16138*3, -24973*3);   // location in hyperspace
        //SectorEntityToken relay = ulterius.addCustomEntity("sun_ice_ulterius_relay",
        //            "Ulterius Relay", "comm_relay", "independent");
        //relay.setCircularOrbit(star, 150, 500, 200);
        ulterius.setLightColor(new Color(255, 238, 193));
        ulterius.autogenerateHyperspaceJumpPoints(true, true);
        
        return ulterius;
    }
    
    public void generate() {
        SectorAPI sector = Global.getSector();
        
        StarSystemAPI ulterius = createUlterius();
        Data.PhantomEntity = ulterius.getStar();
        //sector.removeStarSystem(ulterius);
        
        StarSystemAPI system = sector.getStarSystem("Eos");
        //StarSystemAPI system = sector.getStarSystem("Corvus");
        
        Data.IdoneusCitadel = system.addCustomEntity("sun_ice_idoneus_citadel",
                "Idoneus Citadel", "sun_ice_idoneus_citadel", "sun_ici");
        Data.IdoneusCitadel.setCircularOrbit(system.getStar(), 76, 16000, 900);
        
        
//        SHALOM = Ulterius.addCustomEntity("sun_ice_exiled_colony_ship", "Shalom class Colony Ship",
//                "sun_ice_exiled_colony_ship", "sun_ice");
//        SHALOM.getLocation().set(IdoneusCitadel.getLocation());
        
        createColonyFleetMarket();
        createIdoneusCitadelMarket();
        
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("sun_ice");
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("sun_ici");
        //SharedData.getData().getMarketsWithoutPatrolSpawn().add(Data.ExileMarket.getId());

//        BaseSpawnPoint spawn;
//
//        spawn = new RefugeeSpawnPoint(sector, system, 64f, 1, Data.IdoneusCitadel);
//        system.addScript(spawn);
//        
//        spawn = new BlockadeSpawnPoint(sector, system, 15, 1, Data.IdoneusCitadel);
//        system.addScript(spawn);
//        spawn.spawnFleet();
    }
}
