package com.github.intangir.GalacticChat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.cubespace.Yamler.Config.Config;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@SuppressWarnings("deprecation")
public class Chatter extends Config {

	static public Map<String, Chatter> chatters = new HashMap<String, Chatter>();
	static public Map<String, Set<Chatter>> activeChannels = new HashMap<String, Set<Chatter>>();
	static private MainConfig config;
	static private Logger log;

	private Map<String, String> channels;
	private Map<String, String> colors;
	private String focus;
	private boolean censoring;
	private boolean hidden;
	private Set<String> ignoring;
	private Set<String> banned;
	private transient Map<String, String> aliases;
	private transient CommandSender player;
	private transient String invited;
	private transient String invitedalias;
	private transient String replyTo;
	private transient String retellTo;
	private transient Map<String, Long> spamTime;
	private transient Map<String, String> spamRepeated;

	public Chatter(GalacticChat plugin, CommandSender player) {
		CONFIG_FILE = new File(plugin.getDataFolder(), "players/" + getUniqueId(player) + ".yml");
		
		this.player = player;
		
		if(config == null) {
			config = plugin.getConfig();
		}
		if(log == null) {
			log = plugin.getLogger();
		}

		// add defaults
		BiMap<String, String> bimap = HashBiMap.create(config.getDefaultChannels());
		channels = bimap;
		aliases = bimap.inverse();
		colors = new HashMap<String, String>();
		focus = config.getDefaultFocus();
		invited = null;
		invitedalias = null;
		censoring = true;
		replyTo = null;
		retellTo = null;
		hidden = false;
		ignoring = new HashSet<String>();
		banned = new HashSet<String>();
		spamTime = new HashMap<String, Long>();
		spamRepeated = new HashMap<String, String>();
	}
	
	@Override
	public void init() {
    	debug("Loading " + player.getName());
		try {
			super.init();
		} catch (InvalidConfigurationException e) {
			log.info("Couldn't Load profile for " + player.getName());
		}
    	// resync bimap
    	BiMap<String, String> bimap = HashBiMap.create(channels);
		channels = bimap;
		aliases = bimap.inverse();
	}
	
	@Override
	public void save() {
		debug("Saving " + player.getName());
		try {
			super.save();
		} catch (InvalidConfigurationException e) {
			log.info("Couldn't Save profile for " + player.getName());
		}
		// resync bimap
		BiMap<String, String> bimap = HashBiMap.create(channels);
		channels = bimap;
		aliases = bimap.inverse();
	}
	
	static public void debug(String message) {
		if(config.isDebug()) {
			log.info(message);
		}
	}
	
	static public void logOn(GalacticChat plugin, CommandSender p) {

		// sometimes the logout is missed (i think its when server kicks them?)
		if(chatters.containsKey(p.getName())) {
			logOut(p);
		}

		// load players channels config
    	Chatter joining = new Chatter(plugin, p);
    	joining.init();
    	chatters.put(p.getName(), joining);

    	joining.rejoin();
	}
	
	static public void logOut(CommandSender p) {
		find(p.getName()).save();

    	// remove the player from all the active channels
		find(p.getName()).leaveAll();
		chatters.remove(p.getName());
	}

	static public String getDisplayName(CommandSender p, String color) {
		if(color == null) {
			color = config.getDefaultColor();
		}
		if(p instanceof ProxiedPlayer) {
			return ((ProxiedPlayer) p).getDisplayName() + color;
		} else {
			return color + p.getName();
		}
	}

	static public String getUniqueId(CommandSender p) {
		if(p instanceof ProxiedPlayer) {
			return ((ProxiedPlayer) p).getUniqueId().toString();
		} else {
			return p.getName();
		}
	}
	
	public String getColor(String channel) {
		if(!colors.containsKey(channel)) {
			colors.put(channel, getNextColor());
		}
		return colors.get(channel);
	}

	static public String tabComplete(String cursor) {
    	if(cursor.charAt(0) != '/')
			return null;

		String[] part = cursor.substring(1).split(" ");
    	if(part.length == 2 && config.getCompletable().contains(part[0])) {
    		debug("tab completing " + cursor);
    		// its a completable command, see if they are typing a name
    		for(String name : chatters.keySet()) {
    			if(part[1].length() < name.length() && part[1].equalsIgnoreCase(name.substring(0, part[1].length())) && !chatters.get(name).hidden) {
					return name;
    			}
    		}
    	}
    	return null;
	}
	
