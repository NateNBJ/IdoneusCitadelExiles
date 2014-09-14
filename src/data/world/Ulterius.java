package data.world;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Ulterius {

    public static final Map VENDOR_SHIPS = new HashMap();
    public static final Map VENDOR_WINGS = new HashMap();
    public static final Map VENDOR_WEAPONS = new HashMap();
    public static final Map VENDOR_CREW = new HashMap();
    public static final int VENDOR_SUPPLIES = 2000;
    public static final int VENDOR_FUEL = 3000;
    public static final int VENDOR_MARINES = 100;
    public static SectorEntityToken STATION;

    static {
        VENDOR_CREW.put(CrewXPLevel.GREEN, 500);
        VENDOR_CREW.put(CrewXPLevel.REGULAR, 1000);
        VENDOR_CREW.put(CrewXPLevel.VETERAN, 100);
        VENDOR_CREW.put(CrewXPLevel.ELITE, 25);

        VENDOR_SHIPS.put("sun_ice_shalom_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_abraxas_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_apocrypha_Hull", 1);
        
        VENDOR_SHIPS.put("sun_ice_voidreaver_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_kelpie_Standard", 1);
        VENDOR_SHIPS.put("sun_ice_eidolon_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_soulbane_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_nightseer_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_athame_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_specter_Hull", 1);
        //VENDOR_SHIPS.put("sun_ice_flashghast_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_pentagram_Standard", 1);
        VENDOR_SHIPS.put("sun_ice_shiekwraith_Hull", 1);
        VENDOR_SHIPS.put("sun_ice_seraph_Hull", 1);
        //VENDOR_SHIPS.put("sun_ice_palantir_Hull", 1);

        VENDOR_WINGS.put("sun_ice_poltergeist_wing", 1);
        VENDOR_WINGS.put("sun_ice_phantom_wing", 1);
        VENDOR_WINGS.put("sun_ice_umbra_wing", 1);
        VENDOR_WINGS.put("sun_ice_stormwhisp_wing", 1);

        //VENDOR_WEAPONS.put("sun_ice_hypermassdriver", 2);
        VENDOR_WEAPONS.put("sun_ice_mobiusray", 8);
        VENDOR_WEAPONS.put("sun_ice_tractorbeam", 4);
        VENDOR_WEAPONS.put("sun_ice_boomerangpod", 12);
        VENDOR_WEAPONS.put("sun_ice_hypermassbomb", 3);
        VENDOR_WEAPONS.put("sun_ice_flamebolt", 8);
        VENDOR_WEAPONS.put("sun_ice_hexac", 16);
        VENDOR_WEAPONS.put("sun_ice_boomerang", 20);
        VENDOR_WEAPONS.put("sun_ice_scatterpd", 18);
        VENDOR_WEAPONS.put("sun_ice_lighthexac", 24);
        VENDOR_WEAPONS.put("sun_ice_minepod", 8);
        VENDOR_WEAPONS.put("sun_ice_gandiva", 10);
        VENDOR_WEAPONS.put("sun_ice_nos", 12);
        VENDOR_WEAPONS.put("sun_ice_falx", 16);
        //VENDOR_WEAPONS.put("sun_ice_spitfire", 10);
    }

    public void generate() {
        SectorAPI sector = Global.getSector();
        StarSystemAPI system = sector.createStarSystem("Ulterius");
        LocationAPI hyper = Global.getSector().getHyperspace();

        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");

        // create the star and generate the hyperspace anchor for this system
        PlanetAPI star = system.initStar("brown_dwarf_star", // id in planets.json
                200f, // radius (in pixels at default zoom)
                -16138, -24973);   // location in hyperspace

        system.setLightColor(new Color(255, 238, 193)); // light color in entire system, affects all entities

        STATION = system.addOrbitalStation(star, 76, 700, 30, "Citadel", "sun_ici");
        initStationCargo();

        system.autogenerateHyperspaceJumpPoints(true, true);

        BaseSpawnPoint spawn;

        int systems = sector.getStarSystems().size();
        int fleets = (int) Math.max(1, systems / 6f);

        //Global.getLogger(Ulterius.class).log(Level.INFO, "stuffs " + fleets);
        spawn = new RefugeeSpawnPoint(sector, system, 16f / systems, fleets, STATION);
        system.addScript(spawn);
        //spawn.spawnFleet();

        spawn = new BlockadeSpawnPoint(sector, system, 15, 3, STATION);
        system.addScript(spawn);
        spawn.spawnFleet();

        FactionAPI ici = sector.getFaction("sun_ici");
        List factions = new ArrayList(sector.getAllFactions());
        factions.remove(ici);

        for (Iterator iter = factions.iterator(); iter.hasNext();) {
            FactionAPI faction = (FactionAPI) iter.next();
            ici.setRelationship(faction.getId(), -1);
        }
    }

    public static void resetStationCargo() {
        CargoAPI cargo = Global.getSector().getStarSystem("Ulterius").getEntityByName("Citadel").getCargo();

        cargo.clear();
        cargo.getMothballedShips().clear();

        initStationCargo();
    }

    private static void initStationCargo() {
        CargoAPI cargo = Global.getSector().getStarSystem("Ulterius").getEntityByName("Citadel").getCargo();

        cargo.addMarines(VENDOR_MARINES);
        cargo.addSupplies(VENDOR_SUPPLIES);
        cargo.addFuel(VENDOR_FUEL);

        cargo.addCrew(CrewXPLevel.GREEN, (Integer) VENDOR_CREW.get(CrewXPLevel.GREEN));
        cargo.addCrew(CrewXPLevel.REGULAR, (Integer) VENDOR_CREW.get(CrewXPLevel.REGULAR));
        cargo.addCrew(CrewXPLevel.VETERAN, (Integer) VENDOR_CREW.get(CrewXPLevel.VETERAN));
        cargo.addCrew(CrewXPLevel.ELITE, (Integer) VENDOR_CREW.get(CrewXPLevel.ELITE));

        for (Iterator iter = VENDOR_SHIPS.keySet().iterator(); iter.hasNext();) {
            String id = (String) iter.next();
            int count = (Integer) VENDOR_SHIPS.get(id);

            for (int i = 0; i < count; ++i) {
                cargo.getMothballedShips().addFleetMember(Global.getFactory()
                        .createFleetMember(FleetMemberType.SHIP, id));
            }
        }

        for (Iterator iter = VENDOR_WINGS.keySet().iterator(); iter.hasNext();) {
            String id = (String) iter.next();
            int count = (Integer) VENDOR_WINGS.get(id);

            for (int i = 0; i < count; ++i) {
                cargo.getMothballedShips().addFleetMember(Global.getFactory()
                        .createFleetMember(FleetMemberType.FIGHTER_WING, id));
            }
        }

        for (Iterator iter = VENDOR_WEAPONS.keySet().iterator(); iter.hasNext();) {
            String id = (String) iter.next();

            cargo.addWeapons(id, (Integer) VENDOR_WEAPONS.get(id));
        }
    }
}
