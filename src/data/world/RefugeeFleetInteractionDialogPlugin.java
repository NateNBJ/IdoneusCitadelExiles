package data.world;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DisengageHarryAvailability;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.EngagementOutcome;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.PursueAvailability;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.Status;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.EncounterOption;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.InitialBoardingResponse;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.PostEngagementOption;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.PursuitOption;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatReadinessPlugin;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.impl.campaign.ExampleCustomUIPanel;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext.BoardingAttackType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext.BoardingOutcome;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext.BoardingResult;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext.EngageBoardableOutcome;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.DumpMemory;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import org.lwjgl.input.Keyboard;

public class RefugeeFleetInteractionDialogPlugin implements InteractionDialogPlugin, CoreInteractionListener {

    @Override
    public void coreUIDismissed() {
            optionSelected(null, OptionId.INIT_NO_TEXT);
    }

    private static enum VisualType {

        FLEET_INFO,
        OTHER,
    }

    private static enum OptionId {
        INIT,
        INIT_NO_TEXT,
        TRADE_CARGO,
        TRADE_SHIPS,
        REFIT,
        OPEN_COMM,
        CUT_COMM,
        ENGAGE,
        ATTEMPT_TO_DISENGAGE,
        DISENGAGE,
        SCUTTLE,
        PURSUE,
        AUTORESOLVE_PURSUE,
        HARRY_PURSUE,
        LET_THEM_GO,
        LEAVE,
        CONTINUE,
        GO_TO_MAIN,
        GO_TO_ENGAGE,
        CONTINUE_LOOT,
        CONTINUE_INTO_BATTLE,
        MAINTAIN_CONTACT,
        STAND_DOWN,
        CONTINUE_INTO_BOARDING,
        BOARDING_ACTION,
        SELECT_FLAGSHIP,
        CRASH_MOTHBALL,
        ENGAGE_BOARDABLE,
        ABORT_BOARDING_ACTION,
        HARD_DOCK,
        LAUNCH_ASSAULT_TEAMS,
        LET_IT_GO,
        SELECTOR_MARINES,
        SELECTOR_CREW,
    }

    private InteractionDialogAPI dialog;
    private TextPanelAPI textPanel;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;

    private CampaignFleetAPI playerFleet;
    private CampaignFleetAPI otherFleet;

    private FleetGoal playerGoal = FleetGoal.ATTACK;
    private FleetGoal otherGoal = FleetGoal.ATTACK;

    private VisualType currVisualType = VisualType.FLEET_INFO;

    private FleetEncounterContext context = new FleetEncounterContext();

    private static final Color HIGHLIGHT_COLOR = Global.getSettings().getColor("buttonShortcut");

    private RuleBasedInteractionDialogPluginImpl conversationDelegate;

    public static boolean directToComms = false;

    public Map<String, MemoryAPI> getMemoryMap() {
        return conversationDelegate == null ? null : conversationDelegate.getMemoryMap();
    }

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();

        playerFleet = Global.getSector().getPlayerFleet();
        otherFleet = (CampaignFleetAPI) (dialog.getInteractionTarget());

        playerFleet.getFleetData().takeSnapshot();
        otherFleet.getFleetData().takeSnapshot();

        visual.setVisualFade(0.25f, 0.25f);
        visual.showFleetInfo((String) null, playerFleet, (String) null, otherFleet, context);
        
        FleetInteractionDialogPluginImpl.inConversation = false;
        directToComms = false;
        conversationDelegate = new RuleBasedInteractionDialogPluginImpl();
        conversationDelegate.setEmbeddedMode(true);
        conversationDelegate.init(dialog);

        conversationDelegate.fireBest("BeginFleetEncounter");

        if (directToComms) {
            optionSelected(null, OptionId.OPEN_COMM);
        } else {
            optionSelected(null, OptionId.INIT);
        }

    }

    private EngagementResultAPI lastResult = null;

    public void backFromEngagement(EngagementResultAPI result) {

        if (!otherFleet.getMemoryWithoutUpdate().contains(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS)) {
            otherFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true, 0);
        }

        context.processEngagementResults(result);
        lastResult = result;

        if (origFlagship != null) {
            playerFleet.getFleetData().setFlagship(origFlagship);
            origFlagship = null;
            selectedFlagship = null;

        }

        if (context.getLastEngagementOutcome() == null) {
            return; // failsafe
        }

        boolean totalDefeat = !playerFleet.isValidPlayerFleet();
        boolean mutualDestruction = context.getLastEngagementOutcome() == EngagementOutcome.MUTUAL_DESTRUCTION;

        DataForEncounterSide playerSide = context.getDataFor(playerFleet);
        CrewCompositionAPI crewLosses = playerSide.getCrewLossesDuringLastEngagement();
        if (crewLosses.getTotalCrew() + crewLosses.getMarines() > 0 && !totalDefeat && !mutualDestruction) {
            addText(getString("casualtyReport"));

            DataForEncounterSide data = context.getDataFor(playerFleet);
            int crewLost = (int) (data.getCrewLossesDuringLastEngagement().getTotalCrew());
            int marinesLost = (int) (data.getCrewLossesDuringLastEngagement().getMarines());
            String crewLostStr = getApproximate(crewLost);
            String marinesLostStr = getApproximate(marinesLost);
            textPanel.highlightInLastPara(HIGHLIGHT_COLOR, crewLostStr, marinesLostStr);
        }

        boolean showFleetInfo = false;
        boolean playerHasPostCombatOptions = false;
        boolean enemyHasPostCombatOptions = false;

        switch (context.getLastEngagementOutcome()) {
            case BATTLE_ENEMY_WIN:
                addText(getString("battleDefeat"));
                showFleetInfo = true;
                enemyHasPostCombatOptions = true;
                break;
            case BATTLE_ENEMY_WIN_TOTAL:
                addText(getString("battleTotalDefeat"));
                showFleetInfo = true;
                enemyHasPostCombatOptions = true;
                break;
            case BATTLE_PLAYER_WIN:
                addText(getString("battleVictory"));
                showFleetInfo = true;
                playerHasPostCombatOptions = true;
                break;
            case BATTLE_PLAYER_WIN_TOTAL:
                addText(getString("battleTotalVictory"));
                showFleetInfo = true;
                playerHasPostCombatOptions = true;
                break;
            case ESCAPE_ENEMY_LOSS_TOTAL:
                addText(getString("pursuitTotalVictory"));
                showFleetInfo = true;
                playerHasPostCombatOptions = true;
                break;
            case ESCAPE_ENEMY_SUCCESS:
                if (result.getLoserResult().getDisabled().isEmpty() && result.getLoserResult().getDestroyed().isEmpty()) {
                    addText(getString("pursuitVictoryNoLosses"));
                } else {
                    addText(getString("pursuitVictoryLosses"));
                }
                showFleetInfo = true;
                playerHasPostCombatOptions = true;
                break;
            case ESCAPE_ENEMY_WIN:
                addText(getString("pursuitDefeat"));
                showFleetInfo = true;
                enemyHasPostCombatOptions = true;
                break;
            case ESCAPE_ENEMY_WIN_TOTAL:
                addText(getString("pursuitTotalDefeat"));
                showFleetInfo = true;
                enemyHasPostCombatOptions = true;
                break;
            case ESCAPE_PLAYER_LOSS_TOTAL:
                addText(getString("escapeTotalDefeat"));
                showFleetInfo = true;
                enemyHasPostCombatOptions = true;
                break;
            case ESCAPE_PLAYER_SUCCESS:
                addText(getString("escapeDefeat"));
                showFleetInfo = true;
                enemyHasPostCombatOptions = true;
                break;
            case ESCAPE_PLAYER_WIN:
                addText(getString("escapeVictory"));
                showFleetInfo = true;
                playerHasPostCombatOptions = true;
                break;
            case ESCAPE_PLAYER_WIN_TOTAL:
                addText(getString("escapeTotalVictory"));
                showFleetInfo = true;
                playerHasPostCombatOptions = true;
                break;
            case MUTUAL_DESTRUCTION:
                addText(getString("engagementMutualDestruction"));
			// bit of a hack. this'll make it so that the player's ships have a chance to be repaired
                // in the event of mutual destruction by adding them to the enemy fleet side's "disabled enemy ships" list.
                // it'll work by using the existing vs-player boarding path
                if (mutualDestruction) {
                    DataForEncounterSide otherData = context.getDataFor(otherFleet);
                    for (FleetMemberAPI member : result.getLoserResult().getDisabled()) {
                        otherData.addEnemy(member, Status.DISABLED);
                    }
                    context.applyPostEngagementOption(result, PostEngagementOption.STAND_DOWN);
                }
        }

        if (showFleetInfo) {
            visual.showFleetInfo((String) null, playerFleet, (String) null, otherFleet, context);
        }

        if (enemyHasPostCombatOptions) {
            PostEngagementOption option = otherFleet.getAI().pickPostEngagementOption(context, playerFleet);
            if (result.getLoserResult().getFleet().getFleetData().getMembersListCopy().isEmpty() || isFightingOver()) {
                if (option == PostEngagementOption.MAINTAIN_CONTACT) {
                    option = PostEngagementOption.STAND_DOWN;
                }
            }
            float recoveryFraction = context.getStandDownRecoveryFraction();
            boolean hasStandDown = recoveryFraction >= 0.01f;
            if (!hasStandDown && option == PostEngagementOption.STAND_DOWN) {
                option = null;
            }

            applyWinnerOption(option);

            if (option != null) {
                switch (option) {
                    case MAINTAIN_CONTACT:
                        if (context.getDataFor(playerFleet).getInReserveDuringLastEngagement().isEmpty()) {
                            addText(getString("enemyMaintainContactNoReserves"));
                        } else {
                            addText(getString("enemyMaintainContact"));
                        }
                        break;
                    case STAND_DOWN:
                        if (context.getLastEngagementOutcome() == EngagementOutcome.BATTLE_ENEMY_WIN_TOTAL
                                || context.getLastEngagementOutcome() == EngagementOutcome.ESCAPE_ENEMY_WIN_TOTAL
                                || context.getLastEngagementOutcome() == EngagementOutcome.ESCAPE_PLAYER_LOSS_TOTAL) {
                            addText(getString("enemyStandDownAfterTotalVictory"));
                        } else {
                            addText(getString("enemyStandDown"));
                        }
                        break;
                }
            }
            updateMainState();
        } else if (playerHasPostCombatOptions) {
            updatePostCombat(result);
        } else {
            updateMainState();
        }

