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

    public static void resetColonyFleetMarket() {
        Data.ExileMarket.getConnectedEntities().clear();
        Data.ExileMarket.setPrimaryEntity(Data.PhantomEntity);
        Data.PhantomEntity.setMarket(Data.ExileMarket);
    }
    public void createColonyFleetMarket() {
        Data.ExileMarket = Global.getFactory().createMarket(
                "sun_ice_colony_fleet_market", "The Exiled Idoneus Colony Fleet", 3);
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
                "sun_ice_idoneus_citadel_market", "Idoneus Citadel", 3);
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
        Data.Ulterius = sector.createStarSystem("Ulterius");
        Data.Ulterius.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        PlanetAPI star = Data.Ulterius.initStar("sun_ice_ulterius", "brown_dwarf_star", // id in planets.json
                200f, // radius (in pixels at default zoom)
                -161380, -249730);   // location in hyperspace
        SectorEntityToken relay = Data.Ulterius.addCustomEntity("sun_ice_ulterius_relay",
                    "Ulterius Relay", "comm_relay", "independent");
        relay.setCircularOrbit(star, 150, 500, 200);
        Data.Ulterius.setLightColor(new Color(255, 238, 193));
        Data.Ulterius.autogenerateHyperspaceJumpPoints(true, true);
        
        return Data.Ulterius;
    }
    
    public void generate() {
        SectorAPI sector = Global.getSector();
        
        createUlterius();
        Data.PhantomEntity = Data.Ulterius.getStar();
        sector.removeStarSystem(Data.Ulterius);
        
        StarSystemAPI system = sector.getStarSystem("Arcadia");
        //StarSystemAPI system = sector.getStarSystem("Corvus");
        
        
        

//        Data.IdoneusCitadel = system.addOrbitalStation("sun_ice_idoneus_citadel",
//                system.getStar(), "stations", "sun_ice_idoneus_citadel", 160, 76,
//                210, 900, "Idoneus Citadel", "sun_ici");
        
//        
        Data.IdoneusCitadel = system.addCustomEntity("sun_ice_idoneus_citadel",
                "Idoneus Citadel", "sun_ice_idoneus_citadel", "sun_ici");
        Data.IdoneusCitadel.setCircularOrbit(system.getStar(), 76, 16000, 900);
        
        
//        SHALOM = Ulterius.addCustomEntity("sun_ice_exiled_colony_ship", "Shalom class Colony Ship",
//                "sun_ice_exiled_colony_ship", "sun_ice");
//        SHALOM.getLocation().set(IdoneusCitadel.getLocation());
        
        
        
//	PlanetAPI a1 = system.addPlanet("sun_ice_test", system.getStar(), "HRHRG", "rocky_metallic", 0, 150, 2900, 100);
//        
//        Data.IdoneusCitadel = system.addCustomEntity("sun_ice_idoneus_citadel",
//                "Idoneus Citadel", "sun_ice_idoneus_citadel", "sun_ici");
//        Data.IdoneusCitadel.setCircularOrbit(a1, 76, 210, 900);
        
        createColonyFleetMarket();
        createIdoneusCitadelMarket();
        
//        a1.setMarket(Data.CitadelMarket);
//        Data.CitadelMarket.setPrimaryEntity(a1);
        
//        StarSystemAPI corvus = Global.getSector().getStarSystem("Corvus");
//        corvus.addOrbitalStation("sun_ice_idoneus_citadel2", corvus.getStar(), 76, 700, 30, "Idoneus Citadel2", "sun_ice");
//        makeTestStation();
        
        
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("sun_ice");
        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("sun_ici");
        //SharedData.getData().getMarketsWithoutPatrolSpawn().add(Data.ExileMarket.getId());
        

        BaseSpawnPoint spawn;
//64f
        spawn = new RefugeeSpawnPoint(sector, system, 64f, 1, Data.IdoneusCitadel);
        system.addScript(spawn);

        
//        spawn = new BlockadeSpawnPoint(sector, Ulterius, 15, 1, IdoneusCitadel);
//        Ulterius.addScript(spawn);
//        spawn.spawnFleet();
    }
}
