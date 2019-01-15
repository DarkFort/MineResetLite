package com.koletar.jj.mineresetlite.commands;

import com.koletar.jj.mineresetlite.*;
import com.vk2gpz.mineresetlite.util.WorldGuardUtil;
import com.vk2gpz.vklib.mc.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

import static com.koletar.jj.mineresetlite.Phrases.phrase;

/**
 * @author jjkoletar
 */
public class MineCommands {
	private MineResetLite plugin;
	private Map<Player, Location> point1;
	private Map<Player, Location> point2;
	
	public MineCommands(MineResetLite plugin) {
		this.plugin = plugin;
		point1 = new HashMap<>();
		point2 = new HashMap<>();
	}
	
	@Command(aliases = {"list", "l"},
			description = "Список всех шахт",
			permissions = {"mineresetlite.mine.list"},
			help = {"Список всех созданных в настоящее время шахт во всех мирах."},
			min = 0, max = 0, onlyPlayers = false)
	public void listMines(CommandSender sender, String[] args) {
		sender.sendMessage(phrase("mineList", StringTools.buildList(plugin.mines, "&c", "&d, ")));
	}
	
	@Command(aliases = {"pos1", "p1"},
			description = "Изменить первую точку выделения",
			help = {"Запустите эту команду, чтобы установить первую точку выделения на блок, на который вы смотрите.",
					"Используйте /mrl pos1 -feet, чтобы установить первую точку на место, на котором вы стоите."},
			usage = "(-feet)",
			permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
			min = 0, max = 1, onlyPlayers = true)
	public void setPoint1(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		if (args.length == 0) {
			//Use block being looked at
			point1.put(player, player.getTargetBlock(null, 100).getLocation());
			player.sendMessage(phrase("firstPointSet"));
			return;
		} else if (args[0].equalsIgnoreCase("-feet")) {
			//Use block being stood on
			point1.put(player, player.getLocation());
			player.sendMessage(phrase("firstPointSet"));
			return;
		}
		//Args weren't empty or -feet, bad args
		throw new InvalidCommandArgumentsException();
	}
	
	@Command(aliases = {"pos2", "p2"},
			description = "Изменить вторую точку выделения",
			help = {"Запустите эту команду, чтобы установить вторую точку выделения на блок, на который вы смотрите.",
					"Используйте /mrl pos2 -feet, чтобы установить вторую точку на место, на котором вы стоите."},
			usage = "(-feet)",
			permissions = {"mineresetlite.mine.create", "mineresetlite.mine.redefine"},
			min = 0, max = 1, onlyPlayers = true)
	public void setPoint2(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		if (args.length == 0) {
			//Use block being looked at
			point2.put(player, player.getTargetBlock(null, 100).getLocation());
			player.sendMessage(phrase("secondPointSet"));
			return;
		} else if (args[0].equalsIgnoreCase("-feet")) {
			//Use block being stood on
			point2.put(player, player.getLocation());
			player.sendMessage(phrase("secondPointSet"));
			return;
		}
		//Args weren't empty or -feet, bad args
		throw new InvalidCommandArgumentsException();
	}
	
