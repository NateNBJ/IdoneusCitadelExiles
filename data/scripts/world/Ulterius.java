package data.scripts.world;

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
import java.util.Iterator;
import org.apache.log4j.Level;

public class Ulterius {

	public void generate() {
		SectorAPI sector = Global.getSector();
		StarSystemAPI system = sector.createStarSystem("Ulterius");
		LocationAPI hyper = Global.getSector().getHyperspace();
		
		system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
		
		// create the star and generate the hyperspace anchor for this system
		PlanetAPI star = system.initStar("brown_dwarf_star", // id in planets.json
										 200f, 		// radius (in pixels at default zoom)
										 1638, -11973);   // location in hyperspace
		
		system.setLightColor(new Color(255, 238, 193)); // light color in entire system, affects all entities
				
		SectorEntityToken station = system.addOrbitalStation(star, 76, 700, 30, "Citadel", "sun_ici");
		initStationCargo(station);

		system.autogenerateHyperspaceJumpPoints(true, true);

		BaseSpawnPoint spawn;

        int systems = sector.getStarSystems().size();
        int fleets = (int)Math.max(1, systems / 6f);

        Global.getLogger(Ulterius.class).log(Level.INFO, "stuffs " + fleets);
        
        spawn = new RefugeeSpawnPoint(sector, system, 16f / systems, fleets, station);
		system.addScript(spawn);
		//spawn.spawnFleet();

        spawn = new BlockadeSpawnPoint(sector, system, 15, 3, station);
        system.addScript(spawn);
        spawn.spawnFleet();


        FactionAPI ici = sector.getFaction("sun_ici");
        List factions = new ArrayList(sector.getAllFactions());
        factions.remove(ici);

        for(Iterator iter = factions.iterator(); iter.hasNext();) {
          FactionAPI faction = (FactionAPI)iter.next();
          ici.setRelationship(faction.getId(), -1);
        }
	}
	
	private void initStationCargo(SectorEntityToken station) {
		CargoAPI cargo = station.getCargo();
		addRandomWeapons(cargo, 5);
		
		cargo.addCrew(CrewXPLevel.VETERAN, 200);
		cargo.addCrew(CrewXPLevel.REGULAR, 2000);
		cargo.addMarines(200);
		cargo.addSupplies(3000);
		cargo.addFuel(2000);

		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_voidreaver_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_voidreaver_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_kelpie_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_kelpie_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_eidolon_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_eidolon_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_soulbane_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_soulbane_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_soulbane_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_specter_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_specter_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_specter_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_flashghast_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_flashghast_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_athame_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_athame_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_nightseer_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_nightseer_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_pentagram_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sun_ice_pentagram_Standard"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, "sun_ice_poltergeist_wing"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, "sun_ice_poltergeist_wing"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, "sun_ice_phantom_wing"));
		cargo.getMothballedShips().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.FIGHTER_WING, "sun_ice_phantom_wing"));
	}
	
	private void addRandomWeapons(CargoAPI cargo, int count) {
		List weaponIds = Global.getSector().getAllWeaponIds();
		for (int i = 0; i < count; i++) {
			String weaponId = (String) weaponIds.get((int) (weaponIds.size() * Math.random()));
			int quantity = (int)(Math.random() * 4f + 2f);
			cargo.addWeapons(weaponId, quantity);
		}
	}
}