//		if (!context.wasLastEngagementEscape()) {
//			if (context.didPlayerWinLastEngagement()) {
//				addText(getString("cleanDisengageOpportunity"));
//			}
//		}
        if (isFightingOver()) {
            if (context.isEngagedInHostilities()) {
                context.getDataFor(playerFleet).setDisengaged(!context.didPlayerWinEncounter());
                context.getDataFor(otherFleet).setDisengaged(context.didPlayerWinEncounter());
            }
        }

    }

    private void addPostBattleAttitudeText() {
        if (!context.wasLastEngagementEscape()) {
            if (context.didPlayerWinLastEngagement()) {
                addText(getString("cleanDisengageOpportunity"));
            }
        }
        if (!isFightingOver()) {
            if (otherFleetWantsToFight()) {
                addText(getString("postBattleAggressive"));
            } else if (otherFleetWantsToDisengage()) {
                if (!otherCanDisengage()) {
                    addText(getString("postBattleAggressive"));
                } else {
                    addText(getString("postBattleDisengage"));
                }
            } else {
                if (otherFleetHoldingVsStrongerEnemy()) {
                    addText(getString("postBattleHoldVsStrongerEnemy"));
                } else {
                    addText(getString("postBattleNeutral"));
                }
            }
        }
    }

    public void optionSelected(String text, Object optionData) {
        if (optionData == null) {
            return;
        }

        if (FleetInteractionDialogPluginImpl.inConversation) {
            conversationDelegate.optionSelected(text, optionData);
            if (!FleetInteractionDialogPluginImpl.inConversation) {
                optionSelected(null, OptionId.CUT_COMM);
            }
            return;
        }

        if (optionData == DumpMemory.OPTION_ID) {
            new DumpMemory().execute(null, dialog, null, conversationDelegate.getMemoryMap());
            return;
        }

        if (optionData instanceof String) {
            //??? failsafe
            optionSelected(null, OptionId.CUT_COMM);
            return;
        }

        OptionId option = (OptionId) optionData;

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        switch (option) {
            case INIT:
                boolean hostile = otherFleet.getAI() != null && otherFleet.getAI().isHostileTo(playerFleet);
                hostile |= context.isEngagedInHostilities();
                if (otherFleetWantsToFight()) {
                    addText(getString("initialAggressive"));
                } else if (otherFleetWantsToDisengage()) {
                    if (!otherCanDisengage()) {
                        if (hostile) {
                            addText(getString("initialAggressive"));
                        } else {
                            addText(getString("initialNeutral"));
                        }
                    } else {
                        if (hostile) {
                            addText(getString("initialDisengage"));
                        } else {
                            addText(getString("initialCareful"));
                        }
                    }
                } else {
                    if (otherFleetHoldingVsStrongerEnemy()) {
                        addText(getString("initialHoldVsStrongerEnemy"));
                    } else {
                        addText(getString("initialNeutral"));
                    }
                    
                    RepLevel rep = otherFleet.getFaction().getRelationshipLevel(playerFleet.getFaction());
                    if (rep.compareTo(RepLevel.SUSPICIOUS) < 0) {
                        addText("Due to your low standing with the Idoneus Exiles, the Colony fleet refuses to let you approach for trade.");
                    }
                }
                //textPanel.highlightFirstInLastPara("neutral posture", HIGHLIGHT_COLOR);
                updateMainState();
                break;

            // sun_ice
            case INIT_NO_TEXT:
                updateMainState();
                Data.SendTradeOffers = false;
                removeSoldShipsFromPlayerFleet();
                break;
            case TRADE_CARGO:
                addText("An emissary of the refugee fleet sends you a manifest over comm-link containing a list of the cargo they are willing to trade.");
                options.clearOptions();
                visual.showCore(CoreUITabId.CARGO, otherFleet, this);
                break;
            case TRADE_SHIPS:
                addText("An emissary of the refugee fleet sends you a manifest over comm-link containing a list of the ships they are willing to trade.");
                options.clearOptions();
                visual.showCore(CoreUITabId.FLEET, otherFleet, this);
                break;
            case REFIT:
                //addText("");
                options.clearOptions();
                visual.showCore(CoreUITabId.REFIT, otherFleet, this);
                break;

            case ENGAGE:
                //visual.showImagePortion("illustrations", "hound_hangar", 350, 75, 800, 800, 0, 0, 400, 400);
                if (otherFleetWantsToDisengage() && otherCanDisengage()) {
                    playerGoal = FleetGoal.ATTACK;
                    otherGoal = FleetGoal.ESCAPE;
                    addText(getString("engagePursuit"));
                } else {
                    playerGoal = FleetGoal.ATTACK;
                    otherGoal = FleetGoal.ATTACK;
                    addText(getString("engageMutual"));
                }
                updatePreCombat();
                break;
            case CONTINUE_INTO_BATTLE:
                BattleCreationContext bcc = new BattleCreationContext(playerFleet, playerGoal, otherFleet, otherGoal);
                if (playerGoal == FleetGoal.ESCAPE) {
                    DataForEncounterSide data = context.getDataFor(otherFleet);
//				if (data.getLastWinOption() == PostEngagementOption.HARRY) {
//					bcc.setPursuitRangeModifier(-1000);
//				}
                } else if (otherGoal == FleetGoal.ESCAPE) {
                    DataForEncounterSide data = context.getDataFor(playerFleet);
//				if (data.getLastWinOption() == PostEngagementOption.HARRY) {
//					bcc.setPursuitRangeModifier(-1000);
//				}

                    CampaignFleetAIAPI ai = otherFleet.getAI();
                    if (ai != null) {
                        ai.performCrashMothballingPriorToEscape(context, playerFleet);
                    }
                }

                visual.fadeVisualOut();
                dialog.startBattle(bcc);
                break;
            case DISENGAGE:
                CampaignFleetAIAPI ai = otherFleet.getAI();
                PursuitOption po = otherFleet.getAI().pickPursuitOption(context, playerFleet);
                if (otherFleetHoldingVsStrongerEnemy() || !otherFleetWantsToFight()) {
                    po = PursuitOption.LET_THEM_GO;
                }

                context.applyPursuitOption(otherFleet, playerFleet, po);
                context.getDataFor(playerFleet).setDisengaged(true);
                context.getDataFor(otherFleet).setDisengaged(false);
                switch (po) {
                    case PURSUE:
                    // shouldn't happen here, or we'd be in ATTEMPT_TO_DISENGAGE
                    case HARRY:
                        addText(getString("enemyHarass"));
                        context.setEngagedInHostilities(true);
                        context.getDataFor(playerFleet).setDisengaged(true);
                        context.getDataFor(otherFleet).setDisengaged(false);
                        break;
                    case LET_THEM_GO:
                        if (canDisengageCleanly(playerFleet)) {
                            addText(getString("enemyUnableToPursue"));
                        } else {
                            addText(getString("enemyDecidesNotToPursue"));
                        }
                        break;
                }
                updateMainState();
                break;
            case ATTEMPT_TO_DISENGAGE:
                boolean letGo = true;
                if (otherFleetWantsToFight()) {
                    PursuitOption pursuitOption = otherFleet.getAI().pickPursuitOption(context, playerFleet);
                    if (pursuitOption == PursuitOption.PURSUE) {
                        playerGoal = FleetGoal.ESCAPE;
                        otherGoal = FleetGoal.ATTACK;
                        addText(getString("enemyPursuit"));
                        letGo = false;
                        updatePreCombat();
                    } else if (pursuitOption == PursuitOption.HARRY) {
                        context.applyPursuitOption(otherFleet, playerFleet, PursuitOption.HARRY);
                        addText(getString("enemyHarass"));
                        context.setEngagedInHostilities(true);
                        //context.getDataFor(playerFleet).setDisengaged(!context.isEngagedInHostilities());
                        context.getDataFor(playerFleet).setDisengaged(true);
                        context.getDataFor(otherFleet).setDisengaged(false);
                        updateMainState();
                        letGo = false;
                    } else {
                        letGo = true;
                    }
                }
                if (letGo) {
                    PursueAvailability pa = context.getPursuitAvailability(otherFleet, playerFleet);
                    DisengageHarryAvailability dha = context.getDisengageHarryAvailability(otherFleet, playerFleet);
                    if (dha == DisengageHarryAvailability.AVAILABLE || pa == PursueAvailability.AVAILABLE) {
                        addText(getString("enemyDecidesNotToPursue"));
                    } else {
                        addText(getString("enemyUnableToPursue"));
                    }
                    context.getDataFor(playerFleet).setDisengaged(true);
                    context.getDataFor(otherFleet).setDisengaged(!context.isEngagedInHostilities());
                    updateMainState();
                }

//			String name = "Corvus III";
//			SectorEntityToken planet = Global.getSector().getStarSystem("Corvus").getEntityByName(name);
//			//planet = Global.getSector().getStarSystem("Corvus").getStar();
//			if (planet != null) {
//				addText("Incoming visual feed from " + name + ".");
//				visual.showPlanetInfo(planet);
//			} else {
//				addText("Planet " + name + " not found in the Corvus system.");
//			}
//			dialog.showTextPanel();
                //dialog.hideTextPanel();
                //dialog.setXOffset(-200);
                break;
            case OPEN_COMM:
                dialog.showTextPanel();
                dialog.flickerStatic(0.1f, 0.1f);

//			addText(String.format("\"%s\"", dialog.getNPCText(context, playerFleet, otherFleet)));
//			visual.showPersonInfo(otherFleet.getCommander());
//			updateDialogState();
                FleetInteractionDialogPluginImpl.inConversation = true;
                conversationDelegate = new RuleBasedInteractionDialogPluginImpl();
                conversationDelegate.setEmbeddedMode(true);
                conversationDelegate.init(dialog);

                boolean otherWantsToRun = otherFleetWantsToDisengage() && otherCanDisengage();
                MemoryAPI mem = conversationDelegate.getMemoryMap().get(MemKeys.LOCAL);
                if (otherWantsToRun) {
                    mem.unset("$weakerThanPlayerButHolding");
                }

                if (!conversationDelegate.fireBest("OpenCommLink")) {
                    addText("You try to establish a comm link, but only get static.");
                    FleetInteractionDialogPluginImpl.inConversation = false;
                }
                if (FleetInteractionDialogPluginImpl.inConversation) {
                    visual.showPersonInfo(otherFleet.getCommander());
                }
                break;
            case CUT_COMM:
                dialog.showTextPanel();
                dialog.flickerStatic(0.1f, 0.1f);

//			addText(getString("cutComm"));
//			visual.showFleetInfo((String)null, playerFleet, (String)null, otherFleet, context);
//			updateMainState();
                FleetInteractionDialogPluginImpl.inConversation = false;
//			addText(getString("cutComm"));
                visual.showFleetInfo((String) null, playerFleet, (String) null, otherFleet, context);
                //updateMainState();
                optionSelected(null, OptionId.INIT);

                break;
            case PURSUE:
                playerGoal = FleetGoal.ATTACK;
                otherGoal = FleetGoal.ESCAPE;
                addText(getString("pursue"));
                updatePreCombat();
                break;
            case AUTORESOLVE_PURSUE:
                dialog.showFleetMemberPickerDialog("Select craft to send in pursuit", "Ok", "Cancel",
                        3, 7, 58f, false, true, playerFleet.getFleetData().getMembersListCopy(),
                        new FleetMemberPickerListener() {
                            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                                if (members != null && !members.isEmpty()) {
                                    BattleAutoresolverPluginImpl resolver = new BattleAutoresolverPluginImpl(playerFleet, otherFleet);
                                    resolver.resolvePlayerPursuit(context, members);
                                    if (resolver.getResult() != null) {
                                        addText(getString("pursuitAutoresolve"));
                                        backFromEngagement(resolver.getResult());
                                    }
                                }
                            }

                            public void cancelledFleetMemberPicking() {

                            }
                        });
                break;
            case CRASH_MOTHBALL:
                List<FleetMemberAPI> choices = getCrashMothballable(playerFleet.getFleetData().getCombatReadyMembersListCopy());
                dialog.showFleetMemberPickerDialog("Select craft to crash-mothball", "Ok", "Cancel",
                        3, 7, 58f, false, true, choices,
                        new FleetMemberPickerListener() {
                            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                                for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                                    member.getRepairTracker().setCrashMothballed(false);
                                }
                                if (members != null && !members.isEmpty()) {
                                    for (FleetMemberAPI member : members) {
                                        member.getRepairTracker().setCrashMothballed(true);
                                    }

                                    crashMothballList = createShipNameListString(members);
                                    if (members.size() == 1) {
                                        addText(getString("crashMothballSelectedOneShip"));
                                    } else {
                                        addText(getString("crashMothballSelectedMultiple"));
                                    }
                                }
                            }

                            public void cancelledFleetMemberPicking() {

                            }
                        });
                break;
            case SCUTTLE:
                break;
            case GO_TO_ENGAGE:
                break;
            case GO_TO_MAIN:
                updateMainState();
                break;
            case CONTINUE:
                visual.showCustomPanel(810, 400, new ExampleCustomUIPanel());
                dialog.hideTextPanel();
                break;
            case LEAVE:
                if (isFightingOver()) {
                    if (!context.hasWinnerAndLoser()) {
                        if (context.getDataFor(playerFleet).isWonLastEngagement()) {
                            context.getDataFor(playerFleet).setDisengaged(false);
                            context.getDataFor(otherFleet).setDisengaged(true);
                        } else {
                            context.getDataFor(playerFleet).setDisengaged(true);
                            context.getDataFor(otherFleet).setDisengaged(false);
                        }
                    }
                } else {
                    if (context.isEngagedInHostilities()) {
                        context.getDataFor(playerFleet).setDisengaged(true);
                        context.getDataFor(otherFleet).setDisengaged(false);
                    } else {
                        context.getDataFor(playerFleet).setDisengaged(true);
                        context.getDataFor(otherFleet).setDisengaged(true);
                    }
                }

                context.applyAfterBattleEffectsIfThereWasABattle();
                Global.getSector().getCampaignUI().addMessage("Game paused");
                dialog.dismiss();
                break;
            case HARRY_PURSUE:
                addText(getString("playerHarass"));
                context.applyPursuitOption(playerFleet, otherFleet, PursuitOption.HARRY);
                context.setEngagedInHostilities(true);
                context.getDataFor(playerFleet).setDisengaged(false);
                context.getDataFor(otherFleet).setDisengaged(true);
                context.setEngagedInHostilities(true);
