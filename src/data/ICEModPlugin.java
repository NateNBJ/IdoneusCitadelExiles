package data;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.ai.drone.MxDroneAI;
import data.ai.missile.GandivaMissileAI;
import data.ai.missile.AttackDroneMissileAI;
import data.ai.missile.BoomerangMissileAI;
import data.ai.missile.MineAI;
import data.ai.missile.MinePodAI;
import data.ai.missile.PdDroneMissileAI;
import data.ai.missile.ScatterPdMissileAI;
import data.ai.missile.SpitfireMissileAI;
import data.ai.weapon.ChupacabraAutofireAIPlugin;
import data.ai.weapon.FissionDrillAutofireAIPlugin;
import data.ai.weapon.MobiusRayAutofireAIPlugin;
import data.ai.weapon.HypermassDriverAutofireAIPlugin;
import data.ai.weapon.NosAutofireAIPlugin;
import data.ai.weapon.NovaDischargerAutofireAIPlugin;
import data.ai.weapon.PdDroneAutofireAIPlugin;
import data.world.Data;
import data.world.ICECampaignPlugin;
import data.world.ICEEveryFrameScript;
import data.world.Ulterius;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

public class ICEModPlugin extends BaseModPlugin {
    public static boolean SHADER_LIB_ENABLED = false;
    public static boolean EXERELIN_ENABLED = false;
	public static boolean exerelinCorvusMode = false;
    public static final boolean SMILE_FOR_CAMERA = false;
    public static final Color HEAL_TEXT_COLOR = new Color(0, 255, 100);
    
    static void initFactionRelations() {
        SectorAPI sector = Global.getSector();
        FactionAPI ice, ici, hgm, tty, ind, snd, lud, prt, player;
        ice = sector.getFaction("sun_ice");
        ici = sector.getFaction("sun_ici");
        hgm = sector.getFaction("hegemony");
        tty = sector.getFaction("tritachyon");
        ind = sector.getFaction("independent");
        snd = sector.getFaction("sindrian_diktat");
        lud = sector.getFaction("luddic_church");
        prt = sector.getFaction("pirates");
        player = sector.getFaction("player");
        
        Data.Exiles = ice;
        Data.Garrison = ici;

        ice.setRelationship(hgm.getId(), RepLevel.HOSTILE);
        ice.setRelationship(prt.getId(), RepLevel.HOSTILE);
        ice.setRelationship(snd.getId(), RepLevel.HOSTILE);
        ice.setRelationship(lud.getId(), RepLevel.HOSTILE);
        ice.setRelationship(ind.getId(), RepLevel.SUSPICIOUS);
        ice.setRelationship(tty.getId(), RepLevel.SUSPICIOUS);
        ice.setRelationship(player.getId(), RepLevel.SUSPICIOUS);

        List factions = new ArrayList(sector.getAllFactions());
        factions.remove(ici);

        for (Iterator iter = factions.iterator(); iter.hasNext();) {
            FactionAPI faction = (FactionAPI) iter.next();
            ici.setRelationship(faction.getId(), RepLevel.HOSTILE);
        }
        
        ici.setRelationship(player.getId(), RepLevel.SUSPICIOUS);
        ici.setRelationship(ind.getId(), RepLevel.SUSPICIOUS);
        ici.setRelationship(snd.getId(), RepLevel.SUSPICIOUS);
        ici.setRelationship(tty.getId(), RepLevel.SUSPICIOUS);
        
        factions.remove(ice);
        factions.remove(player);

        for (Iterator iter = factions.iterator(); iter.hasNext();) {
            FactionAPI faction = (FactionAPI) iter.next();
            if (faction.getRelationship(ice.getId()) == 0) {
                float relation = -0.5f;
                relation -= faction.getRelationship(hgm.getId()) * 0.5f;
                relation -= faction.getRelationship(lud.getId()) * 0.5f;
                relation -= faction.getRelationship(prt.getId()) * 0.3f;
                relation = Math.min(1, Math.max(-1, relation));

                ice.setRelationship(faction.getId(), relation);
            }
        }
    }
    static void tryToEnableLighting() {
        try {
            Global.getSettings().getScriptClassLoader().loadClass("org.dark.shaders.util.ShaderLib");
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/light_data.csv");
			TextureData.readTextureDataCSV("data/lights/texture_data.csv");

            SHADER_LIB_ENABLED = true;
        } catch (Exception e) {
        }
    }