	@Command(aliases = {"create", "save"},
			description = "Создать шахту либо из вашего выделения WorldEdit, либо вручную указав точки",
			help = {"При условии, что у вас есть выделение сделанный с помощью WorldEdit или выделения точек с помощью MRL,",
					"будет создана пустая шахта. Эта шахта не будет иметь композиции и настроек по умолчанию."},
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.create"},
			min = 1, max = -1, onlyPlayers = true)
	public void createMine(CommandSender sender, String[] args) {
		//Find out how they selected the region
		Player player = (Player) sender;
		World world = null;
		Vector p1 = null;
		Vector p2 = null;
		//Native selection techniques?
		if (point1.containsKey(player) && point2.containsKey(player)) {
			world = point1.get(player).getWorld();
			if (!world.equals(point2.get(player).getWorld())) {
				player.sendMessage(phrase("crossWorldSelection"));
				return;
			}
			p1 = point1.get(player).toVector();
			p2 = point2.get(player).toVector();
		}
		Object[] selections = WorldGuardUtil.getSelection(plugin, player);
		if (selections != null) {
			world = (World)selections[0];
			p1 = (Vector)selections[1];
			p2 = (Vector)selections[2];
		}
		
		if (p1 == null) {
			player.sendMessage(phrase("emptySelection"));
			return;
		}
		//Construct mine name
		String name = StringTools.buildSpacedArgument(args);
		//Verify uniqueness of mine name
		Mine[] mines = plugin.matchMines(name);
		if (mines.length > 0) {
			player.sendMessage(phrase("nameInUse", name));
			return;
		}
		//Sort coordinates
		if (p1.getX() > p2.getX()) {
			//Swap
			double x = p1.getX();
			p1.setX(p2.getX());
			p2.setX(x);
		}
		if (p1.getY() > p2.getY()) {
			double y = p1.getY();
			p1.setY(p2.getY());
			p2.setY(y);
		}
		if (p1.getZ() > p2.getZ()) {
			double z = p1.getZ();
			p1.setZ(p2.getZ());
			p2.setZ(z);
		}
		//Create!
		Mine newMine = new Mine(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(), p2.getBlockX(), p2.getBlockY(), p2.getBlockZ(), name, world);
		plugin.mines.add(newMine);
		player.sendMessage(phrase("mineCreated", newMine));
		plugin.buffSave();
	}
	
