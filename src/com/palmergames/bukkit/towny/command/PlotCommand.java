package com.palmergames.bukkit.towny.command;

import com.google.common.collect.Iterables;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.PlotClearEvent;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.event.PlotPreClearEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.event.plot.PlotNotForSaleEvent;
import com.palmergames.bukkit.towny.event.plot.PlotSetForSaleEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleExplosionEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleFireEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotToggleMobsEvent;
import com.palmergames.bukkit.towny.event.plot.toggle.PlotTogglePvpEvent;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Send a list of all general towny plot help commands to player Command: /plot
 */

public class PlotCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	
	private static final List<String> plotTabCompletes = Arrays.asList(
		"claim",
		"unclaim",
		"forsale",
		"fs",
		"notforsale",
		"nfs",
		"evict",
		"perm",
		"set",
		"toggle",
		"clear",
		"group"
	);
	
	private static final List<String> plotGroupTabCompletes = Arrays.asList(
		"add",
		"remove",
		"set",
		"toggle",
		"fs",
		"notforsale",
		"forsale",
		"perm",
		"rename"
	);
	
	private static final List<String> plotSetTabCompletes = Arrays.asList(
		"reset",
		"shop",
		"embassy",
		"arena",
		"wilds",
		"inn",
		"jail",
		"farm",
		"bank",
		"outpost",
		"name",
		"perm"
	);
	
	private static final List<String> plotRectCircleCompletes = Arrays.asList(
		"rect",
		"circle"
	);
	
	private static final List<String> plotToggleTabCompletes = Arrays.asList(
		"fire",
		"pvp",
		"explosion",
		"mobs"
	);

	public PlotCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			if (plugin.isError()) {
				sender.sendMessage(Colors.Rose + "[Towny Error] Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			try {
				if (!TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName()).isUsingTowny()) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_set_use_towny_off"));
					return false;
				}
			} catch (NotRegisteredException e) {
				// World not registered				
			}

			if (args == null) {
				HelpMenu.PLOT_HELP.send(player);

			} else {
				try {
					return parsePlotCommand(player, args);
				} catch (TownyException x) {
					// No permisisons
					 x.getMessage();
				}
			}

		} else
			// Console
			HelpMenu.PLOT_HELP.send(sender);
		
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player) {
			switch (args[0].toLowerCase()) {
				case "set":
					if (args.length == 2) {
						return NameUtil.filterByStart(plotSetTabCompletes, args[1]);
					}
					if (args.length > 2 && args[1].equalsIgnoreCase("perm")) {
						return permTabComplete(StringMgmt.remArgs(args, 2));
					}
					break;
				case "toggle":
					if (args.length == 2)
						return NameUtil.filterByStart(plotToggleTabCompletes, args[1]);
					else if (args.length == 3)
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
					break;
				case "claim":
				case "unclaim":
				case "notforsale":
				case "nfs":
					if (args.length == 2)
						return NameUtil.filterByStart(plotRectCircleCompletes, args[1]);
					break;
				case "forsale":
				case "fs":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(Collections.singletonList("within"), args[1]);
						case 3:
							return NameUtil.filterByStart(plotRectCircleCompletes, args[2]);
					}
					break;
				case "group":
					if (args.length == 2) {
						return NameUtil.filterByStart(plotGroupTabCompletes, args[1]);
					} else if (args.length > 2) {
						return permTabComplete(StringMgmt.remFirstArg(args));
					}
					break;
				case "perm":
					if (args.length == 2)
						return NameUtil.filterByStart(Collections.singletonList("hud"), args[1]);
					break;
				default:
					if (args.length == 1)
						return NameUtil.filterByStart(plotTabCompletes, args[0]);
					break;
			}
		}

		return Collections.emptyList();
	}

	public boolean parsePlotCommand(Player player, String[] split) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.PLOT_HELP.send(player);
		} else {

			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			
			if (resident == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered"));
				return true;
			}
			
			String world = player.getWorld().getName();

			try {
				if (!TownyAPI.getInstance().isWilderness(player.getLocation()) && TownyAPI.getInstance().getTownBlock(player.getLocation()).getTown().isRuined())
					throw new TownyException(Translation.of("msg_err_cannot_use_command_because_town_ruined"));

				if (split[0].equalsIgnoreCase("claim")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLAIM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (TownyAPI.getInstance().isWarTime())
						throw new TownyException(Translation.of("msg_war_cannot_do"));

					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					
					// Fast-fail if this is a single plot and it is already claimed.
					if (selection.size() == 1 && selection.get(0).hasTownBlock() && selection.get(0).getTownBlock().hasResident())
						throw new TownyException(Translation.of("msg_already_claimed", selection.get(0).getTownBlock().getResident()));
					
					// Filter to just plots that are for sale.
					selection = AreaSelectionUtil.filterPlotsForSale(selection);

					// Filter out plots already owned by the player.
					selection = AreaSelectionUtil.filterUnownedBlocks(resident, selection);

					if (selection.size() > 0) {
						
						if (selection.size() > TownySettings.getMaxResidentPlots(resident))
							throw new TownyException(Translation.of("msg_max_plot_own", TownySettings.getMaxResidentPlots(resident)));

						double cost = 0;

						// Remove any plots Not for sale (if not the mayor) and
						// tally up costs.
						for (WorldCoord worldCoord : new ArrayList<>(selection)) {
							try {
								TownBlock townBlock = worldCoord.getTownBlock();
								double price = townBlock.getPlotPrice();
								
								if (townBlock.hasPlotObjectGroup()) {
									// This block is part of a group, special tasks need to be done.
									PlotGroup group = townBlock.getPlotObjectGroup();
									
									if (TownyEconomyHandler.isActive() && (!resident.getAccount().canPayFromHoldings(group.getPrice())))
										throw new TownyException(Translation.of("msg_no_funds_claim_plot_group", group.getTownBlocks().size(), TownyEconomyHandler.getFormattedBalance(group.getPrice())));

									// Add the confirmation for claiming a plot group.
									Confirmation.runOnAccept(() -> {
										ArrayList<WorldCoord> coords = new ArrayList<>();

										// Get worldcoords from plot group.
										group.getTownBlocks().forEach((tb) -> coords.add(tb.getWorldCoord()));

										// Execute the plot claim.
										new PlotClaim(Towny.getPlugin(), player, resident, coords, true, false, true).start();
									})
									.setTitle(Translation.of("msg_plot_group_claim_confirmation", group.getTownBlocks().size()) + " " + TownyEconomyHandler.getFormattedBalance(group.getPrice()) + ". " + Translation.of("are_you_sure_you_want_to_continue"))
									.sendTo(player);
									
									return true;
								}
								
								// Check if a plot has a price.
								if (price > -1)
									cost += townBlock.getPlotPrice();
								else {
									if (!townBlock.getTown().isMayor(resident)) 
										selection.remove(worldCoord);
								}
							} catch (NotRegisteredException e) {
								selection.remove(worldCoord);
							}
						}

						int maxPlots = TownySettings.getMaxResidentPlots(resident);
						int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);
						
						//Infinite plots
						if (maxPlots != -1) {
							maxPlots = maxPlots + extraPlots;
						}
						
						if (maxPlots >= 0 && resident.getTownBlocks().size() + selection.size() > maxPlots)
							throw new TownyException(Translation.of("msg_max_plot_own", maxPlots));

						if (TownyEconomyHandler.isActive() && (!resident.getAccount().canPayFromHoldings(cost)))
							throw new TownyException(Translation.of("msg_no_funds_claim_plot", TownyEconomyHandler.getFormattedBalance(cost)));

						if (cost != 0) {
							String title = Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost));
							final List<WorldCoord> finalSelection = selection;
							Confirmation.runOnAccept(() ->  {	
								// Start the claim task
								new PlotClaim(plugin, player, resident, finalSelection, true, false, false).start();
							})
							.setTitle(title)
							.sendTo(player);
						} else {
							// Start the claim task
							new PlotClaim(plugin, player, resident, selection, true, false, false).start();
						}
					} else {
						player.sendMessage(Translation.of("msg_err_empty_area_selection"));
					}
				} else if (split[0].equalsIgnoreCase("evict")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_EVICT.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (TownyAPI.getInstance().isWarTime())
						throw new TownyException(Translation.of("msg_war_cannot_do"));
					
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					Town town = townBlock.getTown();										
					
					if (townBlock.getResident() == null) {
						
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_no_one_to_evict"));						
					} else {
						
						Resident owner = townBlock.getResident();
						if (!town.equals(resident.getTown())){ 
							
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_part_town"));
							return false;							
						}

						if (townBlock.hasPlotObjectGroup()) {
							for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
								
								owner = tb.getResident();
								tb.setResident(null);
								tb.setPlotPrice(-1);

								// Set the plot permissions to mirror the towns.
								tb.setType(townBlock.getType());

								owner.save();
								// Update the townBlock data file so it's no longer using custom settings.
								tb.save();
							}
							
							player.sendMessage(Translation.of("msg_plot_evict_group", townBlock.getPlotObjectGroup().getName()));
							return true;
						}

						townBlock.setResident(null);
						townBlock.setPlotPrice(-1);

						// Set the plot permissions to mirror the towns.
						townBlock.setType(townBlock.getType());
						
						owner.save();
						// Update the townBlock data file so it's no longer using custom settings.
						townBlock.save();
						
						player.sendMessage(Translation.of("msg_plot_evict"));
					}

				} else if (split[0].equalsIgnoreCase("unclaim")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_UNCLAIM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (TownyAPI.getInstance().isWarTime())
						throw new TownyException(Translation.of("msg_war_cannot_do"));

					if (split.length == 2 && split[1].equalsIgnoreCase("all")) {
						// Start the unclaim task
						new PlotClaim(plugin, player, resident, null, false, false, false).start();

					} else {
						
						List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
						selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection);

						if (selection.size() > 0) {

							for (WorldCoord coord : selection) {
								TownBlock block = coord.getTownBlock();

								if (!block.hasPlotObjectGroup()) {
									// Start the unclaim task
									new PlotClaim(plugin, player, resident, selection, false, false, false).start();
									return true;
								}
								
								// Get all the townblocks part of the group.
								final List<WorldCoord> groupSelection = new ArrayList<>();
								block.getPlotObjectGroup().getTownBlocks().forEach((tb) -> {
									groupSelection.add(tb.getWorldCoord());
								});
								
								// Create confirmation.
								Confirmation.runOnAccept(() -> {
									new PlotClaim(Towny.getPlugin(), player, resident, groupSelection, false, false, false).start();
								})
								.setTitle(Translation.of("msg_plot_group_unclaim_confirmation", block.getPlotObjectGroup().getTownBlocks().size()) + " " + Translation.of("are_you_sure_you_want_to_continue"))
								.sendTo(player);
								
								return true;
							}

						} else {
							player.sendMessage(Translation.of("msg_err_empty_area_selection"));
						}
					}

				} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_NOTFORSALE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					WorldCoord pos = new WorldCoord(world, Coord.parseCoord(player));
					List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.remFirstArg(split));
					selection = AreaSelectionUtil.filterPlotsForSale(selection);
					
					if (permSource.testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode())) {
						for (WorldCoord worldCoord : selection) {
							if (worldCoord.getTownBlock().hasPlotObjectGroup()) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_plot_nfs", worldCoord));
								return false;
							}
							setPlotForSale(resident, worldCoord, -1);
						}
						return true;
					}
					
					// The follow test will clean up the initial selection fairly well, the plotTestOwner later on in the setPlotForSale will ultimately stop any funny business.
					if (permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()) && (resident.hasTown() && resident.getTown() == pos.getTownBlock().getTown())) {
						selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection); // Treat it as a mayor able to set their town's plots not for sale.
						selection = AreaSelectionUtil.filterOutResidentBlocks(resident, selection); // Filter out any resident-owned plots.
					} else {
						selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection); // Treat it as a resident making their own plots not for sale.
					}

					if (selection.isEmpty())
						throw new TownyException(Translation.of("msg_err_empty_area_selection"));

					// Set each WorldCoord in selection not for sale.
					for (WorldCoord worldCoord : selection) {
						
						// Skip over any plots that are part of a PlotGroup.
						if (worldCoord.getTownBlock().hasPlotObjectGroup()) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_plot_nfs", worldCoord));
							continue;
						}
						
						setPlotForSale(resident, worldCoord, -1);
					}
						

				} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					WorldCoord pos = new WorldCoord(world, Coord.parseCoord(player));
					double plotPrice = pos.getTownBlock().getTown().getPlotTypePrice(pos.getTownBlock().getType());

					if (split.length > 1) {
						/*
						 * This is not a case of '/plot fs' and has a cost and/or an area selection involved.
						 */

						// areaSelectPivot is how Towny handles the 'within' area selection when setting plots for sale.
						// Will return -1 if the word 'within' is not found.
						int areaSelectPivot = AreaSelectionUtil.getAreaSelectPivot(split);
						List<WorldCoord> selection;
						
						if (areaSelectPivot >= 0) { // 'within' has been used in the command, make a selection.
							selection = AreaSelectionUtil.selectWorldCoordArea(resident, new WorldCoord(world, Coord.parseCoord(player)), StringMgmt.subArray(split, areaSelectPivot + 1, split.length));
							
							// The follow test will clean up the initial selection fairly well, the plotTestOwner later on in the setPlotForSale will ultimately stop any funny business.
							if (permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()) && (resident.hasTown() && resident.getTown() == pos.getTownBlock().getTown())) {
								selection = AreaSelectionUtil.filterOwnedBlocks(resident.getTown(), selection); // Treat it as a mayor able to put their town's plots for sale.
								selection = AreaSelectionUtil.filterOutResidentBlocks(resident, selection); // Filter out any resident-owned plots.
							} else {
								selection = AreaSelectionUtil.filterOwnedBlocks(resident, selection); // Treat it as a resident putting their own plots up for sale.
							}
							
							if (selection.isEmpty()) 
								throw new TownyException(Translation.of("msg_err_empty_area_selection"));

						} else { // No 'within' found so this will be a case of /plot fs $, add a single coord to selection.
							selection = new ArrayList<>();
							selection.add(pos);
						}

						// Check that it's not: /plot forsale within rect 3
						if (areaSelectPivot != 1) {
							try {
								// command was 'plot fs $'
								plotPrice = Double.parseDouble(split[1]);
								if (plotPrice < 0) {
									TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_negative_money"));
									return true;
								}
							} catch (NumberFormatException e) {
								TownyMessaging.sendMessage(resident, Translation.of("msg_error_must_be_num"));
								return true;
							}
						}

						// Set each WorldCoord in selection for sale.
						for (WorldCoord worldCoord : selection) {
							TownBlock townBlock = worldCoord.getTownBlock();
							
							// Skip over any plots that are part of a PlotGroup.
							if (townBlock.hasPlotObjectGroup()) {
								TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_plot_fs2", worldCoord));
								continue;
							}
							
							// Otherwise continue on normally.
							setPlotForSale(resident, worldCoord, plotPrice);
						}
					} else {
						/*
						 * basic 'plot fs' command
						 */
						
						// Skip over any plots that are part of a PlotGroup.
						if (pos.getTownBlock().hasPlotObjectGroup()) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_plot_fs2", pos));
							return false;
						}

						// Otherwise continue on normally.
						setPlotForSale(resident, pos, plotPrice);
					}

				} else if (split[0].equalsIgnoreCase("perm") || split[0].equalsIgnoreCase("info")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (split.length > 1 && split[1].equalsIgnoreCase("hud")) {
						
						if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_HUD.getNode()))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						
						HUDManager.togglePermHUD(player);
						
					} else {
						if (TownyAPI.getInstance().isWilderness(player.getLocation())) {
							TownyMessaging.sendMessage(player, TownyFormatter.getStatus(TownyUniverse.getInstance().getDataSource().getWorld(player.getLocation().getWorld().getName())));
						} else {
							TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
							TownyMessaging.sendMessage(player, TownyFormatter.getStatus(townBlock));
						}
					}

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test in the plottoggle.
					 */

					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					// Test we are allowed to work on this plot
					plotTestOwner(resident, townBlock); // ignore the return as
					// we are only checking
					// for an exception
					
					// Make sure that the player is only operating on a single plot and not a plotgroup.
					if (townBlock.hasPlotObjectGroup()) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_toggle"));
						return false;
					}

					plotToggle(player, new WorldCoord(world, Coord.parseCoord(player)).getTownBlock(), StringMgmt.remFirstArg(split));

				} else if (split[0].equalsIgnoreCase("set")) {

					split = StringMgmt.remFirstArg(split);
					
					if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

						player.sendMessage(ChatTools.formatTitle("/... set"));
						player.sendMessage(ChatTools.formatCommand("", "set", "[plottype]", "Ex: Inn, Wilds, Farm, Embassy etc"));
						player.sendMessage(ChatTools.formatCommand("", "set", "outpost", "Costs " + TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost())));
						player.sendMessage(ChatTools.formatCommand("", "set", "reset", "Removes a plot type"));
						player.sendMessage(ChatTools.formatCommand("", "set", "[name]", "Names a plot"));
						player.sendMessage(ChatTools.formatCommand("Level", "[resident/ally/outsider]", "", ""));
						player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
						player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
						player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "ally off", ""));
						player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "friend build on", ""));
						player.sendMessage(Translation.of("plot_perms", "'friend'", "'resident'"));
						player.sendMessage(Translation.of("plot_perms_1"));

					} else if (split.length > 0) {

						if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(split[0].toLowerCase())))
							throw new TownyException(Translation.of("msg_err_command_disable"));
						
						TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
						
						// Make sure that the player is only operating on a plot object group if one exists.
						if (townBlock.hasPlotObjectGroup()) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_belongs_to_group_set"));
							return false;
						}

						if (split[0].equalsIgnoreCase("perm")) {

							// Set plot level permissions (if the plot owner) or
							// Mayor/Assistant of the town.
							
							// Test we are allowed to work on this plot
							TownBlockOwner owner = plotTestOwner(resident, townBlock);

							setTownBlockPermissions(player, owner, townBlock, StringMgmt.remFirstArg(split));

							return true;

						} else if (split[0].equalsIgnoreCase("name")) {
							
							// Test we are allowed to work on this plot
							plotTestOwner(resident, townBlock);
							if (split.length == 1) {
								townBlock.setName("");
								TownyMessaging.sendMsg(player, Translation.of("msg_plot_name_removed"));
								townBlock.save();
								return true;
							}
							
							String newName = StringMgmt.join(StringMgmt.remFirstArg(split), "_");
							
							// Test if the plot name contains invalid characters.
							if (!NameValidation.isBlacklistName(newName)) {								
								townBlock.setName(newName);

								//townBlock.setChanged(true);
								townBlock.save();

								TownyMessaging.sendMsg(player, Translation.of("msg_plot_name_set_to", townBlock.getName()));

							} else {

								TownyMessaging.sendErrorMsg(player, Translation.of("msg_invalid_name"));

							}
							return true;
						} else if (split[0].equalsIgnoreCase("outpost")) {

							if (TownySettings.isAllowingOutposts()) {
								if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode()))
									throw new TownyException(Translation.of("msg_err_command_disable"));
								
								// Test we are allowed to work on this plot
								plotTestOwner(resident, townBlock);
								
								Town town = townBlock.getTown();
								TownyWorld townyWorld = townBlock.getWorld();
								boolean isAdmin = permSource.isTownyAdmin(player);
								Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());
								
								if (OutpostUtil.OutpostTests(town, resident, townyWorld, key, isAdmin, true)) {
									// Test if they can pay.
									if (TownyEconomyHandler.isActive() && !town.getAccount().canPayFromHoldings(TownySettings.getOutpostCost())) 
										throw new TownyException(Translation.of("msg_err_cannot_afford_to_set_outpost"));
									 
									// Create a confirmation for setting outpost.
									Confirmation.runOnAccept(() -> {
										townBlock.setOutpost(true);
										 
										try {
											town.addOutpostSpawn(player.getLocation());
										} catch (TownyException e) {
											TownyMessaging.sendErrorMsg(e.getMessage());
											return;
										}

										//Make them pay, ignoring exception because we already know they can pay.
										if (TownyEconomyHandler.isActive() && TownySettings.getOutpostCost() > 0 )
											try {
												town.getAccount().withdraw(TownySettings.getOutpostCost(), "Plot Set Outpost");
											} catch (EconomyException ignored) {
											}
										town.save();
										townBlock.save();
										TownyMessaging.sendMessage(player, Translation.of("msg_plot_set_cost", TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost()), Translation.of("outpost")));
									})
									.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost())))
									.sendTo(player);
	
								}
								return true;
							}
						}
						
						/*
						 * After trying all of the other /plot set subcommands, attempt to set the townblock type.
						 */
						
						try {
							String plotTypeName = split[0];
							
							// Handle type being reset
							if (plotTypeName.equalsIgnoreCase("reset"))
								plotTypeName = "default";
							
							TownBlockType townBlockType = TownBlockType.lookup(plotTypeName);

							if (townBlockType == null)
								throw new TownyException(Translation.of("msg_err_not_block_type"));
							
							try {
								// Test we are allowed to work on this plot
								plotTestOwner(resident, townBlock); // ignore the return as we
								// are only checking for an
								// exception
							} catch (TownyException e) {
								TownyMessaging.sendErrorMsg(player, e.getMessage());
								return false;
							}
							
							PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(townBlockType, townBlock, resident);
							BukkitTools.getPluginManager().callEvent(preEvent);

							if (preEvent.isCancelled()) {
								player.sendMessage(preEvent.getCancelMessage());
								return false;
							}
								
							double cost = townBlockType.getCost();
							
							// Test if we can pay first to throw an exception.
							if (cost > 0 && TownyEconomyHandler.isActive() && !resident.getAccount().canPayFromHoldings(cost))
								throw new EconomyException(Translation.of("msg_err_cannot_afford_plot_set_type_cost", townBlockType, TownyEconomyHandler.getFormattedBalance(cost)));

							// Handle payment via a confirmation to avoid suprise costs.
							if (cost > 0 && TownyEconomyHandler.isActive()) {
								Confirmation.runOnAccept(() -> {
							
									try {
										resident.getAccount().withdraw(cost, String.format("Plot set to %s", townBlockType));
									} catch (EconomyException ignored) {
									}					

									TownyMessaging.sendMessage(resident, Translation.of("msg_plot_set_cost", TownyEconomyHandler.getFormattedBalance(cost), townBlockType));

									try {
										townBlock.setType(townBlockType, resident);
										
									} catch (TownyException e) {
										TownyMessaging.sendErrorMsg(resident, e.getMessage());
									}
									TownyMessaging.sendMsg(player, Translation.of("msg_plot_set_type", townBlockType));
								})
									.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost)))
									.sendTo(BukkitTools.getPlayerExact(resident.getName()));
							
							// No cost or economy so no confirmation.
							} else {
								townBlock.setType(townBlockType, resident);
								TownyMessaging.sendMsg(player, Translation.of("msg_plot_set_type", plotTypeName));
							}
						} catch (NotRegisteredException nre) {
							TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_part_town"));
						} catch (TownyException te){
							TownyMessaging.sendErrorMsg(player, te.getMessage());
						}

					} else {

						player.sendMessage(ChatTools.formatCommand("", "/plot set", "name", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "reset", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set", "shop|embassy|arena|wilds|spleef|inn|jail|farm|bank", ""));
						player.sendMessage(ChatTools.formatCommand("", "/plot set perm", "?", ""));
					}

				} else if (split[0].equalsIgnoreCase("clear")) {

					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_CLEAR.getNode()))
						throw new TownyException(Translation.of("msg_err_command_disable"));
					
					TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
					
					if (townBlock != null) {
						
						if (!townBlock.getWorld().isUsingPlotManagementMayorDelete())
							throw new TownyException(Translation.of("msg_err_plot_clear_disabled_in_this_world"));

						/*
						  Only allow mayors or plot owners to use this command.
						 */
						if (townBlock.hasResident()) {
							if (!townBlock.isOwner(resident)) {
								player.sendMessage(Translation.of("msg_area_not_own"));
								return true;
							}

						} else if (!townBlock.getTown().equals(resident.getTown())) {
							player.sendMessage(Translation.of("msg_area_not_own"));
							return true;
						}

						PlotPreClearEvent preEvent = new PlotPreClearEvent(townBlock);
						BukkitTools.getPluginManager().callEvent(preEvent);
						
						if (preEvent.isCancelled()) {
							player.sendMessage(preEvent.getCancelMessage());
							return false;
						}
							

						for (String material : TownyUniverse.getInstance().getDataSource().getWorld(world).getPlotManagementMayorDelete())
							if (Material.matchMaterial(material) != null) {
								TownyRegenAPI.deleteTownBlockMaterial(townBlock, Material.getMaterial(material));
								player.sendMessage(Translation.of("msg_clear_plot_material", material));
							} else
								throw new TownyException(Translation.of("msg_err_invalid_property", material));

						// Raise an event for the claim
						BukkitTools.getPluginManager().callEvent(new PlotClearEvent(townBlock));

					} else {
						// Shouldn't ever reach here as a null townBlock should
						// be caught already in WorldCoord.
						player.sendMessage(Translation.of("msg_err_empty_area_selection"));
					}

				} else if (split[0].equalsIgnoreCase("group")) {

					return handlePlotGroupCommand(StringMgmt.remFirstArg(split), player);
					
				} else
					throw new TownyException(Translation.of("msg_err_invalid_property", split[0]));

			} catch (TownyException | EconomyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		}

		return true;
	}

	/**
	 * Returns a TownyPermissionChange object representing the change action
	 *
	 * @param player Player initiator
	 * @param townBlockOwner Resident/Town with the targeted permissions change
	 * @param townBlock Targeted town block
	 * @param split Permission arguments
	 * @return a TownyPermissionChange object
	 */
	public static TownyPermissionChange setTownBlockPermissions(Player player, TownBlockOwner townBlockOwner, TownBlock townBlock, String[] split) {
		TownyPermissionChange permChange = null;

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			player.sendMessage(ChatTools.formatTitle("/... set perm"));
			if (townBlockOwner instanceof Town)
				player.sendMessage(ChatTools.formatCommand("Level", "[resident/nation/ally/outsider]", "", ""));
			if (townBlockOwner instanceof Resident)
				player.sendMessage(ChatTools.formatCommand("Level", "[friend/town/ally/outsider]", "", ""));

			player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
			player.sendMessage(ChatTools.formatCommand("", "set perm", "reset", ""));
			player.sendMessage(ChatTools.formatCommand("Eg", "/plot set perm", "friend build on", ""));
			return null;

		} else {

			TownyPermission perm = townBlock.getPermissions();

			if (split.length == 1) {

				if (split[0].equalsIgnoreCase("reset")) {

					// reset this townBlock permissions (by town/resident)
					permChange = new TownyPermissionChange(TownyPermissionChange.Action.RESET, false, townBlock);

					perm.change(permChange);
					townBlock.save();

					TownyMessaging.sendMsg(player, Translation.of("msg_set_perms_reset_single"));
					// Reset all caches as this can affect everyone.
					plugin.resetCache();

					return permChange;

				} else {

					// Set all perms to On or Off
					// '/plot set perm off'

					try {
						boolean b = StringMgmt.parseOnOff(split[0]);

						permChange = new TownyPermissionChange(TownyPermissionChange.Action.ALL_PERMS, b);
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
						return null;
					}

				}

			} else if (split.length == 2) {
				boolean b;

				try {
					b = StringMgmt.parseOnOff(split[1]);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";
				else if (split[0].equalsIgnoreCase("town"))
					split[0] = "nation";
				else if (split[0].equalsIgnoreCase("itemuse"))
					split[0] = "item_use";

				// Check if it is a perm level first
				try {
					TownyPermission.PermLevel permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase());

					permChange = new TownyPermissionChange(TownyPermissionChange.Action.PERM_LEVEL, b, permLevel);
				}
				catch (IllegalArgumentException permLevelException) {
					// If it is not a perm level, then check if it is a action type
					try {
						TownyPermission.ActionType actionType = TownyPermission.ActionType.valueOf(split[0].toUpperCase());

						permChange = new TownyPermissionChange(TownyPermissionChange.Action.ACTION_TYPE, b, actionType);
					} catch (IllegalArgumentException actionTypeException) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
						return null;
					}
				}
			} else if (split.length == 3) {
				// Reset the friend to resident so the perm settings don't fail
				if (split[0].equalsIgnoreCase("friend"))
					split[0] = "resident";

					// reset the town to nation so the perm settings don't fail
				else if (split[0].equalsIgnoreCase("town"))
					split[0] = "nation";

				if (split[1].equalsIgnoreCase("itemuse"))
					split[1] = "item_use";

				TownyPermission.PermLevel permLevel;
				TownyPermission.ActionType actionType;

				try {
					permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase());
					actionType = TownyPermission.ActionType.valueOf(split[1].toUpperCase());
				} catch (IllegalArgumentException ignore) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

				try {
					boolean b = StringMgmt.parseOnOff(split[2]);

					permChange = new TownyPermissionChange(TownyPermissionChange.Action.SINGLE_PERM, b, permLevel, actionType);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_set_perm_syntax_error"));
					return null;
				}

			}

			if (permChange != null)
				perm.change(permChange);

			townBlock.setChanged(true);
			townBlock.save();
			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendMsg(player, Translation.of("msg_set_perms"));
				TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
				TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString2().replace("n", "t") : perm.getColourString2().replace("f", "r"))));
				TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
			}


			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
			Bukkit.getServer().getPluginManager().callEvent(event);

			// Reset all caches as this can affect everyone.
			plugin.resetCache();
			return permChange;
		}
	}

	/**
	 * Set the plot for sale/not for sale if permitted
	 * 
	 * @param resident - Resident Object.
	 * @param worldCoord - WorldCoord.
	 * @param forSale - Price.
	 * @throws TownyException - Exception.
	 */
	public void setPlotForSale(Resident resident, WorldCoord worldCoord, double forSale) throws TownyException {

		try {
			TownBlock townBlock = worldCoord.getTownBlock();

			// Test we are allowed to work on this plot
			plotTestOwner(resident, townBlock); // ignore the return as we
			// are only checking for an
			// exception
			if (forSale > TownySettings.getMaxPlotPrice() ) {
				townBlock.setPlotPrice(TownySettings.getMaxPlotPrice());
			} else {
				townBlock.setPlotPrice(forSale);
			}

			if (forSale != -1) {
				TownyMessaging.sendPrefixedTownMessage(townBlock.getTown(), Translation.of("MSG_PLOT_FOR_SALE", resident.getName(), worldCoord.toString()));
				if (!resident.hasTown() || (resident.hasTown() && townBlock.getTown() != resident.getTown()))
					TownyMessaging.sendMsg(resident, Translation.of("MSG_PLOT_FOR_SALE", resident.getName(), worldCoord.toString()));
				Bukkit.getPluginManager().callEvent(new PlotSetForSaleEvent(resident, forSale, townBlock));
			} else {
				TownyMessaging.sendMsg(resident, Translation.of("msg_plot_set_to_nfs"));
				Bukkit.getPluginManager().callEvent(new PlotNotForSaleEvent(resident, townBlock));
			}

			// Save this townblock so the for sale status is remembered.
			townBlock.save();

		} catch (NotRegisteredException e) {
			throw new TownyException(Translation.of("msg_err_not_part_town"));
		}
	}

	/**
	 * Toggle the plots flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 * 
	 * @param player - Player.
	 * @param townBlock - TownBlock object.
	 * @param split  - Current command arguments.
	 */
	public void plotToggle(Player player, TownBlock townBlock, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/plot toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot toggle", "mobs", ""));
		} else {

			try {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
					throw new TownyException(Translation.of("msg_err_command_disable"));
				
				Optional<Boolean> choice = Optional.empty();
				if (split.length == 2) {
					choice = BaseCommand.parseToggleChoice(split[1]);
				}

				if (split[0].equalsIgnoreCase("pvp")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					//togglepvp fix PC-1
					Resident resident;
					resident = TownyUniverse.getInstance().getResident(player.getName());
					if (resident != null) {
					boolean outsiderintown = false;
					for (Player target : Bukkit.getOnlinePlayers()) {
						Resident targetresident = TownyUniverse.getInstance().getResident(target.getName());
						Block block = target.getLocation().getBlock().getRelative(BlockFace.DOWN);
						if (!TownyAPI.getInstance().isWilderness(block.getLocation())) {
							WorldCoord coord = WorldCoord.parseWorldCoord(target.getLocation());
							if (coord.equals(townBlock.getWorldCoord()) && ((!(targetresident.hasTown())) || (!(targetresident.getTown().equals(townBlock.getTown()))))) {
								outsiderintown = true;
							}
						}
					}
					if(outsiderintown) {
						throw new TownyException(Translation.of("polit_cubes_msg_err_cannot_toggle_pvp_in_plot_when_outsider_in_town"));
					} 
					} //PC-1 END
					if (TownySettings.getPVPCoolDownTime() > 0 && !permSource.testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode())) {
						// Test to see if the pvp cooldown timer is active for the town this plot belongs to.
						if (CooldownTimerTask.hasCooldown(townBlock.getTown().getName(), CooldownType.PVP))
							throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(townBlock.getTown().getName(), CooldownType.PVP)));
	
						// Test to see if the pvp cooldown timer is active for this plot.
						if (CooldownTimerTask.hasCooldown(townBlock.getWorldCoord().toString(), CooldownType.PVP))
							throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(townBlock.getWorldCoord().toString(), CooldownType.PVP)));
					}
					
					// Prevent plot pvp from being enabled if admin pvp is disabled
					if (townBlock.getTown().isAdminDisabledPVP() && !townBlock.getPermissions().pvp)
						throw new TownyException(Translation.of("msg_err_admin_controlled_pvp_prevents_you_from_changing_pvp", "adminDisabledPVP", "on"));
					
					// Prevent plot pvp from being disabled if admin pvp is enabled
					if (townBlock.getTown().isAdminEnabledPVP() && townBlock.getPermissions().pvp)
						throw new TownyException(Translation.of("msg_err_admin_controlled_pvp_prevents_you_from_changing_pvp", "adminEnabledPVP", "off"));

					// Fire cancellable event directly before setting the toggle.
					PlotTogglePvpEvent plotTogglePvpEvent = new PlotTogglePvpEvent(townBlock.getTown(), player, choice.orElse(!townBlock.getPermissions().pvp));
					Bukkit.getPluginManager().callEvent(plotTogglePvpEvent);
					if (plotTogglePvpEvent.isCancelled())
						throw new TownyException(plotTogglePvpEvent.getCancellationMsg());

					townBlock.getPermissions().pvp = choice.orElse(!townBlock.getPermissions().pvp);
					// Add a cooldown timer for this plot.
					if (TownySettings.getPVPCoolDownTime() > 0 && !permSource.testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode()))
						CooldownTimerTask.addCooldownTimer(townBlock.getWorldCoord().toString(), CooldownType.PVP);
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_pvp", "Plot", townBlock.getPermissions().pvp ? Translation.of("enabled") : Translation.of("disabled")));

				} else if (split[0].equalsIgnoreCase("explosion")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					// Fire cancellable event directly before setting the toggle.
					PlotToggleExplosionEvent plotToggleExplosionEvent = new PlotToggleExplosionEvent(townBlock.getTown(), player, choice.orElse(!townBlock.getPermissions().explosion));
					Bukkit.getPluginManager().callEvent(plotToggleExplosionEvent);
					if (plotToggleExplosionEvent.isCancelled())
						throw new TownyException(plotToggleExplosionEvent.getCancellationMsg());

					townBlock.getPermissions().explosion = choice.orElse(!townBlock.getPermissions().explosion);
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_expl", "the Plot", townBlock.getPermissions().explosion ? Translation.of("enabled") : Translation.of("disabled")));

				} else if (split[0].equalsIgnoreCase("fire")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					// Fire cancellable event directly before setting the toggle.
					PlotToggleFireEvent plotToggleFireEvent = new PlotToggleFireEvent(townBlock.getTown(), player, choice.orElse(!townBlock.getPermissions().fire));
					Bukkit.getPluginManager().callEvent(plotToggleFireEvent);
					if (plotToggleFireEvent.isCancelled())
						throw new TownyException(plotToggleFireEvent.getCancellationMsg());

					townBlock.getPermissions().fire = choice.orElse(!townBlock.getPermissions().fire);
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_fire", "the Plot", townBlock.getPermissions().fire ? Translation.of("enabled") : Translation.of("disabled")));

				} else if (split[0].equalsIgnoreCase("mobs")) {
					// Make sure we are allowed to set these permissions.
					toggleTest(player, townBlock, StringMgmt.join(split, " "));
					// Fire cancellable event directly before setting the toggle.
					PlotToggleMobsEvent plotToggleMobsEvent= new PlotToggleMobsEvent(townBlock.getTown(), player, choice.orElse(!townBlock.getPermissions().mobs));
					Bukkit.getPluginManager().callEvent(plotToggleMobsEvent);
					if (plotToggleMobsEvent.isCancelled())
						throw new TownyException(plotToggleMobsEvent.getCancellationMsg());

					townBlock.getPermissions().mobs = choice.orElse(!townBlock.getPermissions().mobs);
					
					TownyMessaging.sendMessage(player, Translation.of("msg_changed_mobs", "the Plot", townBlock.getPermissions().mobs ? Translation.of("enabled") : Translation.of("disabled")));

				} else {
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "plot"));
					return;
				}

				townBlock.setChanged(true);

				//Change settings event
				TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(townBlock);
				Bukkit.getServer().getPluginManager().callEvent(event);

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
			
			townBlock.save();
		}
	}

	/**
	 * Toggle the plot group flags for pvp/explosion/fire/mobs (if town/world
	 * permissions allow)
	 *
	 * @param player - Player.
	 * @param plotGroup - PlotObjectGroup object.
	 * @param split  - Current command arguments.
	 */
	public void plotGroupToggle(Player player, PlotGroup plotGroup, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			player.sendMessage(ChatTools.formatTitle("/plot group toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/plot group toggle", "mobs", ""));
		} else {

			try {
				// We need to keep an ending string to show the message only after the transaction is over,
				// to prevent chat log spam.
				String endingMessage = "";
				
				Optional<Boolean> choice = Optional.empty();
				if (split.length == 2) {
					choice = BaseCommand.parseToggleChoice(split[1]);
				}
				
				for (TownBlock groupBlock : plotGroup.getTownBlocks()) {
					if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(split[0].toLowerCase())))
						throw new TownyException(Translation.of("msg_err_command_disable"));

					if (split[0].equalsIgnoreCase("pvp")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));

						if (TownySettings.getPVPCoolDownTime() > 0) {
							// Test to see if the pvp cooldown timer is active for the town this plot belongs to.
							if (CooldownTimerTask.hasCooldown(groupBlock.getTown().getName(), CooldownType.PVP))
								throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(groupBlock.getTown().getName(), CooldownType.PVP)));

							// Test to see if the pvp cooldown timer is active for this plot.
							if (CooldownTimerTask.hasCooldown(groupBlock.getWorldCoord().toString(), CooldownType.PVP))
								throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(groupBlock.getWorldCoord().toString(), CooldownType.PVP)));
						}

						groupBlock.getPermissions().pvp = choice.orElse(!groupBlock.getPermissions().pvp);
						// Add a cooldown timer for this plot.
						if (TownySettings.getPVPCoolDownTime() > 0)
							CooldownTimerTask.addCooldownTimer(groupBlock.getWorldCoord().toString(), CooldownType.PVP);
						endingMessage = Translation.of("msg_changed_pvp", "Plot Group", groupBlock.getPermissions().pvp ? Translation.of("enabled") : Translation.of("disabled"));

					} else if (split[0].equalsIgnoreCase("explosion")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));
						groupBlock.getPermissions().explosion = choice.orElse(!groupBlock.getPermissions().explosion);
						endingMessage = Translation.of("msg_changed_expl", "the Plot Group", groupBlock.getPermissions().explosion ? Translation.of("enabled") : Translation.of("disabled"));

					} else if (split[0].equalsIgnoreCase("fire")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));
						groupBlock.getPermissions().fire = choice.orElse(!groupBlock.getPermissions().fire);
						endingMessage =  Translation.of("msg_changed_fire", "the Plot Group", groupBlock.getPermissions().fire ? Translation.of("enabled") : Translation.of("disabled"));

					} else if (split[0].equalsIgnoreCase("mobs")) {
						// Make sure we are allowed to set these permissions.
						toggleTest(player, groupBlock, StringMgmt.join(split, " "));
						groupBlock.getPermissions().mobs = choice.orElse(!groupBlock.getPermissions().mobs);
						endingMessage =  Translation.of("msg_changed_mobs", "the Plot Group", groupBlock.getPermissions().mobs ? Translation.of("enabled") : Translation.of("disabled"));

					} else {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "plot"));
						return;
					}

					groupBlock.setChanged(true);

					//Change settings event
					TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(groupBlock);
					Bukkit.getServer().getPluginManager().callEvent(event);
					
					// Save
					groupBlock.save();
				}
				
				// Finally send the message.
				TownyMessaging.sendMessage(player, endingMessage);
				

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

			
		}
	}

	/**
	 * Check the world and town settings to see if we are allowed to alter these
	 * settings
	 * 
	 * @param player
	 * @param townBlock
	 * @param split
	 * @throws TownyException if toggle is not permitted
	 */
	private void toggleTest(Player player, TownBlock townBlock, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

		split = split.toLowerCase();

		if (split.contains("mobs")) {
			if (townBlock.getWorld().isForceTownMobs())
				throw new TownyException(Translation.of("msg_world_mobs"));
		}

		if (split.contains("fire")) {
			if (townBlock.getWorld().isForceFire())
				throw new TownyException(Translation.of("msg_world_fire"));
		}

		if (split.contains("explosion")) {
			if (townBlock.getWorld().isForceExpl())
				throw new TownyException(Translation.of("msg_world_expl"));
		}

		if (split.contains("pvp")) {
			if (townBlock.getWorld().isForcePVP())
				throw new TownyException(Translation.of("msg_world_pvp"));
		}
		if ((split.contains("pvp")) || (split.trim().equalsIgnoreCase("off"))) {
			if (townBlock.getType().equals(TownBlockType.ARENA))
				throw new TownyException(Translation.of("msg_plot_pvp"));
		}
	}

	/**
	 * Test the townBlock to ensure we are either the plot owner, or the
	 * mayor/assistant
	 * 
	 * @param resident - Resident Object.
	 * @param townBlock - TownBlock Object.
	 * @return - returns owner of plot.
	 * @throws TownyException - Exception.
	 */
	public TownBlockOwner plotTestOwner(Resident resident, TownBlock townBlock) throws TownyException {

		Player player = BukkitTools.getPlayer(resident.getName());
		boolean isAdmin = TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player);

		if (townBlock.hasResident()) {

			Resident owner = townBlock.getResident();
			boolean isSameTown = (resident.hasTown()) && resident.getTown() == owner.getTown() && townBlock.getTown() == resident.getTown();  //Last test makes it so mayors cannot alter embassy plots owned by their residents in towns they are not mayor of.

			if ((resident == owner)
					|| ((isSameTown) && (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())))
					|| ((townBlock.getTown() == resident.getTown())) && (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
					|| isAdmin) {

				return owner;
			}

			// Not the plot owner or the towns mayor or an admin.
			throw new TownyException(Translation.of("msg_area_not_own"));

		} else {

			Town owner = townBlock.getTown();
			boolean isSameTown = (resident.hasTown()) && resident.getTown() == owner;

			if (isSameTown && !BukkitTools.getPlayer(resident.getName()).hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode()))
				throw new TownyException(Translation.of("msg_not_mayor_ass"));

			if (!isSameTown && !isAdmin)
				throw new TownyException(Translation.of("msg_err_not_part_town"));

			return owner;
		}

	}
	
	private boolean handlePlotGroupCommand(String[] split, Player player) throws TownyException {

		Resident resident;
		String world;

		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		resident = getResidentOrThrow(player.getUniqueId());
		world = player.getWorld().getName();
		
		TownBlock townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
		Town town = townBlock.getTown();

		// Test we are allowed to work on this plot
		plotTestOwner(resident, townBlock);

		if (split.length <= 0 || split[0].equalsIgnoreCase("?")) {

			player.sendMessage(ChatTools.formatTitle("/plot group"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "add | new | create", "[name]", "Ex: /plot group new ExpensivePlots"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "remove", "", "Removes a plot from the specified group."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "rename", "[newName]", "Renames the group you are standing in."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "set", "...", "Ex: /plot group set perm resident on."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "toggle", "...", "Ex: /plot group toggle [pvp|fire|mobs]"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "forsale | fs", "[price]", "Ex: /plot group forsale 50"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "notforsale | nfs", "", "Ex: /plot group notforsale"));

			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translation.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			
			return true;
		}

		if (split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("new") || split[0].equalsIgnoreCase("create")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_ADD.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// Add the group to the new plot.
			PlotGroup newGroup = null;

			if (townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_already_belongs_to_a_group", townBlock.getPlotObjectGroup().getName()));
				return false;
			}

			if (split.length == 2) {
				// Create a brand new plot group.
				UUID plotGroupID = TownyUniverse.getInstance().generatePlotGroupID();
				String plotGroupName = split[1];

				newGroup = new PlotGroup(plotGroupID, plotGroupName, town);

				// Don't add the group to the town data if it's already there.
				if (town.hasPlotGroupName(newGroup.getName())) {
					newGroup = town.getPlotObjectGroupFromName(newGroup.getName());
				}

				townBlock.setPlotObjectGroup(newGroup);

				// Check if a plot price is available.
				if (!(townBlock.getPlotPrice() < 0)) {
					newGroup.addPlotPrice(townBlock.getPlotPrice());
				}

				// Add the plot group to the town set.
				town.addPlotGroup(newGroup);
			} else {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_group_name_required"));
				return false;
			}

			TownyUniverse.getInstance().getDataSource().savePlotGroupList();

			// Save changes.
			newGroup.save();
			townBlock.save();
			town.save();

			TownyMessaging.sendMsg(player, Translation.of("msg_plot_was_put_into_group_x", townBlock.getX(), townBlock.getZ(), newGroup.getName()));

		} else if (split[0].equalsIgnoreCase("remove")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_REMOVE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			String name = townBlock.getPlotObjectGroup().getName();
			// Remove the plot from the group.
			townBlock.getPlotObjectGroup().removeTownBlock(townBlock);

			// Detach group from townblock.
			townBlock.removePlotObjectGroup();

			// Save
			townBlock.save();
			TownyMessaging.sendMsg(player, Translation.of("msg_plot_was_removed_from_group_x", townBlock.getX(), townBlock.getZ(), name));

		} else if (split[0].equalsIgnoreCase("rename")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_RENAME.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (!townBlock.hasPlotObjectGroup()) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}

			if (split.length == 1) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_group_name_required"));
				return false;
			}
		
			String newName= split[1];			
			String oldName = townBlock.getPlotObjectGroup().getName();
			// Change name;
			TownyUniverse.getInstance().getDataSource().renameGroup(townBlock.getPlotObjectGroup(), newName);
			TownyMessaging.sendMsg(player, Translation.of("msg_plot_renamed_from_x_to_y", oldName, newName));

		} else if (split[0].equalsIgnoreCase("forsale") || split[0].equalsIgnoreCase("fs")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_FORSALE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// This means the player wants to fs the plot group they are in.
			PlotGroup group = townBlock.getPlotObjectGroup();
			
			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			
			if (split.length < 2) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_group_specify_price"));
				return false;
			}

			int price = Integer.parseInt(split[1]);

			group.setPrice(price);
			
			// Save
			group.save();
			TownyUniverse.getInstance().getDataSource().savePlotGroupList();

			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_put_group_up_for_sale", player.getName(), group.getName(), TownyEconomyHandler.getFormattedBalance(group.getPrice())));
			
		} else if (split[0].equalsIgnoreCase("notforsale") || split[0].equalsIgnoreCase("nfs")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_NOTFORSALE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// This means the player wants to nfs the plot group they are in.
			PlotGroup group = townBlock.getPlotObjectGroup();
			
			if (group == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}

			group.setPrice(-1);

			// Save
			group.save();
			TownyUniverse.getInstance().getDataSource().savePlotGroupList();

			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));
		} else if (split[0].equalsIgnoreCase("toggle")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TOGGLE.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			if (townBlock.getPlotObjectGroup() == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			
			// Create confirmation.
			PlotGroup plotGroup = townBlock.getPlotObjectGroup();
			String title = Translation.of("msg_plot_group_toggle_confirmation", townBlock.getPlotObjectGroup().getTownBlocks().size()) + " " + Translation.of("are_you_sure_you_want_to_continue");
			Confirmation.runOnAccept(() -> {
				// Perform the toggle.
				new PlotCommand(Towny.getPlugin()).plotGroupToggle(player, plotGroup, StringMgmt.remArgs(split, 1));
			})
			.setTitle(title)
			.sendTo(player);
			
			return true;
		} else if (split[0].equalsIgnoreCase("set")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_SET.getNode()))
				throw new TownyException(Translation.of("msg_err_command_disable"));
			
			// Check if group is present.
			if (townBlock.getPlotObjectGroup() == null) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_plot_not_associated_with_a_group"));
				return false;
			}
			TownBlockOwner townBlockOwner = plotTestOwner(resident, townBlock);
			
			if (split.length < 2) {
				player.sendMessage(ChatTools.formatTitle("/plot group set"));
				if (townBlockOwner instanceof Town)
					player.sendMessage(ChatTools.formatCommand("Level", "[resident/nation/ally/outsider]", "", ""));
				if (townBlockOwner instanceof Resident)
					player.sendMessage(ChatTools.formatCommand("Level", "[friend/town/ally/outsider]", "", ""));				
				player.sendMessage(ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "[on/off]", "Toggle all permissions"));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "[level/type] [on/off]", ""));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "[level] [type] [on/off]", ""));
				player.sendMessage(ChatTools.formatCommand("/plot group set", "perm", "reset", ""));
				player.sendMessage(ChatTools.formatCommand("Eg", "/plot group set perm", "friend build on", ""));				
				player.sendMessage(ChatTools.formatCommand("/plot group set", "[townblocktype]", "", "Farm, Wilds, Bank, Embassy, etc."));
				return false;
			}

			if (split[1].equalsIgnoreCase("perm")) {
				
				// Set plot level permissions (if the plot owner) or
				// Mayor/Assistant of the town.
				
				PlotGroup plotGroup = townBlock.getPlotObjectGroup();
				
				Runnable permHandler = () -> {

					Iterator<TownBlock> townBlockIterator = plotGroup.getTownBlocks().iterator();
					
					if (!townBlockIterator.hasNext()) {
						return;
					}
					
					// Test the waters
					TownBlock tb = townBlockIterator.next();

					// setTownBlockPermissions returns a towny permission change object
					TownyPermissionChange permChange = PlotCommand.setTownBlockPermissions(player, townBlockOwner, tb, StringMgmt.remArgs(split, 2));
					// If the perm change object is not null
					if (permChange != null) {

						// A simple index loop starting from the second element
						while (townBlockIterator.hasNext()) {
							tb = townBlockIterator.next();

							tb.getPermissions().change(permChange);

							tb.setChanged(true);
							tb.save();

							// Change settings event
							TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(tb);
							Bukkit.getServer().getPluginManager().callEvent(event);
						}

						plugin.resetCache();
						
						TownyPermission perm = Iterables.getFirst(plotGroup.getTownBlocks(), null).getPermissions();
						TownyMessaging.sendMsg(player, Translation.of("msg_set_perms"));
						TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
						TownyMessaging.sendMessage(player, (Colors.Green + " Perm: " + ((townBlockOwner instanceof Resident) ? perm.getColourString2().replace("n", "t") : perm.getColourString2().replace("f", "r"))));
						TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + (!(CombatUtil.preventPvP(townBlock.getWorld(), townBlock)) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
					}
				};

				String title = Translation.of("msg_plot_group_set_perm_confirmation", townBlock.getPlotObjectGroup().getTownBlocks().size()) + " " + Translation.of("are_you_sure_you_want_to_continue");
				// Create confirmation.
				Confirmation.runOnAccept(permHandler)
					.setTitle(title)
					.sendTo(player);
				
				return true;
			}
			
			/*
			 * After all other set commands are tested for we attempt to set the townblocktype.
			 */
			
			String plotTypeName = split[1];

			// Stop setting plot groups to Jail plot, because that would set a spawn point for each plot in the location of the player.			
			if (plotTypeName.equalsIgnoreCase("jail")) {
				throw new TownyException(Translation.of(Translation.of("msg_err_cannot_set_group_to_jail")));
			}

			// Handle type being reset
			if (plotTypeName.equalsIgnoreCase("reset"))
				plotTypeName = "default";

			TownBlockType type = TownBlockType.lookup(plotTypeName);

			if (type == null)
				throw new TownyException(Translation.of("msg_err_not_block_type"));
				
			for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
				
				try {
					// Test we are allowed to work on this plot
					plotTestOwner(resident, townBlock); // ignore the return as we
					// are only checking for an
					// exception
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(resident, e.getMessage());
					return false;
				}
				
				// Allow for PlotPreChangeTypeEvent to trigger
				PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(type, tb, resident);
				BukkitTools.getPluginManager().callEvent(preEvent);
				
				// If any one of the townblocks is not allowed to be set, cancel setting all of them.
				if (preEvent.isCancelled()) {
					player.sendMessage(preEvent.getCancelMessage());
					return false;
				}
			}
			
			int amount = townBlock.getPlotObjectGroup().getTownBlocks().size();			
			double cost = type.getCost() * amount;
			
			try {
				// Test if we can pay first to throw an exception.
				if (cost > 0 && TownyEconomyHandler.isActive() && !resident.getAccount().canPayFromHoldings(cost))
					throw new EconomyException(Translation.of("msg_err_cannot_afford_plot_set_type_cost", type, TownyEconomyHandler.getFormattedBalance(cost)));

				// Handle payment via a confirmation to avoid suprise costs.
				if (cost > 0 && TownyEconomyHandler.isActive()) {
					Confirmation.runOnAccept(() -> {
				
						try {
							resident.getAccount().withdraw(cost, String.format("Plot (" + amount + ") set to %s", type));
						} catch (EconomyException ignored) {
						}					

						TownyMessaging.sendMessage(resident, Translation.of("msg_plot_set_cost", TownyEconomyHandler.getFormattedBalance(cost), type));

						for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks()) {
							try {
								tb.setType(type, resident);
							} catch (TownyException ignored) {
								// Cannot be set to jail type as a group.
							}
						}
						TownyMessaging.sendMsg(player, Translation.of("msg_set_group_type_to_x", type));
						
					})
						.setTitle(Translation.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost)))
						.sendTo(BukkitTools.getPlayerExact(resident.getName()));
				
				// No cost or economy so no confirmation.
				} else {

					for (TownBlock tb : townBlock.getPlotObjectGroup().getTownBlocks())
						tb.setType(type, resident);
					TownyMessaging.sendMsg(player, Translation.of("msg_set_group_type_to_x", plotTypeName));

				}

				
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(resident, e.getMessage());
			}
			
		} else {

			player.sendMessage(ChatTools.formatTitle("/plot group"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "add | new | create", "[name]", "Ex: /plot group new ExpensivePlots"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "remove", "", "Removes a plot from the specified group."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "rename", "[newName]", "Renames the group you are standing in."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "set", "...", "Ex: /plot group set perm resident on."));
			player.sendMessage(ChatTools.formatCommand("/plot group", "toggle", "...", "Ex: /plot group toggle [pvp|fire|mobs]"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "forsale | fs", "[price]", "Ex: /plot group forsale 50"));
			player.sendMessage(ChatTools.formatCommand("/plot group", "notforsale | nfs", "", "Ex: /plot group notforsale"));

			if (townBlock.hasPlotObjectGroup())
				TownyMessaging.sendMsg(player, Translation.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			
			return true;
		}
		
		return false;
	}

}