//			options.clearOptions();
//			options.addOption("Leave", OptionId.LEAVE, null);
                goToEncounterEndPath();
			//updateMainState();
                //dialog.dismiss();
                break;
            case LET_THEM_GO:
                addText(getString("playerLetGo"));
                context.getDataFor(playerFleet).setDisengaged(!context.isEngagedInHostilities());
                context.getDataFor(otherFleet).setDisengaged(true);
                goToEncounterEndPath();
                break;
            case STAND_DOWN:
                applyWinnerOption(PostEngagementOption.STAND_DOWN);
                addText(getString("playerStandDown"));
                addPostBattleAttitudeText();
                updateMainState();
                break;
            case MAINTAIN_CONTACT:
                applyWinnerOption(PostEngagementOption.MAINTAIN_CONTACT);
                if (context.getDataFor(otherFleet).getInReserveDuringLastEngagement().isEmpty()) {
                    addText(getString("playerMaintainContactNoReserves"));
                } else {
                    addText(getString("playerMaintainContact"));
                }
                addPostBattleAttitudeText();
                updateMainState();
                break;
            case CONTINUE_LOOT:
                visual.setVisualFade(0, 0);
                dialog.hideTextPanel();

                visual.showLoot("Salvaged", context.getLoot(), new CoreInteractionListener() {
                    public void coreUIDismissed() {
                        context.applyAfterBattleEffectsIfThereWasABattle();
                        dialog.dismiss();
                        dialog.hideTextPanel();
                    }
                });
                options.clearOptions();
                dialog.setPromptText("");
                //options.addOption("Leave", OptionId.LEAVE, null);
                break;
            case CONTINUE_INTO_BOARDING:
                goToEncounterEndPath();
                break;
            case BOARDING_ACTION:
                List<FleetMemberAPI> members = new ArrayList<FleetMemberAPI>();
                for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                    if (member.isFighterWing()) {
                        continue;
                    }
                    //if (!member.canBeDeployedForCombat()) continue;
                    members.add(member);
                }

                if (!members.isEmpty()) {
                    dialog.showFleetMemberPickerDialog("Select ships to use in boarding action", "Ok", "Cancel",
                            3, 7, 58f, false, true, members,
                            new FleetMemberPickerListener() {
                                public void pickedFleetMembers(List<FleetMemberAPI> members) {
                                    if (members != null && !members.isEmpty()) {
                                        boardingTaskForce = members;
                                        boardingTaskForceList = createShipNameListString(boardingTaskForce);
                                        if (members.size() == 1) {
                                            addText(getString("boardingTaskForceSelectedOneShip"));
                                        } else {
                                            addText(getString("boardingTaskForceSelectedMultiple"));
                                        }
                                        goToEncounterEndPath();
                                    }
                                }

                                public void cancelledFleetMemberPicking() {

                                }
                            });
                } else {
                    toBoard = null;
                    goToEncounterEndPath();
                }
                break;
            case SELECT_FLAGSHIP:
                members = new ArrayList<FleetMemberAPI>();
                for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                    if (member.isFighterWing()) {
                        continue;
                    }
                    members.add(member);
                }
                if (!members.isEmpty()) {
                    dialog.showFleetMemberPickerDialog("Select flagship for this engagement", "Ok", "Cancel",
                            3, 7, 58f, false, false, members,
                            new FleetMemberPickerListener() {
                                public void pickedFleetMembers(List<FleetMemberAPI> members) {
                                    if (members != null && !members.isEmpty()) {
                                        origFlagship = playerFleet.getFlagship();
                                        selectedFlagship = members.get(0);
                                        playerFleet.getFleetData().setFlagship(selectedFlagship);
                                        addText(getString("selectedFlagship"));
                                    }
                                }

                                public void cancelledFleetMemberPicking() {

                                }
                            });
                }
                break;
            case ENGAGE_BOARDABLE:
                EngageBoardableOutcome outcome = context.engageBoardableShip(toBoard, otherFleet, playerFleet);
                switch (outcome) {
                    case DESTROYED:
                        addText(getString("engageBoardableDestroyed"));
                        break;
                    case DISABLED:
                        addText(getString("engageBoardableDisabled"));
                        break;
                    case ESCAPED:
                        addText(getString("engageBoardableEscaped"));
                        break;
                }
                toBoard = null;
                goToEncounterEndPath();
                break;
            case LET_IT_GO:
                context.letBoardableGo(toBoard, otherFleet, playerFleet);
                addText(getString("letBoardableGo"));
                toBoard = null;
                boardingTaskForce = null;
                goToEncounterEndPath();
                break;
            case ABORT_BOARDING_ACTION:
                context.letBoardableGo(toBoard, otherFleet, playerFleet);
                addText(getString("letBoardableGo"));
                boardingTaskForce = null;
                toBoard = null;
                goToEncounterEndPath();
                break;
            case HARD_DOCK:
                initBoardingParty();
                if (boardingParty != null) {
                    boardingAttackType = BoardingAttackType.SHIP_TO_SHIP;
                    boardingResult = context.boardShip(toBoard, boardingParty, boardingAttackType, boardingTaskForce, playerFleet, otherFleet);
                    goToEncounterEndPath();
                }
                break;
            case LAUNCH_ASSAULT_TEAMS:
                initBoardingParty();
                if (boardingParty != null) {
                    boardingAttackType = BoardingAttackType.LAUNCH_FROM_DISTANCE;
                    boardingResult = context.boardShip(toBoard, boardingParty, boardingAttackType, boardingTaskForce, playerFleet, otherFleet);
                    goToEncounterEndPath();
                }
                break;
        }
    }

    private boolean okToLeave = false;
    private boolean didRepairs = false;
    private boolean didBoardingCheck = false;
    private boolean pickedMemberToBoard = false;
    private FleetMemberAPI toBoard = null;
    private String repairedShipList = null;
    private String boardingTaskForceList = null;
    private String crashMothballList = null;
    private List<FleetMemberAPI> boardingTaskForce = null;
    private int boardingPhase = 0;
    private CrewCompositionAPI maxBoardingParty = null;
    private CrewCompositionAPI boardingParty = null;
    private BoardingAttackType boardingAttackType = null;
    private BoardingResult boardingResult = null;
    private FleetMemberAPI selectedFlagship = null;
    private FleetMemberAPI origFlagship = null;

    private InitialBoardingResponse aiBoardingResponse = null;

    private void goToEncounterEndPath() {
        if (context.didPlayerWinEncounter()) {
            winningPath();
        } else {
            losingPath();
        }
    }

    private void losingPath() {
        options.clearOptions();

        context.getDataFor(playerFleet).setDisengaged(true);

        if (!recoveredCrew) {
            recoveredCrew = true;
            context.recoverCrew(otherFleet);
        }

        boolean playerHasReadyShips = !playerFleet.getFleetData().getCombatReadyMembersListCopy().isEmpty();
        boolean otherHasReadyShips = !otherFleet.getFleetData().getCombatReadyMembersListCopy().isEmpty();
        boolean totalDefeat = !playerFleet.isValidPlayerFleet();
        boolean mutualDestruction = context.getLastEngagementOutcome() == EngagementOutcome.MUTUAL_DESTRUCTION;
        if (!didBoardingCheck) {
            didBoardingCheck = true;
            toBoard = context.pickShipToBoard(otherFleet, playerFleet);
            if (toBoard != null) {
                pickedMemberToBoard = true;
                options.addOption("Continue", OptionId.CONTINUE_INTO_BOARDING, null);
                return;
            }
        }

        if (toBoard != null && aiBoardingResponse == null) {
            visual.showFleetMemberInfo(toBoard);

            if (mutualDestruction) {
                addText(getString("mutualDestructionRepairs"));
                aiBoardingResponse = InitialBoardingResponse.LET_IT_GO;
            } else {
                if (totalDefeat) {
                    addText(getString("lastFriendlyShipRepairs"));
                } else {
                    addText(getString("friendlyShipBoardable"));
                }
                aiBoardingResponse = otherFleet.getAI().pickBoardingResponse(context, toBoard, playerFleet);
            }

            if (!otherHasReadyShips) {
                aiBoardingResponse = InitialBoardingResponse.LET_IT_GO;
            }

            options.addOption("Continue", OptionId.CONTINUE_INTO_BOARDING, null);
            return;
        }

        if (toBoard != null && aiBoardingResponse != null) {
            switch (aiBoardingResponse) {
                case BOARD:
                    break;
                case ENGAGE:
                    EngageBoardableOutcome outcome = context.engageBoardableShip(toBoard, playerFleet, otherFleet);
                    switch (outcome) {
                        case DESTROYED:
                            if (totalDefeat) {
                                addText(getString("lastFriendlyBoardableDestroyed"));
                            } else {
                                addText(getString("engageFriendlyBoardableDestroyed"));
                            }
                            break;
                        case DISABLED:
                            if (totalDefeat) {
                                addText(getString("lastFriendlyBoardableDisabled"));
                            } else {
                                addText(getString("engageFriendlyBoardableDisabled"));
                            }
                            break;
                        case ESCAPED:
                            if (totalDefeat) {
                                addText(getString("lastFriendlyBoardableEscaped"));
                            } else {
                                addText(getString("engageFriendlyBoardableEscaped"));
                            }
                            break;
                    }
                    break;
                case LET_IT_GO:
                    context.letBoardableGo(toBoard, playerFleet, otherFleet);
                    if (!mutualDestruction) {
                        if (totalDefeat) {
                            addText(getString("engageFriendlyBoardableLetGo"));
                        } else {
                            addText(getString("lastFriendlyBoardableLetGo"));
                        }
                    }
                    break;
            }
        }

        totalDefeat = !playerFleet.isValidPlayerFleet();
        if (totalDefeat) {
            addText(getString("finalOutcomeNoShipsLeft"));
        }

        if (pickedMemberToBoard) {
            visual.showFleetInfo((String) null, playerFleet, (String) null, otherFleet, context);
        }

        context.generateLoot();
        context.autoLoot();
        context.repairShips();
        options.addOption("Leave", OptionId.LEAVE, null);
    }

    private boolean recoveredCrew = false;
    private boolean lootedCredits = false;
    private String creditsLooted = null;

    // sun_ice
    void removeSoldShipsFromPlayerFleet() {
        
    }
    
    private void winningPath() {
        options.clearOptions();
        DataForEncounterSide playerData = context.getDataFor(playerFleet);
        context.getDataFor(otherFleet).setDisengaged(true);

        if (!recoveredCrew) {
            recoveredCrew = true;
            if (playerData.getRecoverableCrewLosses().getTotalCrew() + playerData.getRecoverableCrewLosses().getMarines() > 0) {
                addText(getString("recoveryReport"));
                DataForEncounterSide data = context.getDataFor(playerFleet);
                int crewRecovered = (int) data.getRecoverableCrewLosses().getTotalCrew();
                int marinesRecovered = (int) data.getRecoverableCrewLosses().getMarines();
                String crewRecStr = "" + crewRecovered;
                if (crewRecovered <= 0) {
                    crewRecStr = "no";
                }
                String marinesRecStr = "" + marinesRecovered;
                if (marinesRecovered <= 0) {
                    marinesRecStr = "no";
                }
                //if (crewRecStr != null && marinesRecStr != null) {
                textPanel.highlightInLastPara(HIGHLIGHT_COLOR, crewRecStr, marinesRecStr);
				//} else if (crewRecStr != null) {
                //textPanel.highlightInLastPara(HIGHLIGHT_COLOR, crewRecStr);
                //} else if (marinesRecStr != null) {
                //textPanel.highlightInLastPara(HIGHLIGHT_COLOR, marinesRecStr);
                //}

                context.recoverCrew(playerFleet);
            }
        }

        if (!didRepairs) {
            List<FleetMemberAPI> repaired = context.repairShips();
            didRepairs = true;
            if (!repaired.isEmpty()) {
                repairedShipList = createShipNameListString(repaired);
                addText(getString("repairReport"));

                visual.showFleetInfo((String) null, playerFleet, (String) null, otherFleet, context);
//					options.addOption("Continue", OptionId.REPAIRED_CONTINUE, null);
//					return;
            }
        }

        boolean playerCanBoard = false;
        for (FleetMemberAPI member : playerFleet.getFleetData().getCombatReadyMembersListCopy()) {
            if (member.isFighterWing()) {
                continue;
            }
            if (!member.canBeDeployedForCombat()) {
                continue;
            }
            playerCanBoard = true;
        }
        boolean playerHasPersonnel = playerFleet.getCargo().getMarines() > 0
                || playerFleet.getCargo().getCrew(CrewXPLevel.GREEN) > 0
                || playerFleet.getCargo().getCrew(CrewXPLevel.REGULAR) > 0
                || playerFleet.getCargo().getCrew(CrewXPLevel.VETERAN) > 0
                || playerFleet.getCargo().getCrew(CrewXPLevel.ELITE) > 0;

        boolean playerHasReadyShips = !playerFleet.getFleetData().getCombatReadyMembersListCopy().isEmpty();

        if (!didBoardingCheck) {
            didBoardingCheck = true;
            toBoard = context.pickShipToBoard(playerFleet, otherFleet);
            if (toBoard != null) {
                pickedMemberToBoard = true;
                options.addOption("Continue", OptionId.CONTINUE_INTO_BOARDING, null);
                return;
            }
        }

        if (toBoard != null && boardingTaskForce == null) {
            visual.showFleetMemberInfo(toBoard);

            addText(getString("enemyShipBoardable"));
            int numLifeSigns = (int) (toBoard.getCrewComposition().getTotalCrew() + toBoard.getCrewComposition().getMarines());
            String approxLifeSigns = getApproximate(numLifeSigns);
            textPanel.highlightInLastPara(HIGHLIGHT_COLOR, approxLifeSigns);

            boolean boardingOk = true;
            boolean engageOk = true;
            String boardingTooltip = "tooltipBoardingAction";
            String engageTooltip = "tooltipEngageBoardable";
            if (!playerCanBoard) {
                boardingTooltip = "tooltipNoReadyShips";
                boardingOk = false;
            } else if (!playerHasPersonnel) {
                boardingTooltip = "tooltipNoPersonnel";
                boardingOk = false;
            }
            if (!playerHasReadyShips) {
                engageOk = false;
                engageTooltip = "tooltipNoReadyShips";
            }

            options.addOption("Organize a boarding task force", OptionId.BOARDING_ACTION, getString(boardingTooltip));
            options.addOption("Order nearby ships to engage", OptionId.ENGAGE_BOARDABLE, getString(engageTooltip));

            if (!boardingOk) {
                options.setEnabled(OptionId.BOARDING_ACTION, false);
            }
            if (!engageOk) {
                options.setEnabled(OptionId.ENGAGE_BOARDABLE, false);
            }

            options.addOption("Let it go", OptionId.LET_IT_GO, getString("tooltipLetShipGo"));
            return;
        }

        if (toBoard != null && boardingTaskForce != null && boardingResult == null) {
            maxBoardingParty = context.getMaxBoarders(playerFleet, boardingTaskForce);

            options.addSelector("Send in marines", OptionId.SELECTOR_MARINES,
                    Global.getSettings().getColor("progressBarCRColor"),
                    250, 50, 0, maxBoardingParty.getMarines(), ValueDisplayMode.VALUE,
                    getString("tooltipSelectorMarines"));

            options.addSelector("Send in crew", OptionId.SELECTOR_CREW,
                    Global.getSettings().getColor("progressBarCrewColor"),
                    250, 50, 0, maxBoardingParty.getTotalCrew(), ValueDisplayMode.VALUE,
                    getString("tooltipSelectorCrew"));

            options.setSelectorValue(OptionId.SELECTOR_MARINES, maxBoardingParty.getMarines());
            options.setSelectorValue(OptionId.SELECTOR_CREW, 0);

            options.addOption("Hard dock and attack ship-to-ship", OptionId.HARD_DOCK, getString("tooltipHardDockBoarding"));
            options.addOption("Launch assault teams from a distance", OptionId.LAUNCH_ASSAULT_TEAMS, getString("tooltipLaunchBoarding"));
            options.addOption("Abort the operation", OptionId.ABORT_BOARDING_ACTION, getString("tooltipAboutBoardingAction"));

            return;
        }

        if (boardingResult != null) {
            if (boardingPhase == 0) {
                boardingPhase++;
                if (boardingAttackType == BoardingAttackType.LAUNCH_FROM_DISTANCE) {
                    addText(getString("boardingApproachLaunch"));
                } else if (boardingAttackType == BoardingAttackType.SHIP_TO_SHIP) {
                    addText(getString("boardingApproachDock"));
                }
                options.addOption("Continue", OptionId.CONTINUE_INTO_BOARDING, null);
                return;
            }

            switch (boardingResult.getOutcome()) {
                case SELF_DESTRUCT:
                    addText(getString("boardingOutcomeSelfDestruct"));
                    if (boardingAttackType == BoardingAttackType.SHIP_TO_SHIP) {
                        boolean lostShips = !boardingResult.getLostInSelfDestruct().isEmpty();
                        boolean lostAll = boardingTaskForce.size() == boardingResult.getLostInSelfDestruct().size();

                        boolean singleShip = boardingTaskForce.size() == 1;
                        if (lostShips && lostAll) {
                            if (singleShip) {
                                addText(getString("boardingOutcomeShipLost"));
                            } else {
                                addText(getString("boardingOutcomeTaskForceLost"));
                            }
                        } else if (lostShips) {
                            addText(getString("boardingOutcomeTaskForceLostPartial"));
                        } else {
                            if (singleShip) {
                                addText(getString("boardingOutcomeShipDamaged"));
                            } else {
                                addText(getString("boardingOutcomeTaskForceDamaged"));
                            }
                        }
                    }
                    break;
                case SHIP_ESCAPED:
                    addText(getString("boardingOutcomeEscape"));
                    break;
                case SUCCESS_TOO_DAMAGED:
                    addText(getString("boardingOutcomeTooDamaged"));
                    break;
                case SUCCESS:
                    addText(getString("boardingOutcomeSuccess"));
                    break;
            }

            if (boardingResult.getOutcome() == BoardingOutcome.SHIP_ESCAPED_CLEAN) {
                addText(getString("boardingCleanEscapeMissedLaunch"));
                String crewLost = getIntOrNo(boardingResult.getAttackerLosses().getTotalCrew());
                String marinesLost = getIntOrNo(boardingResult.getAttackerLosses().getMarines());
                textPanel.highlightInLastPara(HIGHLIGHT_COLOR, crewLost, marinesLost);
            } else {
                if (playerFleet.isValidPlayerFleet()) {
                    addText(getString("boardingCasualtyReport"));
                    String crewLost = getIntOrNo(boardingResult.getAttackerLosses().getTotalCrew());
                    String marinesLost = getIntOrNo(boardingResult.getAttackerLosses().getMarines());
                    String enemyCrewLost = getIntOrNo(boardingResult.getDefenderLosses().getTotalCrew());
                    String enemyMarinesLost = getIntOrNo(boardingResult.getDefenderLosses().getMarines());
                    textPanel.highlightInLastPara(HIGHLIGHT_COLOR, crewLost, marinesLost, enemyCrewLost, enemyMarinesLost);
                }
            }

        }

        if (pickedMemberToBoard) {
            visual.showFleetInfo((String) null, playerFleet, (String) null, otherFleet, context);
        }

        if (!lootedCredits) {
            context.generateLoot();
            lootedCredits = true;

            float credits = context.getCreditsLooted();
            creditsLooted = "" + (int) credits;
            if (credits > 0) {
                addText(getString("creditsLootedReport"));
                textPanel.highlightLastInLastPara(creditsLooted, HIGHLIGHT_COLOR);
                playerFleet.getCargo().getCredits().add(credits);
            }
        }

        if (!context.getLoot().isEmpty() && playerFleet.isValidPlayerFleet()) {
            options.addOption("Pick through the salvage", OptionId.CONTINUE_LOOT, null);
        } else {
            if (!playerFleet.isValidPlayerFleet()) {
                addText(getString("finalOutcomeNoShipsLeft"));
            }
            options.addOption("Leave", OptionId.LEAVE, null);
        }
    }

    private List<FleetMemberAPI> getCrashMothballable(List<FleetMemberAPI> all) {
        List<FleetMemberAPI> result = new ArrayList<FleetMemberAPI>();
        CombatReadinessPlugin crPlugin = Global.getSettings().getCRPlugin();
        for (FleetMemberAPI member : all) {
            if (!member.isFighterWing() && member.getRepairTracker().getCR() < crPlugin.getMalfunctionThreshold()) {
                result.add(member);
            }
        }
        return result;
    }

    private void initBoardingParty() {
        if (options.getSelectorValue(OptionId.SELECTOR_CREW) + options.getSelectorValue(OptionId.SELECTOR_MARINES) < 1) {
            return;
        }

        boardingParty = Global.getFactory().createCrewComposition();

        float f = options.getSelectorValue(OptionId.SELECTOR_CREW) / options.getMaxSelectorValue(OptionId.SELECTOR_CREW);

        boardingParty.setMarines((int) options.getSelectorValue(OptionId.SELECTOR_MARINES));

        boardingParty.setGreen((int) (maxBoardingParty.getGreen() * f));
        boardingParty.setRegular((int) (maxBoardingParty.getRegular() * f));
        boardingParty.setVeteran((int) (maxBoardingParty.getVeteran() * f));
        boardingParty.setElite((int) (maxBoardingParty.getElite() * f));
    }

    private void applyWinnerOption(PostEngagementOption option) {
        if (lastResult == null) {
            return;
        }
        context.applyPostEngagementOption(lastResult, option);
        lastResult = null;
    }

    private OptionId lastOptionMousedOver = null;

    public void optionMousedOver(String optionText, Object optionData) {

        if (FleetInteractionDialogPluginImpl.inConversation) {
            conversationDelegate.optionMousedOver(optionText, optionData);
            return;
        }

        if (optionData instanceof String) {
            return;
        }

//		if (optionData == OptionId.HARD_DOCK) {
//			options.setTooltip(OptionId.HARD_DOCK, "Crew: " + options.getSelectorValue(OptionId.SELECTOR_CREW));
//		}
        if (optionData == null) {
            if (currVisualType != VisualType.FLEET_INFO) {
                visual.showFleetInfo((String) null, playerFleet, (String) null, otherFleet, context);
                currVisualType = VisualType.FLEET_INFO;
            }
            lastOptionMousedOver = null;
            return;
        }
        OptionId option = (OptionId) optionData;
        if (option == lastOptionMousedOver) {
            return;
        }
        lastOptionMousedOver = option;

//		if (option == OptionId.LET_THEM_GO) {
//			visual.showImagePortion("illustrations", "space_wreckage", 800, 800, 0, 0, 400, 400);
//			currVisualType = VisualType.OTHER;
//		} else if (option == OptionId.PURSUE) {
//			visual.showImagePortion("illustrations", "hull_breach", 400, 400, 0, 0, 400, 400);
//			currVisualType = VisualType.OTHER;
//		} else if (option == OptionId.HARRY_PURSUE) {
//			visual.showImagePortion("illustrations", "hound_hangar", 800, 800, 0, 0, 400, 400);
//			currVisualType = VisualType.OTHER;
//		} else {
//			if (currVisualType != VisualType.FLEET_INFO) {
//				visual.showFleetInfo((String)null, playerFleet, (String)null, otherFleet, context);
//				currVisualType = VisualType.FLEET_INFO;
//			}
//		}
    }

    public void advance(float amount) {

    }

    private void addText(String text) {
        textPanel.addParagraph(text);
    }

    private void appendText(String text) {
        textPanel.appendToLastParagraph(" " + text);
    }

    private void updateDialogState() {
        options.clearOptions();
        options.addOption("Cut the comm link", OptionId.CUT_COMM, null);
    }

    private void updatePreCombat() {
        options.clearOptions();

        playerFleet.updateCounts();
        int nonFighters = playerFleet.getFleetData().getMembersListCopy().size() - playerFleet.getNumFighters();
        if (playerGoal == FleetGoal.ATTACK && otherGoal == FleetGoal.ESCAPE) {
            String tooltipText = getString("tooltipPursueAutoresolve");
            options.addOption("Order your second-in-command to handle it", OptionId.AUTORESOLVE_PURSUE, tooltipText);
            options.addOption("Transfer command for this engagement", OptionId.SELECT_FLAGSHIP, getString("tooltipSelectFlagship"));
            if (nonFighters <= 1) {
                options.setEnabled(OptionId.SELECT_FLAGSHIP, false);
            }
            options.addOption("Take command of the action", OptionId.CONTINUE_INTO_BATTLE, null);
        } else {
            options.addOption("Transfer command for this engagement", OptionId.SELECT_FLAGSHIP, getString("tooltipSelectFlagship"));
            if (nonFighters <= 1) {
                options.setEnabled(OptionId.SELECT_FLAGSHIP, false);
            }
            if (playerGoal == FleetGoal.ESCAPE) {
                List<FleetMemberAPI> choices = getCrashMothballable(playerFleet.getFleetData().getCombatReadyMembersListCopy());

                options.addOption("Crash-mothball some of your ships to prevent malfunctions", OptionId.CRASH_MOTHBALL, null);
                if (choices.isEmpty()) {
                    options.setEnabled(OptionId.CRASH_MOTHBALL, false);
                    options.setTooltip(OptionId.CRASH_MOTHBALL, getString("tooltipCrashMothballUnavailable"));
                } else {
                    options.setTooltip(OptionId.CRASH_MOTHBALL, getString("tooltipCrashMothball"));
                }
            }
            options.addOption("Continue", OptionId.CONTINUE_INTO_BATTLE, null);
        }
        //if (Global.getSettings().isDevMode()) {
            options.addOption("Cancel", OptionId.GO_TO_MAIN, null);
        //}
    }

    private void updatePostCombat(EngagementResultAPI result) {
        options.clearOptions();

        if (result.didPlayerWin()) {
            float recoveryFraction = context.getStandDownRecoveryFraction();
            boolean hasStandDown = recoveryFraction >= 0.01f;

            if (!result.getLoserResult().getFleet().getFleetData().getMembersListCopy().isEmpty()
                    && !isFightingOver()) {
                options.addOption("Maintain contact with the enemy", OptionId.MAINTAIN_CONTACT, getString("tooltipMaintainContact"));
            } else if (!hasStandDown) {
				//options.addOption("Continue", OptionId.MAINTAIN_CONTACT);//, getString("tooltipMaintainContact"));
                // won't actually stand down, but will process lost enemy ships etc
                applyWinnerOption(PostEngagementOption.STAND_DOWN);
                updateMainState();
                return;
            }

            if (hasStandDown) {
                options.addOption("Order your deployed forces to stand down", OptionId.STAND_DOWN, getString("tooltipStandDown"));
                String rfStr = "" + (int) (recoveryFraction * 100f) + "%";
                options.setTooltipHighlightColors(OptionId.STAND_DOWN, HIGHLIGHT_COLOR);
                options.setTooltipHighlights(OptionId.STAND_DOWN, rfStr);
            } else {
                options.addOption("Order your deployed forces to stand down", OptionId.STAND_DOWN, getString("tooltipStandDownUnavailable"));
                options.setEnabled(OptionId.STAND_DOWN, false);
            }
        } else {
            updateMainState();
        }
    }

    private String createShipNameListString(List<FleetMemberAPI> members) {
        String str = "";
        int fighters = 0;
        int ships = 0;
        for (FleetMemberAPI member : members) {
            boolean last = members.indexOf(member) == members.size() - 1;
            boolean secondToLast = members.indexOf(member) == members.size() - 2;
            boolean fighter = member.isFighterWing();
            if (fighter) {
                fighters++;
            } else {
                ships++;
                if (last && fighters == 0 && ships > 1) {
                    if (members.size() > 2) {
                        str += ", and the " + member.getShipName();
                    } else {
                        str += " and the " + member.getShipName();
                    }
                } else {
                    str += "the " + member.getShipName();
                }
            }
            if (!last && !secondToLast && !fighter) {
                str += ", ";
            }

            if (last && fighters > 0) {
                if (fighters == 1) {
                    if (ships == 0) {
                        str += "a fighter wing";
                    } else {
                        if (ships > 1) {
                            str += ", and a fighter wing";
                        } else {
                            str += " and a fighter wing";
                        }
                    }
                } else {
                    if (ships == 0) {
                        str += "several fighter wings";
                    } else {
                        if (ships > 1) {
                            str += ", and several fighter wings";
                        } else {
                            str += " and several fighter wings";
                        }
                    }
                }
            }
        }
        return str;
    }

    private void updateMainState() {
        options.clearOptions();

        if (isFightingOver()) {
            goToEncounterEndPath();
            return;
        }

        options.addOption("Open a comm link", OptionId.OPEN_COMM, null);

        boolean otherWantsToRun = otherFleetWantsToDisengage() && otherCanDisengage();

        boolean playerHasReadyShips = !playerFleet.getFleetData().getCombatReadyMembersListCopy().isEmpty();
        if (otherWantsToRun && canDisengageCleanly(otherFleet)) {
            addText(getString("enemyCleanDisengage"));
            goToEncounterEndPath();
        } else if (otherWantsToRun) {
            String pursueTooltip = "tooltipPursue";
            String harassTooltip = "tooltipHarassRetreat";
            String letThemGoTooltip = "tooltipLetThemGo";
            if (!context.isEngagedInHostilities()) {
                letThemGoTooltip = "tooltipLetThemGoNoPenalty";
            }
            boolean canPursue = false;
            boolean canHasass = false;

            PursueAvailability pa = context.getPursuitAvailability(playerFleet, otherFleet);
            DisengageHarryAvailability dha = context.getDisengageHarryAvailability(playerFleet, otherFleet);

            switch (pa) {
                case AVAILABLE:
                    canPursue = true;
                    break;
                case LOST_LAST_ENGAGEMENT:
                    pursueTooltip = "tooltipPursueLostLast";
                    break;
                case NO_READY_SHIPS:
                    pursueTooltip = "tooltipNoReadyShips";
                    break;
                case STOOD_DOWN:
                    pursueTooltip = "tooltipPursueStoodDown";
                    break;
                case TOOK_SERIOUS_LOSSES:
                    pursueTooltip = "tooltipPursueSeriousLosses";
                    break;
                case TOO_SLOW:
                    pursueTooltip = "tooltipPursueTooSlow";
                    break;
            }

            switch (dha) {
                case AVAILABLE:
                    canHasass = true;
                    break;
                case NO_READY_SHIPS:
                    harassTooltip = "tooltipNoReadyShips";
                    break;
            }

            CampaignFleetAIAPI ai = otherFleet.getAI();
            boolean hostile = false;
            if (ai != null) {
                hostile = ai.isHostileTo(playerFleet) || context.isEngagedInHostilities();
            }

            options.addOption("Pursue them", OptionId.PURSUE, getString(pursueTooltip));
            options.addOption("Harry their retreat", OptionId.HARRY_PURSUE, getString(harassTooltip));
            if (hostile) {
                options.addOption("Let them go", OptionId.LET_THEM_GO, getString(letThemGoTooltip));
            } else {
                options.addOption("Leave", OptionId.LEAVE, null);
            }

            if (!canPursue) {
                options.setEnabled(OptionId.PURSUE, false);
            }
            if (!canHasass) {
                options.setEnabled(OptionId.HARRY_PURSUE, false);
            }
        } else {
            if (playerHasReadyShips) {
                options.addOption("Move in to engage", OptionId.ENGAGE, getString("tooltipEngage"));
            } else {
                options.addOption("Move in to engage", OptionId.ENGAGE, getString("tooltipNoReadyShips"));
                options.setEnabled(OptionId.ENGAGE, false);
            }

            // sun_ice
            RepLevel rep = otherFleet.getFaction().getRelationshipLevel(playerFleet.getFaction());
            if (rep.compareTo(RepLevel.SUSPICIOUS) >= 0) {
                options.addOption("Trade, or hire personnel", OptionId.TRADE_CARGO);
                options.setShortcut(OptionId.TRADE_CARGO, Keyboard.KEY_I, false, false, false, true);
                options.addOption("Buy or sell ships", OptionId.TRADE_SHIPS, null);
                options.setShortcut(OptionId.TRADE_SHIPS, Keyboard.KEY_F, false, false, false, true);
                options.addOption("Make use of the colony ship's refitting station", OptionId.REFIT);
                options.setShortcut(OptionId.REFIT, Keyboard.KEY_R, false, false, false, true);
            }

            CampaignFleetAIAPI ai = otherFleet.getAI();
            boolean hostile = false;
            if (ai != null) {
                hostile = ai.isHostileTo(playerFleet) || context.isEngagedInHostilities();
            }
            if (otherFleetWantsToFight() || (hostile && !otherFleetWantsToDisengage())) {
                if (canDisengageCleanly(playerFleet)) {
                    options.addOption("Disengage", OptionId.DISENGAGE, getString("tooltipCleanDisengage"));
//				} else if (canDisengageWithoutPursuit(playerFleet) || !otherFleetWantsToFight()) {
//					options.addOption("Disengage", OptionId.DISENGAGE, getString("tooltipHarrassableDisengage"));
                } else {
                    if (canDisengage() || !playerHasReadyShips) {
                        options.addOption("Attempt to disengage", OptionId.ATTEMPT_TO_DISENGAGE, getString("tootipAttemptToDisengage"));
                    } else {
                        addText(getString("playerTooLargeToDisengage"));
                        //options.addOption("Scuttle some of your ships", OptionId.SCUTTLE, null);
                    }
                }
            } else {
                //goToEncounterEndPath();
                options.addOption("Leave", OptionId.LEAVE, null);
            }
        }

        if (Global.getSettings().isDevMode()) {
            DumpMemory.addOption(dialog);
        }
    }

    private boolean canDisengage() {
        float total = 0f;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.canBeDeployedForCombat()) {
                total += member.getFleetPointCost();
            }
        }
        return total <= getDisengageSize();
    }

    private boolean otherCanDisengage() {
        float total = 0f;
        for (FleetMemberAPI member : otherFleet.getFleetData().getMembersListCopy()) {
            if (member.canBeDeployedForCombat()) {
                total += member.getFleetPointCost();
            }
        }
        return total <= getDisengageSize();
    }

    private float getDisengageSize() {
        float abs = Global.getSettings().getFloat("maxDisengageSize");
        float fraction = Global.getSettings().getFloat("maxDisengageFraction") * Global.getSettings().getBattleSize();
        return Math.min(abs, fraction);
    }

    private boolean canDisengageCleanly(CampaignFleetAPI fleet) {
        DataForEncounterSide data = context.getDataFor(fleet);
        if (data.isWonLastEngagement()) {
            return true;
        }
        return false;
    }

    private boolean canDisengageWithoutPursuit(CampaignFleetAPI fleet) {
        CampaignFleetAPI other = playerFleet;
        if (other == fleet) {
            other = otherFleet;
        }
        PursueAvailability pa = context.getPursuitAvailability(other, fleet);
        return pa != PursueAvailability.AVAILABLE;
    }

    private String getString(String id) {
        String str = Global.getSettings().getString("fleetInteractionDialog", id);

        String faction = otherFleet.getFaction().getDisplayName();
        String fleetName = otherFleet.getName();
        String firstName = otherFleet.getCommander().getName().getFirst();
        String lastName = otherFleet.getCommander().getName().getLast();
        String fleetOrShip = "fleet";
        if (otherFleet.getFleetData().getMembersListCopy().size() == 1) {
            fleetOrShip = "ship";
            if (otherFleet.getFleetData().getMembersListCopy().get(0).isFighterWing()) {
                fleetOrShip = "fighter wing";
            }
        }

        DataForEncounterSide data = context.getDataFor(playerFleet);
        int crewLost = (int) (data.getCrewLossesDuringLastEngagement().getTotalCrew());
        String crewLostStr = getApproximate(crewLost);

        int marinesLost = (int) (data.getCrewLossesDuringLastEngagement().getMarines());
        String marinesLostStr = getApproximate(marinesLost);

        int crewRecovered = (int) data.getRecoverableCrewLosses().getTotalCrew();
        int marinesRecovered = (int) data.getRecoverableCrewLosses().getMarines();

        String crewRecStr = "" + crewRecovered;
        if (crewRecovered <= 0) {
            crewRecStr = "no";
        }
        String marinesRecStr = "" + marinesRecovered;
        if (marinesRecovered <= 0) {
            marinesRecStr = "no";
        }

        if (toBoard != null) {
            int numLifeSigns = (int) (toBoard.getCrewComposition().getTotalCrew() + toBoard.getCrewComposition().getMarines());
            str = str.replaceAll("\\$numLifeSigns", getApproximate(numLifeSigns));

            str = str.replaceAll("\\$boardableShipName", toBoard.getShipName());
        }

        str = str.replaceAll("\\$faction", faction);
        str = str.replaceAll("\\$fleetName", fleetName);
        str = str.replaceAll("\\$firstName", firstName);
        str = str.replaceAll("\\$lastName", lastName);
        str = str.replaceAll("\\$fleetOrShip", fleetOrShip);
        str = str.replaceAll("\\$crewLost", crewLostStr);
        str = str.replaceAll("\\$marinesLost", marinesLostStr);
        str = str.replaceAll("\\$crewLost", crewLostStr);
        str = str.replaceAll("\\$crewRecovered", crewRecStr);
        str = str.replaceAll("\\$marinesRecovered", marinesRecStr);

        if (selectedFlagship != null) {
            str = str.replaceAll("\\$flagship", "the " + selectedFlagship.getShipName());
        }

        str = str.replaceAll("\\$creditsLooted", creditsLooted);

        if (boardingTaskForceList != null) {
            str = str.replaceAll("\\$boardingTaskForceShips", boardingTaskForceList);
        }
        if (crashMothballList != null) {
            str = str.replaceAll("\\$crashMothballList", crashMothballList);
        }

        if (boardingTaskForce != null && !boardingTaskForce.isEmpty()) {
            str = str.replaceAll("\\$boardingTaskForceShipName", boardingTaskForce.get(0).getShipName());
        }

        if (repairedShipList != null) {
            str = str.replaceAll("\\$repairedShipList", repairedShipList);
        }

        if (boardingResult != null) {
            str = str.replaceAll("\\$boardingCrewLost", getIntOrNo(boardingResult.getAttackerLosses().getTotalCrew()));
            str = str.replaceAll("\\$boardingMarinesLost", getIntOrNo(boardingResult.getAttackerLosses().getMarines()));
            str = str.replaceAll("\\$boardingEnemyCrewLost", getIntOrNo(boardingResult.getDefenderLosses().getTotalCrew()));
            str = str.replaceAll("\\$boardingEnemyMarinesLost", getIntOrNo(boardingResult.getDefenderLosses().getMarines()));
        }

        float recoveryFraction = context.getStandDownRecoveryFraction();
        str = str.replaceAll("\\$standDownRecovery", "" + (int) (recoveryFraction * 100f));

        return str;
    }

    private String getIntOrNo(float value) {
        if (value < 1) {
            return "no";
        }
        return "" + (int) value;
    }

    private String getApproximate(float value) {
        int v = (int) value;
        String str = "multiple";
        if (v <= 0) {
            str = "no";
        } else if (v < 10) {
            str = "" + v;
        } else if (v < 100) {
            v = (int) Math.round((float) v / 10f) * 10;
            str = "approximately " + v;
        } else if (v < 1000) {
            v = (int) Math.round((float) v / 10f) * 10;
            str = "approximately " + v;
        } else {
            v = (int) Math.round((float) v / 100f) * 100;
            str = "" + v;
        }
        return str;
    }

    private boolean isFightingOver() {
        return context.isBattleOver()
                || (context.getDataFor(otherFleet).disengaged() && context.getDataFor(playerFleet).disengaged());
//		return context.getDataFor(playerFleet).getLastGoal() == FleetGoal.ESCAPE ||
//			   context.getDataFor(otherFleet).getLastGoal() == FleetGoal.ESCAPE;
        //return context.getWinnerData().getLastGoal() == FleetGoal.ESCAPE || context.getLoserData().getLastGoal() == FleetGoal.ESCAPE;
    }

    private boolean otherFleetWantsToFight() {
        boolean hasNonCivReserves = false;
        for (FleetMemberAPI member : context.getDataFor(otherFleet).getInReserveDuringLastEngagement()) {
            if (!member.isCivilian()) {
                hasNonCivReserves = true;
                break;
            }
        }
        if (context.isEngagedInHostilities()
                && !context.getDataFor(otherFleet).isWonLastEngagement()
                && !hasNonCivReserves) {
            return false;
        }

        CampaignFleetAIAPI ai = otherFleet.getAI();
        if (ai == null) {
            return false;
        }
        return (ai.isHostileTo(playerFleet) || context.isEngagedInHostilities())
                && ai.pickEncounterOption(context, playerFleet) == EncounterOption.ENGAGE;
    }

    private boolean otherFleetHoldingVsStrongerEnemy() {
        CampaignFleetAIAPI ai = otherFleet.getAI();
        if (ai == null) {
            return false;
        }
        boolean hostile = ai.isHostileTo(playerFleet) || context.isEngagedInHostilities();
        return hostile && ai.pickEncounterOption(context, playerFleet) == EncounterOption.HOLD_VS_STRONGER;
    }

    private boolean otherFleetWantsToDisengage() {
        boolean hasNonCivReserves = false;
        for (FleetMemberAPI member : context.getDataFor(otherFleet).getInReserveDuringLastEngagement()) {
            if (!member.isCivilian()) {
                hasNonCivReserves = true;
                break;
            }
        }
        if (context.isEngagedInHostilities()
                && !context.getDataFor(otherFleet).isWonLastEngagement()
                && !hasNonCivReserves) {
            return true;
        }

        CampaignFleetAIAPI ai = otherFleet.getAI();
        if (ai == null) {
            return false;
        }
        return ai.pickEncounterOption(context, playerFleet) == EncounterOption.DISENGAGE;
    }

    public Object getContext() {
        return context;
    }
}