	static public Chatter find(CommandSender p) {
		return chatters.get(p.getName());
	}

	static public Chatter find(String p) {
		return chatters.get(p);
	}

	// static version for debug and console
	public void channelInfo(String channel) {
		if(channel == null) {
			for(String chan : channels.keySet()) {
				channelInfo(chan);
			}
		} else {
			// determine channel
			channel = activeChannel(channel);
			if(channel == null) {
				player.sendMessage(ChatColor.RED + "Invalid channel or focus.");
			} else {
				List<String> members = new ArrayList<String>();
				for(Chatter chatter: activeChannels.get(channel)) {
					if(!chatter.hidden) {
						members.add(getDisplayName(chatter.player, null));
					}
				}
				player.sendMessage(String.format("%sUsers in channel [%s. %s] %s", getColor(channel), channels.get(channel), channel, Utility.join(members, ", ")));
			}
		}
	}
	
	// ban a player from a channel
	public void ban(String other, String channel) {
		// determine channel
		channel = activeChannel(channel);
		if(channel == null) {
			player.sendMessage(ChatColor.RED + "Invalid channel or focus.");
			return;
		}
			
		// get the player
		Chatter chatter = find(other);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "There is no player by that name online.");
			return;
		}
		
		if(chatter.banned.contains(channel)) {
			// already banned, toggle it to unbanned
			chatter.banned.remove(channel);
			player.sendMessage(String.format("%sUnbanned %s from [%s. %s]", getColor(channel), other, channels.get(channel), channel));

		} else {
			// add to the banned list
			chatter.banned.add(channel);
			player.sendMessage(String.format("%sBanned %s from [%s. %s]", getColor(channel), other, channels.get(channel), channel));
			
			// remove them if they are in it
			if(chatter.channels.containsKey(channel)) {
				chatter.leave(channel);
			}
			chatter.save();
		}
	}
	
	public void invite(String other, String channel) {
		// determine channel
		channel = activeChannel(channel);
		if(channel == null) {
			player.sendMessage(ChatColor.RED + "Invalid channel or focus.");
			return;
		}
			
		// get the player
		Chatter chatter = find(other);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "There is no player by that name online.");
			return;
		}
		
		// check if they are already in it..
		if(chatter.channels.containsKey(channel)) {
			player.sendMessage(ChatColor.RED + other + " is already in the channel.");
		} else { 
			// send the invite, and tell the two parties
			player.sendMessage(String.format("%sInvited %s to %s[%s. %s]", config.getDefaultColor(), other, getColor(channel), channels.get(channel), channel));
			if(!chatter.ignored(this)) {
				chatter.invited = channel;
				chatter.invitedalias = channels.get(channel);
				chatter.player.sendMessage(String.format("%s%s invited you to [%s. %s], type /%s to accept.", player.getName(), config.getDefaultColor(), channels.get(channel), channel, config.getCommand("join")));
			}
		}
	}
	
	// parses message for either chat commands, or just plain chat
	public boolean parseChat(String message) {
		// handle channel prefix commands
		if(message.charAt(0) == '/') { 
			String[] part = message.substring(1).split(" ", 2);
			debug("looking up command " + part[0]);
			String target = part[0];
			if(config.getLocalChannels().contains(target.toLowerCase())) {
				// let local channel selection count as a valid target so we can setFocus
				target = target.toLowerCase();
			} else {
				target = getChannel(target);
			}
			debug("Determined target " + target);
			if(target == null) {
				// this command must not be a chat alias (probably for local server)
				return false;
			}
			if(part.length == 1) {
				return setFocus(target);
			} else {
				return send(part[1], target);
			}
		} else {
			return send(message, null);
		}
	}
	
	// join the player to all of the channels he was formerly in
	public void rejoin() {
		for(Map.Entry<String, String> e : channels.entrySet()) {
    		join(e.getKey(), e.getValue());
    	}
	}
	
	public void leaveAll() {
		for(Map.Entry<String, String> e : channels.entrySet()) {
			if(!hidden) {
				try {
					rawSend(e.getKey(), String.format("%s%%s left channel [%%s. %s]", getDisplayName(player, null), e.getKey()));
				} catch(java.util.ConcurrentModificationException ignore) {}
			}
			activeChannels.get(e.getKey()).remove(this);
    	}
	}
	
	// parses the join command
	public void parseJoin(String channel, String alias) {
		// people will use this as an all purpose join/focus/accept/change alias command, so check for lots of cases 
		if(channel == null) {
			// responding to an invite hopefully, otherwise misusing it
			if(invited != null) {
				channel = invited;
				alias = invitedalias;
			} else {
				player.sendMessage(ChatColor.RED + "Usage: /join <channel> <optional-alias>");
				return;
			}
		}
		if(config.getLocalChannels().contains(channel.toLowerCase())) {
			// they are trying to join a local channel. set focus
			player.sendMessage(config.getDefaultColor() + "Focused Channel [l. Local]");
			setFocus(channel);
			return;
		} else if(channels.containsKey(channel) && (channels.get(channel).equals(alias) || alias == null)) {
			// if already in this channel, set it as their focus instead
			setFocus(channel);
			return;
		} else if(config.getUnusableChannels().contains(channel.toLowerCase()) || (alias != null && config.getUnusableChannels().contains(alias.toLowerCase()))) {
			player.sendMessage(ChatColor.RED + "Unusable channel or alias name.");
			return;
		} else if(banned.contains(channel)) {
			player.sendMessage(ChatColor.RED + "You are banned from that channel.");
			return;
		} else if(aliases.containsKey(channel)) {
			// they are trying to join an alias, set it as their focus instead
			setFocus(channel);
			return;
		} else if(channels.size() >= 9) {
			// if in too many channels, tell them to leave some
			player.sendMessage(ChatColor.RED + "You are in too many channels already. Perhaps you should /" + config.getCommand("leave") + " some.");
			return;
		}
		
		// verify alias is not already in use
		if(alias != null) {
			String existing = aliases.get(alias);
			//debug("evaluating alias channel " + channel + " alias " + alias + " existing " + existing);
			if(existing != null && !existing.equals(channel)) {
				player.sendMessage(config.getDefaultColor() + "Channel alias " + alias + " already in use, auto assigning a new alias");
				alias = null;
			}
		}
		
		// find a valid alias
		if(alias == null) {
			alias = alias(channel);
		}

		// finally join
		join(channel, alias);
		if(config.isFocusOnUse()) {
			focus = channel;
		}
		save();
	}
	
	public void join(String channel, String alias) {
		debug(player.getName() + " joining channel " + channel + " alias " + alias);

		// create new activeChannel if it doesn't already exist
		if(!activeChannels.containsKey(channel)) {
			activeChannels.put(channel, new HashSet<Chatter>());
		}

		// join the active channel
		activeChannels.get(channel).add(this);

		// make sure its in your own list (redundancy should be harmless)
		channels.put(channel, alias);
		
		if(hidden) {
			player.sendMessage(String.format("%s joined channel [%s. %s] (hidden)", getDisplayName(player, getColor(channel)), channels.get(channel), channel));
		} else {
			rawSend(channel, String.format("%s%%s joined channel [%%s. %s]", getDisplayName(player, getColor(channel)), channel));
		}
	}
	
	// leaves channel
	public void leave(String channel) {
		channel = activeChannel(channel);
		
		if(channel == null) {
			player.sendMessage(ChatColor.RED + "Invalid Channel or focus.");
			return;
		}
		
		if(hidden) {
			player.sendMessage(String.format("%s left channel [%s. %s] (hidden)", getDisplayName(player, getColor(channel)), channels.get(channel), channel));
		} else {
			rawSend(channel, String.format("%s%%s left channel [%%s. %s]", getDisplayName(player, null), channel));
		}

		// actual leaving
		activeChannels.get(channel).remove(this);
		channels.remove(channel);
		colors.remove(channel);
		save();
	}
	
	// toggles censoring mode
	public void censor(String mode) {
		
		if(mode == null) {
			censoring = !censoring;
		} else {
			
			switch(mode.toLowerCase()) {
			case "off":
			case "false":
				censoring = false;
				break;
				
			case "on":
			case "true":
				censoring = true;
				break;
				
			default:
				censoring = !censoring;
				break;
			}
		}
		
		player.sendMessage(config.getDefaultColor() + "Censoring is now " + (censoring ? "on" : "off"));
		
	}
	
	// sets the color for your current channel
	public void color(String c) {
		
		ChatColor color;
		try {
			color = ChatColor.valueOf(c.toUpperCase());
		} catch (IllegalArgumentException ignore) {
			player.sendMessage(ChatColor.RED + "Unknown Color " + c);
			return;
		}
		String channel = activeChannel(null);
		
		if(channel == null) {
			player.sendMessage(ChatColor.RED + "Invalid Channel or focus.");
			
		} else {
			colors.put(channel, color.toString());
			player.sendMessage(String.format("%sColored Channel [%s. %s]", getColor(channel), channels.get(channel), channel));
		}
	}
	
	public String getNextColor() {
		String colorpriority = config.getColorPriority();

		// determine the color to use
		for(String color : colors.values()) {
			// move a used color to back of list
			colorpriority = colorpriority.replace("" + color.charAt(1), "") + color.charAt(1);
		}
		return "" + ChatColor.COLOR_CHAR + colorpriority.charAt(0);
	}

	// gets a channel alias, or creates a new one
	public String alias(String channel) {
		String alias = channels.get(channel);
		if(alias == null) {
			for(Integer a = 1; a <= config.getChannelLimit(); a++) {
				debug("evaluating " + a + " as an available alias");
				if(!aliases.containsKey(a.toString())) {
					debug("returning " + a.toString() + " " + a);
					return a.toString();
				} else {
					debug("mustve contained it");
				}
			}
		}
		return alias;
	}
	
	// tries to resolve the target to a channel
	public String getChannel(String target) {
		if(channels.containsKey(target)) {
			return target;
		}

		for(Map.Entry<String, String> e : aliases.entrySet()) {
			debug("Aliases: " + e.getKey() +  ":" + e.getValue());
		}

		for(Map.Entry<String, String> e : channels.entrySet()) {
			debug("Channel: " + e.getKey() +  ":" + e.getValue());
		}

		return aliases.get(target);
	}

	// sets the default chat target, returns true if the event was fully 'handled'
	public boolean setFocus(String target) {
		if(config.getLocalChannels().contains(target.toLowerCase())) {
			focus = target.toLowerCase();
			save();
			return false;
		} else {
			String channel = getChannel(target);
			if(channel == null) {
				player.sendMessage(ChatColor.RED + "You are not in any channels by that name or alias.");
			} else {
				focus = channel;
				player.sendMessage(getColor(channel) + "Focused Channel [" + channels.get(channel) + ". " + channel + "]");
				save();
			}
		}
		return true;
	}
	
	// returns the channel to message to given an alias, a channel, or null
	// returns the valid channel name or null if it fails or resorts to local
	public String activeChannel(String channel) {
		debug("determining active with channel " + channel + " and focus " + focus);

		// verify you have a valid target
		if(channel == null) {
			// use focus if channel is not set
			channel = focus;
		}
		
		// if its a local channel, fall out and let it pass through to their server 
		if(config.getLocalChannels().contains(channel)) {
			return null;
		}
		
		// resolve any aliases
		return getChannel(channel);
	}
	
	// sends text to a channel, or your focus, returns true if the event was fully 'handled'
	public boolean send(String message, String channel) {
		channel = activeChannel(channel);
		if(channel == null) {
			// not targetting a valid channel
			if(config.isLocalIfNoFocus()) {
				// default to local (fall out)
				if(config.isFocusOnUse()) {
					focus = channel;
				}
				return false;
			} else {
				// or just alert them of invalid focus
				player.sendMessage(ChatColor.RED + "You have no valid focus channel set.");
				return true;
			}
		}
		if(config.isFocusOnUse()) {
			focus = channel;
		}
		
		// send to channel!
		if(!isSpamming(message, channel)) {
			rawSend(channel, String.format(config.getChannelMsgFormat(), ChatColor.WHITE, getDisplayName(player, ""), ChatColor.WHITE.toString(), message.replace("%", "%%")));
		}
		return true;
	}
	
	// raw send to channel
	public void rawSend(String channel, String rawText) {
		String censoredText = config.censor(rawText);
		
		debug("sending rawText "+ rawText);
		
		// send to all of the people in the channel!
		for(Chatter c: activeChannels.get(channel)) {
			c.receive(channel, this, rawText, censoredText);
		}
	}
	
	// receive channel chat
	public void receive(String channel, Chatter sender, String message, String censored) {
		if(!ignored(sender)) {
			if(censoring) {
				message = censored;
			}
			player.sendMessage(String.format(message, getColor(channel), channels.get(channel)));
		}
	}
	
	// check if they are ignored
	public boolean ignored(Chatter sender) {
		return ignoring.contains(getUniqueId(sender.player));
	}
	
	// ignore a person
	public void ignore(String name, Boolean mode) {
		Chatter other = find(name);
		
		if(other == null) {
			player.sendMessage(ChatColor.RED + "There is no player by that name online.");
		} else if(mode == null) {
			ignore(other.player.getName(), !ignoring.contains(getUniqueId(other.player)));
		} else if(mode.booleanValue() == true) {
			ignoring.add(getUniqueId(other.player));
			player.sendMessage(getDisplayName(other.player, null) + " is being ignored.");
		} else {
			ignoring.remove(getUniqueId(other.player));
			player.sendMessage(getDisplayName(other.player, null) + " is not being ignored.");
		}
		save();
	}
	
	// sends a private message
	public void tell(String other, String message) {
		Chatter chatter = find(other);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "There is no player by that name online.");
		} else {
			message(chatter, message);
			retellTo = other;
		}
	}
	
	public void retell(String message) {
		Chatter chatter = find(retellTo);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "No one to retell to.");
		} else {
			message(chatter, message);
		}
	}

	public void reply(String message) {
		Chatter chatter = find(replyTo);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "No one to reply to.");
		} else {
			message(chatter, message);
		}
	}
	
	// message a player
	public void message(Chatter other, String message) {
		player.sendMessage(String.format(config.getTellToFormat(), config.getTellToColor(), getDisplayName(other.player, ""), config.getTellToColor(), message.replace("%", "%%")));
		other.receive(this, String.format(config.getTellFromFormat(), config.getTellFromColor(), getDisplayName(player, ""), config.getTellFromColor(), message.replace("%", "%%")));
	}
	
	// receive private message
	public void receive(Chatter sender, String message) {
		if(!ignored(sender)) {
			if(censoring) {
				message = config.censor(message);
			}
			player.sendMessage(message);
			replyTo = sender.player.getName();
		}
	}
	
	// checks for spamming
	public boolean isSpamming(String text, String channel) {
		final float nanos = 1000000000;
		boolean spamming = false;
		// protected channel
		debug("checking for spam to " + channel + " text: " + text);
		if(config.getProtectedChannels().contains(channel)) {
			// start counting
			long currTime = System.nanoTime();
			if(!spamTime.containsKey(channel))
				spamTime.put(channel, currTime);
			
			// get last delayed time
			long lastTime = spamTime.get(channel);
			
			debug("current time " + currTime + " lastTime  " + lastTime);
			// has the entire delay passed?
			if(currTime >= lastTime) {
				// so much time has passed the old time is irrelevant, update it to now
				debug("delay expired");
				lastTime = currTime;
			} else if(currTime < lastTime - config.getSpamThreshhold() * nanos) {
				// they are spamming, send warning
				player.sendMessage(ChatColor.RED + "You are sending to this channel too quickly!");
				spamming = true;
				
				// are they over the kick threshhold?
				if(currTime < lastTime - config.getSpamKickThreshold() * nanos) {
					leave(channel);
					
					// are they even over the ban threshold?
					if(currTime < lastTime - config.getSpamBanThreshold() * nanos) {
						banned.add(channel);
					}
				}
			}
			
			// now add up the delays based on context for next time
			lastTime += config.getSpamNormalDelay() * nanos;
			
			// all caps
			if(text.toUpperCase().equals(text)) {
				debug("penalizing for uppercase");
				lastTime += config.getSpamAllCapsDelay() * nanos;
			}
			
			// cussing
			if(!text.equals(config.censor(text))) {
				debug("penalizing for cussing");
				lastTime += config.getSpamCussDelay() * nanos;
			}
			
			// repeating
			if(text.equals(spamRepeated.get(channel))) {
				debug("penalizing for repeat");
				lastTime += config.getSpamRepeatDelay() * nanos;
			}
			
			// save info
			debug("delay " + lastTime);
			spamTime.put(channel, lastTime);
			if(!spamming) {
				spamRepeated.put(channel, text);
			}
			
		}
		return spamming;
	}
}
