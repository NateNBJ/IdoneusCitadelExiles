package data;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
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
import data.ai.weapon.RecallAutofireAIPlugin;
import data.world.ICECampaignPlugin;
import data.world.Ulterius;
import java.awt.Color;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;

public class ICEModPlugin extends BaseModPlugin {
    public static boolean SHADER_LIB_ENABLED = false;
    public static boolean EXERELIN_ENABLED = false;
    public static final boolean SMILE_FOR_CAMERA = false;
    public static Color HEAL_TEXT_COLOR = new Color(0, 255, 100);

    public static void tryToEnableLighting() {
        try {
            Global.getSettings().getScriptClassLoader().loadClass("org.dark.shaders.util.ShaderLib");
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/light_data.csv");

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
//        StarSystemAPI star = Global.getSector().getStarSystem("Ulterius");
//        if(star == null) {
//            onNewGame();
//        }
    }

    @Override
    public void onNewGame() {
        if(!EXERELIN_ENABLED) {
            new Ulterius().generate();
            Global.getSector().registerPlugin(new ICECampaignPlugin());
        }
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
        } else if (id.equals("sun_ice_recall")) {
            return new PluginPick(new RecallAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        }

        return super.pickWeaponAutofireAI(weapon);
    }
}
