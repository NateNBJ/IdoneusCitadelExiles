package data;

import data.world.ICEEveryFrameScript;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.ai.drone.MxDroneAI;
import data.ai.missile.ArbalestMissileAI;
import data.ai.missile.AttackDroneMissileAI;
import data.ai.missile.BoomerangMissileAI;
import data.ai.missile.MineAI;
import data.ai.missile.MinePodAI;
import data.ai.missile.PdDroneMissileAI;
import data.ai.missile.ScatterPdMissileAI;
import data.ai.missile.SpitfireMissileAI;
import data.ai.weapon.MobiusRayAutofireAIPlugin;
import data.ai.weapon.HypermassDriverAutofireAIPlugin;
import data.world.Ulterius;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;

public class ICEModPlugin extends BaseModPlugin
{
    public static boolean SHADER_LIB_AVAILABLE = false;
    
    public static void tryToEnableLighting() {
        try {  
            Global.getSettings().getScriptClassLoader().loadClass("org.dark.shaders.util.ShaderLib");
            ShaderLib.init();  
            LightData.readLightDataCSV("data/lights/light_data.csv");

            SHADER_LIB_AVAILABLE = true;  
        }
        catch (Exception e) { }
    }
    
    @Override
    public void onApplicationLoad() {
        tryToEnableLighting();
    }
    @Override
    public void onNewGame() {
		new Ulterius().generate();
        Global.getSector().addScript(new ICEEveryFrameScript());
    }
    @Override
    public PluginPick pickDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        String id = drone.getHullSpec().getHullId();

        if(id.equals("sun_ice_drone_mx"))
            return new PluginPick(new MxDroneAI(drone, mothership, system), CampaignPlugin.PickPriority.MOD_GENERAL);
            
        return super.pickDroneAI(drone, mothership, system);
    }
    @Override
    public PluginPick pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        String id = missile.getProjectileSpecId();
        
        if(id.equals("sun_ice_scatterpd")) {
            return new PluginPick(new ScatterPdMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_boomerang")) {
            return new PluginPick(new BoomerangMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_arbalest")) {
            return new PluginPick(new ArbalestMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_attackdrone")) {
            return new PluginPick(new AttackDroneMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_pddrone")) {
            return new PluginPick(new PdDroneMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_spitfire")) {
            return new PluginPick(new SpitfireMissileAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_mine_pod")) {
            return new PluginPick(new MinePodAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_mine")) {
            return new PluginPick(new MineAI(missile), CampaignPlugin.PickPriority.MOD_GENERAL);
        }

        return super.pickMissileAI(missile, launchingShip);
    }
    @Override
    public PluginPick pickWeaponAutofireAI(WeaponAPI weapon) {
        String id = weapon.getId();
        
        if(id.equals("sun_ice_mobiusray")) {
            return new PluginPick(new MobiusRayAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        } else if(id.equals("sun_ice_hypermassdriver")) {
            return new PluginPick(new HypermassDriverAutofireAIPlugin(weapon), CampaignPlugin.PickPriority.MOD_GENERAL);
        }
        
        return super.pickWeaponAutofireAI(weapon);
    }
}