    @Override
    public void onApplicationLoad() {
        tryToEnableLighting();
        
        try {
            Global.getSettings().getScriptClassLoader().loadClass("data.scripts.world.ExerelinGen");
            EXERELIN_ENABLED = true;
        } catch (ClassNotFoundException ex) {
            EXERELIN_ENABLED = false;
        }
    }
    
    @Override
    public void onGameLoad() {
        if(EXERELIN_ENABLED) return;

        Data.load();
        
        if(Data.IdoneusCitadel == null) {
            onNewGame();
            onNewGameAfterEconomyLoad();
            onNewGameAfterTimePass();
        }
    }

    @Override
    public void beforeGameSave() {
        Data.save();
    }

    @Override
    public void onNewGame() {
        if(EXERELIN_ENABLED) return;

        new Ulterius().generate();
        Global.getSector().registerPlugin(new ICECampaignPlugin());
        Global.getSector().addScript(new ICEEveryFrameScript());
    }
    @Override
    public void onNewGameAfterEconomyLoad() {
        if(EXERELIN_ENABLED) return;

//        for(MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
//            float pop = (float) Math.pow(10, m.getSize());
//            float demand = (m.hasCondition("military_base") ? 0.02f : 0.005f) * pop;
//            m.getDemand("sun_ice_tech").getDemand().modifyFlat("sun_ice_base_idoneus_tech_demand", demand);
//        }
    }

    @Override
    public void onNewGameAfterTimePass() {
        if(EXERELIN_ENABLED) return;
        
        initFactionRelations();
        Data.save();
    }

    @Override
    public PluginPick pickDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        String id = drone.getHullSpec().getHullId();

        if (id.equals("sun_ice_drone_mx")) {
            return new PluginPick(new MxDroneAI(drone, mothership, system), CampaignPlugin.PickPriority.MOD_GENERAL);
        }

        return super.pickDroneAI(drone, mothership, system);
    }

    @Override
    public PluginPick pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        String id = missile.getProjectileSpecId();

        if (id.equals("sun_ice_scatterpd")) {
            return new PluginPick(new ScatterPdMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_boomerang")) {
            return new PluginPick(new BoomerangMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_gandiva")) {
            return new PluginPick(new GandivaMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_attackdrone")) {
            return new PluginPick(new AttackDroneMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_pddrone")) {
            return new PluginPick(new PdDroneMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_spitfire")) {
            return new PluginPick(new SpitfireMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_mine_pod")) {
            return new PluginPick(new MinePodAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_mine")) {
            return new PluginPick(new MineAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        }

        return super.pickMissileAI(missile, launchingShip);
    }

    @Override
    public PluginPick pickWeaponAutofireAI(WeaponAPI weapon) {
        String id = weapon.getId();

        if (id.equals("sun_ice_mobiusray")) {
            return new PluginPick(new MobiusRayAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_hypermassdriver")) {
            return new PluginPick(new HypermassDriverAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_nova")) {
            return new PluginPick(new NovaDischargerAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_fissiondrill")) {
            return new PluginPick(new FissionDrillAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_nos")) {
            return new PluginPick(new NosAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_chupacabra")) {
            return new PluginPick(new ChupacabraAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if (id.equals("sun_ice_pddrone")) {
            return new PluginPick(new PdDroneAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        }

        return super.pickWeaponAutofireAI(weapon);
    }
}