	@Command(aliases = {"info", "i"},
			description = "Список информации о шахте",
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.info"},
			min = 1, max = -1, onlyPlayers = false)
	public void mineInfo(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMInes(sender, mines)) return;
		sender.sendMessage(phrase("mineInfoName", mines[0]));
		sender.sendMessage(phrase("mineInfoWorld", mines[0].getWorld()));
		//Build composition list
		StringBuilder csb = new StringBuilder();
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			csb.append(entry.getValue() * 100);
			csb.append("% ");
			csb.append(MaterialUtil.getMaterial("" + entry.getKey().getBlockId()).toString());
			if (entry.getKey().getData() != 0) {
				csb.append(":");
				csb.append(entry.getKey().getData());
			}
			csb.append(", ");
		}
		if (csb.length() > 2) {
			csb.delete(csb.length() - 2, csb.length() - 1);
		}
		sender.sendMessage(phrase("mineInfoComposition", csb));
		if (mines[0].getResetDelay() != 0) {
			sender.sendMessage(phrase("mineInfoResetDelay", mines[0].getResetDelay()));
			sender.sendMessage(phrase("mineInfoTimeUntilReset", mines[0].getTimeUntilReset()));
		}
		sender.sendMessage(phrase("mineInfoSilence", mines[0].isSilent()));
		if (mines[0].getResetWarnings().size() > 0) {
			sender.sendMessage(phrase("mineInfoWarningTimes", StringTools.buildList(mines[0].getResetWarnings(), "", ", ")));
		}
		if (mines[0].getSurface() != null) {
			sender.sendMessage(phrase("mineInfoSurface", mines[0].getSurface()));
		}
		if (mines[0].getFillMode()) {
			sender.sendMessage(phrase("mineInfoFillMode"));
		}
	}
	
	private boolean invalidMInes(CommandSender sender, Mine[] mines) {
		if (mines.length > 1) {
			sender.sendMessage(phrase("tooManyMines", plugin.toString(mines)));
			return true;
		} else if (mines.length == 0) {
			sender.sendMessage(phrase("noMinesMatched"));
			return true;
		}
		return false;
	}
	
	@Command(aliases = {"set", "add", "+"},
			description = "Установить процент блока в шахте",
			help = {"Эта команда всегда будет перезаписывать текущий процент для указанного блока,",
					"если процент уже был установлен. Вы не можете установить процент любого конкретного",
					"блока, так что процент будет составлять более 100%."},
			usage = "<mine name> <block>:(data) <percentage>%",
			permissions = {"mineresetlite.mine.composition"},
			min = 3, max = -1, onlyPlayers = false)
	public void setComposition(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
		if (invalidMInes(sender, mines)) return;
		//Match material
		String[] bits = args[args.length - 2].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (m == null) {
			sender.sendMessage(phrase("unknownBlock"));
			return;
		}
		if (!m.isBlock()) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}
		//Parse percentage
		String percentageS = args[args.length - 1];
		if (!percentageS.endsWith("%")) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}
		StringBuilder psb = new StringBuilder(percentageS);
		psb.deleteCharAt(psb.length() - 1);
		double percentage;
		try {
			percentage = Double.valueOf(psb.toString());
		} catch (NumberFormatException nfe) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}
		if (percentage > 100 || percentage <= 0) {
			sender.sendMessage(phrase("badPercentage"));
			return;
		}
		percentage = percentage / 100; //Make it a programmatic percentage
		SerializableBlock block = new SerializableBlock(m.getId(), data);
		Double oldPercentage = mines[0].getComposition().get(block);
		double total = 0;
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			if (!entry.getKey().equals(block)) {
				total += entry.getValue();
			} else {
				block = entry.getKey();
			}
		}
		total += percentage;
		if (total > 1) {
			sender.sendMessage(phrase("insaneCompositionChange"));
			if (oldPercentage == null) {
				mines[0].getComposition().remove(block);
			} else {
				mines[0].getComposition().put(block, oldPercentage);
			}
			return;
		}
		mines[0].getComposition().put(block, percentage);
		sender.sendMessage(phrase("mineCompositionSet", mines[0], percentage * 100, block, (1 - mines[0].getCompositionTotal()) * 100));
		plugin.buffSave();
	}
	
	@Command(aliases = {"unset", "remove", "-"},
			description = "Удалить блок из состава шахты",
			usage = "<mine name> <block>:(data)",
			permissions = {"mineresetlite.mine.composition"},
			min = 2, max = -1, onlyPlayers = false)
	public void unsetComposition(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 1));
		if (invalidMInes(sender, mines)) return;
		//Match material
		String[] bits = args[args.length - 1].split(":");
		Material m = plugin.matchMaterial(bits[0]);
		if (m == null) {
			sender.sendMessage(phrase("unknownBlock"));
			return;
		}
		if (!m.isBlock()) {
			sender.sendMessage(phrase("notABlock"));
			return;
		}
		byte data = 0;
		if (bits.length == 2) {
			try {
				data = Byte.valueOf(bits[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
		}
		//Does the mine contain this block?
		SerializableBlock block = new SerializableBlock(m.getId(), data);
		for (Map.Entry<SerializableBlock, Double> entry : mines[0].getComposition().entrySet()) {
			if (entry.getKey().equals(block)) {
				mines[0].getComposition().remove(entry.getKey());
				sender.sendMessage(phrase("blockRemovedFromMine", mines[0], block, (1 - mines[0].getCompositionTotal()) * 100));
				return;
			}
		}
		sender.sendMessage(phrase("blockNotInMine", mines[0], block));
		plugin.buffSave();
	}
	
	@Command(aliases = {"reset", "r"},
			description = "Сбросить шахту",
			help = {"Если вы укажете аргумент -s, шахта автоматически сбросит настройки. Сброс, вызванный этой",
					"командой, не будет показывать 1-минутное предупреждение, если эта шахта не помечена, чтобы",
					"всегда иметь предупреждение. Если состав шахты не равен 100%, состав будет",
					"заполняться воздухом до тех пор, пока общее количество не станет равным 100%."},
			usage = "<mine name> (-s)",
			permissions = {"mineresetlite.mine.reset"},
			min = 1, max = -1, onlyPlayers = false)
	public void resetMine(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args).replace(" -s", ""));
		if (invalidMInes(sender, mines)) return;
		if (args[args.length - 1].equalsIgnoreCase("-s")) {
			//Silent reset
			mines[0].reset();
		} else {
			MineResetLite.broadcast(phrase("mineResetBroadcast", mines[0], sender), mines[0]);
			mines[0].reset();
		}
	}
	
	@Command(aliases = {"flag", "f"},
			description = "Установить различные свойства шахты, в том числе автоматический сброс",
			help = {"Доступные флаги:",
					"resetPercent: Целое число (0 <x <100), указывающее процент добытых блоков, инициирующих сброс. Установите -1, чтобы отключить автоматический сброс процентов.",
					"resetDelay: Целое число минут, указывающее время между автоматическим сбросом. Установите 0, чтобы отключить автоматический сброс.",
					"resetWarnings: Разделенный запятыми список целых минут для предупреждения перед автоматическим сбросом. Предупреждения должны быть меньше, чем задержка сброса.",
					"surface: Блок, который при сбросе покроет всю верхнюю поверхность шахты, скрывая поверхность руды. Установите поверхность на воздух, чтобы очистить значение.",
					"fillMode: Альтернативный алгоритм сброса, который будет только \"сбрасывать\" воздушные блоки внутри вашей шахты. Установите true или false.",
					"fillMode: Альтернативный алгоритм сброса, который будет только \"сбрасывать\" воздушные блоки внутри вашей шахты. Установите true или false.",
					"isSilent: Логическое значение (true или false) того, должна ли эта шахта передавать уведомление о сбросе, когда оно сбрасывается *автоматически*"},
			usage = "<mine name> <setting> <value>",
			permissions = {"mineresetlite.mine.flag"},
			min = 3, max = -1, onlyPlayers = false)
	public void flag(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 2));
		if (invalidMInes(sender, mines)) return;
		String setting = args[args.length - 2];
		String value = args[args.length - 1];
		if (setting.equalsIgnoreCase("resetEvery") || setting.equalsIgnoreCase("resetDelay")) {
			int delay;
			try {
				delay = Integer.valueOf(value);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("badResetDelay"));
				return;
			}
			if (delay < 0) {
				sender.sendMessage(phrase("badResetDelay"));
				return;
			}
			mines[0].setResetDelay(delay);
			if (delay == 0) {
				sender.sendMessage(phrase("resetDelayCleared", mines[0]));
			} else {
				sender.sendMessage(phrase("resetDelaySet", mines[0], delay));
			}
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("resetWarnings") || setting.equalsIgnoreCase("resetWarning")) {
			String[] bits = value.split(",");
			List<Integer> warnings = mines[0].getResetWarnings();
			List<Integer> oldList = new LinkedList<>(warnings);
			warnings.clear();
			for (String bit : bits) {
				try {
					warnings.add(Integer.valueOf(bit));
				} catch (NumberFormatException nfe) {
					sender.sendMessage(phrase("badWarningList"));
					return;
				}
			}
			//Validate warnings
			for (Integer warning : warnings) {
				if (warning >= mines[0].getResetDelay()) {
					sender.sendMessage(phrase("badWarningList"));
					mines[0].setResetWarnings(oldList);
					return;
				}
			}
			if (warnings.contains(0) && warnings.size() == 1) {
				warnings.clear();
				sender.sendMessage(phrase("warningListCleared", mines[0]));
				return;
			} else if (warnings.contains(0)) {
				sender.sendMessage(phrase("badWarningList"));
				mines[0].setResetWarnings(oldList);
				return;
			}
			sender.sendMessage(phrase("warningListSet", mines[0]));
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("surface")) {
			//Match material
			String[] bits = value.split(":");
			Material m = plugin.matchMaterial(bits[0]);
			if (m == null) {
				sender.sendMessage(phrase("unknownBlock"));
				return;
			}
			if (!m.isBlock()) {
				sender.sendMessage(phrase("notABlock"));
				return;
			}
			byte data = 0;
			if (bits.length == 2) {
				try {
					data = Byte.valueOf(bits[1]);
				} catch (NumberFormatException nfe) {
					sender.sendMessage(phrase("unknownBlock"));
					return;
				}
			}
			if (m.equals(Material.AIR)) {
				mines[0].setSurface(null);
				sender.sendMessage(phrase("surfaceBlockCleared", mines[0]));
				plugin.buffSave();
				return;
			}
			SerializableBlock block = new SerializableBlock(m.getId(), data);
			mines[0].setSurface(block);
			sender.sendMessage(phrase("surfaceBlockSet", mines[0]));
			plugin.buffSave();
			return;
		} else if (setting.equalsIgnoreCase("fill") || setting.equalsIgnoreCase("fillMode")) {
			//Match true or false
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
				mines[0].setFillMode(true);
				sender.sendMessage(phrase("fillModeEnabled"));
				plugin.buffSave();
				return;
			} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
				mines[0].setFillMode(false);
				sender.sendMessage(phrase("fillModeDisabled"));
				plugin.buffSave();
				return;
			}
			sender.sendMessage(phrase("invalidFillMode"));
		} else if (setting.equalsIgnoreCase("isSilent") || setting.equalsIgnoreCase("silent") || setting.equalsIgnoreCase("silence")) {
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("enabled")) {
				mines[0].setSilence(true);
				sender.sendMessage(phrase("mineIsNowSilent", mines[0]));
				plugin.buffSave();
				return;
			} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("disabled")) {
				mines[0].setSilence(false);
				sender.sendMessage(phrase("mineIsNoLongerSilent", mines[0]));
				plugin.buffSave();
				return;
			}
			sender.sendMessage(phrase("badBoolean"));
		} else if (setting.equalsIgnoreCase("resetPercent")) {
			StringBuilder psb = new StringBuilder(value);
			psb.deleteCharAt(psb.length() - 1);
			double percentage;
			try {
				percentage = Double.valueOf(psb.toString());
			} catch (NumberFormatException nfe) {
				sender.sendMessage(phrase("badPercentage"));
				return;
			}
			if (percentage > 100 || percentage <= 0) {
				sender.sendMessage(phrase("badPercentage"));
				return;
			}
			percentage = percentage / 100; //Make it a programmatic percentage
			mines[0].setResetPercent(percentage);
			
			if (percentage < 0) {
				sender.sendMessage(phrase("resetDelayCleared", mines[0]));
			} else {
				sender.sendMessage(phrase("resetPercentageSet", mines[0], (int) (percentage * 100)));
			}
			plugin.buffSave();
			return;
		}
		sender.sendMessage(phrase("unknownFlag"));
	}
	
	@Command(aliases = {"erase"},
			description = "Полностью удалить шахту",
			help = {"Как и большинство данных, убедитесь, что вам не нужно ничего восстанавливать с этой шахты, прежде чем удалить ее."},
			usage = "<mine name>",
			permissions = {"mineresetlite.mine.erase"},
			min = 1, max = -1, onlyPlayers = false)
	public void erase(CommandSender sender, String[] args) {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMInes(sender, mines)) return;
		plugin.eraseMine(mines[0]);
		sender.sendMessage(phrase("mineErased", mines[0]));
	}
	
	@Command(aliases = {"reschedule"},
			description = "Синхронизация автоматический сброс всех шахт",
			help = {"Эта команда установит 'время начала' сброса шахты в ту же точку."},
			usage = "",
			permissions = {"mineresetlite.mine.flag"},
			min = 0, max = 0, onlyPlayers = false)
	public void reschedule(CommandSender sender, String[] args) {
		for (Mine mine : plugin.mines) {
			mine.setResetDelay(mine.getResetDelay());
		}
		plugin.buffSave();
		sender.sendMessage(phrase("rescheduled"));
	}
	
	@Command(aliases = {"tp", "teleport"}, description = "Телепортация к указанной шахте", help = {"Эта команда телепортирует вас в центр указанной шахты или в точку телепортации, если это указано."}, usage = "<mine name>", permissions = {"mineresetlite.mine.tp"}, min = 1, max = -1, onlyPlayers = true)
	public void teleport(CommandSender sender, String[] args) {
		Mine mine = null;
		
		for (Mine aMine : plugin.mines) {
			if (aMine.getName().equalsIgnoreCase(args[0])) {
				mine = aMine;
			}
		}
		
		if (mine == null) {
			sender.sendMessage(phrase("noMinesMatched"));
			return;
		}
		
		mine.teleport((Player) sender);
	}
	
	@Command(aliases = {"settp", "stp"}, description = "Установить указанную точку спавна шахты", help = {"Эта команда установит точку спавна сброса указанной шахты туда, где вы стоите.", "Используйте /mrl removetp <имя шахты> для удаления точки спавна шахты."}, usage = "<mine name>", permissions = {"mineresetlite.mine.settp"}, min = 1, max = -1, onlyPlayers = true)
	public void setTP(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMInes(sender, mines)) return;
		mines[0].setTp(player.getLocation());
		plugin.buffSave();
		sender.sendMessage(phrase("tpSet", mines[0]));
	}
	
	@Command(aliases = {"removetp", "rtp"}, description = "Удалить указанную точку спавна шахты", help = {"Эта команда удалит точку спавна указанной шахты.", "Используйте /mrl removetp для удаления точки спавна шахты.", "Используйте /mrl settp, чтобы установить ее туда, где вы стоите."}, usage = "<mine name>", permissions = {"mineresetlite.mine.removetp"}, min = 1, max = -1, onlyPlayers = true)
	public void removeTP(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Player player = (Player) sender;
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args));
		if (invalidMInes(sender, mines)) return;
		mines[0].setTp(new Location(player.getWorld(), 0, -Integer.MAX_VALUE, 0));
		plugin.buffSave();
		sender.sendMessage(phrase("tpRemove", mines[0]));
	}
	
	@Command(aliases = {"addpotion", "addpot"}, description = "Добавить указанное зелье в шахте", help = {"Эта команда доставит указанное зелье в шахту, где вы стоите.", "Испольуйте /mrl removepot <имя шахты> <имя зелья>, чтобы убрать указанный эффект зелья из шахты."}, usage = "<mine name> <potionname:amplifier>", permissions = {"mineresetlite.mine.addpotion"}, min = 1, max = -1, onlyPlayers = true)
	public void addPot(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 1));
		if (invalidMInes(sender, mines)) return;
		mines[0].addPotion(args[args.length - 1]);
		plugin.buffSave();
		sender.sendMessage(phrase("potionAdded", args[args.length - 1], mines[0]));
	}
	
	@Command(aliases = {"removepotion", "removepot"}, description = "Удалить указанное зелье в шахте", help = {"Эта команда удалит указанное зелье в шахте, где вы стоите.", "Используйте /mrl removepot <potionname>, чтобы убрать указанный эффект зелья из шахты.", "Используйте /mrl addpot <имя шахты> <имя зелья:усиление>, чтобы добавить указанный эффект зелья в шахту."}, usage = "<mine name> <potionname>", permissions = {"mineresetlite.mine.removepotion"}, min = 1, max = -1, onlyPlayers = true)
	public void removePot(CommandSender sender, String[] args) throws InvalidCommandArgumentsException {
		Mine[] mines = plugin.matchMines(StringTools.buildSpacedArgument(args, 1));
		if (invalidMInes(sender, mines)) return;
		mines[0].removePotion(args[args.length - 1]);
		plugin.buffSave();
		sender.sendMessage(phrase("potionRemoved", args[args.length - 1], mines[0]));
	}
}
