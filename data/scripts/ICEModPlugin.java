package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.weapons.BoomerangMissileAI;
import data.scripts.weapons.MobiusRayAutofireAIPlugin;
import data.scripts.plugins.MxDroneAI;
import data.scripts.weapons.ArbalestMissileAI;
import data.scripts.weapons.AttackDroneMissileAI;
import data.scripts.weapons.HypermassDriverAutofireAIPlugin;
import data.scripts.weapons.PdDroneMissileAI;
import data.scripts.weapons.ScatterPdMissileAI;
import data.scripts.world.Ulterius;

public class ICEModPlugin extends BaseModPlugin
{
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

