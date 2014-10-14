package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@SuppressWarnings("unchecked")
public class RefugeeSpawnPoint extends BaseSpawnPoint {

    static boolean relationsAreSet = false;

    static void initFactionRelations() {
        SectorAPI sector = Global.getSector();
        FactionAPI ice, ici, hgm, tty, ind, snd, prt, player;
        ice = sector.getFaction("sun_ice");
        ici = sector.getFaction("sun_ici");
        hgm = sector.getFaction("hegemony");
        //tty = sector.getFaction("tritachyon");
        ind = sector.getFaction("independent");
        //snd = sector.getFaction("sindrian_diktat");
        prt = sector.getFaction("pirates");
        player = sector.getFaction("player");

        ice.setRelationship(hgm.getId(), -1); // For piracy
        ice.setRelationship(ind.getId(), -1); // For preying on them
        ice.setRelationship(prt.getId(), -1); // For hunting their prey

        List factions = new ArrayList(sector.getAllFactions());
        factions.remove(ici);
        factions.remove(ice);
        factions.remove(player);

        for (Iterator iter = factions.iterator(); iter.hasNext();) {
            FactionAPI faction = (FactionAPI) iter.next();
            if (faction.getRelationship(ice.getId()) == 0) {
                int relation = -1;
                relation -= faction.getRelationship(hgm.getId());
                relation -= faction.getRelationship(ind.getId());
                relation -= faction.getRelationship(prt.getId());
                relation = Math.min(1, Math.max(-1, relation));

                ice.setRelationship(faction.getId(), relation);
            }
        }

        relationsAreSet = true;
    }

    public RefugeeSpawnPoint(SectorAPI sector, LocationAPI location,
            float daysInterval, int maxFleets, SectorEntityToken anchor) {

        super(sector, location, daysInterval, maxFleets, anchor);
    }

    @Override
    public CampaignFleetAPI spawnFleet() {
        if (!relationsAreSet) {
            initFactionRelations();
        }

        CampaignFleetAPI fleet = getSector().createFleet("sun_ice", "refugees");
        getLocation().spawnFleet(getAnchor(), 0, 0, fleet);

        List systems = new ArrayList(getSector().getStarSystems());
        systems.remove(getSector().getStarSystem("Ulterius"));
        StarSystemAPI destination = (StarSystemAPI) systems.get(new Random().nextInt(systems.size()));

        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, destination.getStar(), 100000);
        fleet.addAssignment(FleetAssignment.RAID_SYSTEM, destination.getStar(), 100000);

        BaseSpawnPoint spawn;

        spawn = new GuardianSpawnPoint(getSector(), destination, 21.453f, 1, fleet);
        destination.addScript(spawn);

        spawn = new RaiderSpawnPoint(getSector(), destination, 11.946f, 2, fleet);
        destination.addScript(spawn);

        spawn = new ScoutSpawnPoint(getSector(), destination, 5.057f, 5, fleet);
        destination.addScript(spawn);
        
        Global.getSector().getCampaignUI().addMessage("A large colony fleet has been driven from Idoneus Citadel, and is seeking refuge at " + destination.getName());

        return fleet;
    }

}
