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
import static com.github.intangir.GalacticChat.Utility.join;

@SuppressWarnings("deprecation")
public class Chatter extends Config {

	static public Map<String, Chatter> chatters = new HashMap<String, Chatter>();
	static public Map<String, Set<Chatter>> activeChannels = new HashMap<String, Set<Chatter>>();
	static private MainConfig config;
	static private Logger log;

	private Map<String, String> channels;
	private String focus;
	private boolean censoring;
	private boolean hidden;
	private Set<String> ignoring;
	private Set<String> banned;
	private transient Map<String, String> aliases;
	private transient CommandSender player;
	private transient String invited;
	private transient String replyTo;
	private transient String retellTo;

	public Chatter(GalacticChat plugin, CommandSender player) {
		CONFIG_FILE = new File(plugin.getDataFolder(), "players/" + player.getName() + ".yml");
		
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
		focus = config.getDefaultFocus();
		invited = null;
		censoring = true;
		replyTo = null;
		retellTo = null;
		hidden = false;
		ignoring = new HashSet<String>();
		banned = new HashSet<String>();
		

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

		// load players channels config
    	Chatter joining = new Chatter(plugin, p);
    	joining.init();
    	chatters.put(p.getName(), joining);

    	joining.rejoin();
	}
	
	static public void logOut(CommandSender p) {
		chatters.get(p.getName()).save();

    	// remove the player from all the active channels
		chatters.get(p.getName()).leaveAll();
		chatters.remove(p.getName());
	}
	
	static public Chatter find(CommandSender p) {
		return chatters.get(p.getName());
	}

	static public Chatter find(String p) {
		return chatters.get(p);
	}

	// static version for debug and console
	public void channelInfo(String channel) {
		channel = activeChannel(channel);
		if(channel == null) {
			player.sendMessage(config.getDefaultColor() + "Invalid channel or focus.");
		} else {
			List<String> members = new ArrayList<String>();
			for(Chatter chatter: activeChannels.get(channel)) {
				members.add(chatter.player.getName());
			}
			player.sendMessage(String.format("%sUsers in channel [%s. %s] %s", config.getDefaultColor(), channels.get(channel), channel, join(members, ", ")));
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
				debug("Determined target " + target);
			}
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
			} else {
				player.sendMessage(ChatColor.RED + "Usage: /join <channel> <optional-alias>");
				return;
			}
		}
		if(config.getLocalChannels().contains(channel.toLowerCase())) {
			// they are trying to join a local channel. set focus
			player.sendMessage(config.getDefaultColor() + "Focused Channel Local");
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
			player.sendMessage(ChatColor.RED + "You are in too many channels already. Perhaps you should /leave some.");
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

		join(channel, alias);
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
		
		rawSend(channel, player.getName(), String.format("%%s%s joined channel [%%s. %s]", player.getName(), channel));
	}
	
	public void leave(String channel) {
		if(channel == null) {
			channel = getChannel(focus);
			if(channel == null) {
				player.sendMessage(ChatColor.RED + "You have no valid focus channel set.");
				return;
			}
		}
		
		channel = getChannel(channel);
		if(channel == null) {
			player.sendMessage(ChatColor.RED + "You are not in any channels by that name or alias.");
		} else {
			rawSend(channel, player.getName(), String.format("%%s%s left channel [%%s. %s]", player.getName(), channel));

			// actual leaving
			activeChannels.get(channel).remove(this);
			channels.remove(channel);
			save();
		}
	}
	
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
				player.sendMessage(config.getDefaultColor() + "Focused Channel [" + channels.get(channel) + ". " + channel + "]");
				save();
			}
		}
		return true;
	}
	
	// returns the channel to message to given an alias, a channel, or null
	// returns the valid channel name or null if it fails or resorts to local
	public String activeChannel(String channel) {
		debug("determining active with " + channel + " and " + focus);

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
				return false;
			} else {
				// or just alert them of invalid focus
				player.sendMessage(ChatColor.RED + "You have no valid focus channel set.");
				return true;
			}
		}
		
		// send to channel!
		rawSend(channel, player.getName(), String.format("%%s[%%s]%s<%s> %s", ChatColor.WHITE, player.getName(), message));
		return true;
	}
	
	// raw send to channel
	public void rawSend(String channel, String sender, String rawText) {
		String censoredText = config.censor(rawText);
		
		debug("sending rawText "+ rawText);
		
		// send to all of the people in the channel!
		for(Chatter c: activeChannels.get(channel)) {
			c.receive(channel, sender, rawText, censoredText);
		}
	}
	
	// receive channel chat
	public void receive(String channel, String sender, String message, String censored) {
		if(!ignored(sender)) {
			if(censoring) {
				message = censored;
			}
			player.sendMessage(String.format(message, config.getDefaultColor(), channels.get(channel)));
		}
	}
	
	// check if they are ignored
	public boolean ignored(String sender) {
		return false;
	}
	
	public void ignore(String other, Boolean mode) {
		if(mode.booleanValue() == true) {
			ignoring.add(other);
			player.sendMessage(config.getDefaultColor() + other + " is being ignored.");
		} else if(mode.booleanValue() == false) {
			ignoring.remove(other);
			player.sendMessage(config.getDefaultColor() + other + " is not being ignored.");
		} else {
			ignore(other, !ignoring.contains(other));
		}
	}
	
	// sends a private message
	public void tell(String other, String message) {
		Chatter chatter = chatters.get(other);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "There is no player by that name online.");
		} else {
			message(chatter, player.getName(), message);
			retellTo = other;
		}
	}
	
	public void retell(String message) {
		Chatter chatter = chatters.get(retellTo);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "No one to retell to.");
		} else {
			message(chatter, player.getName(), message);
		}
	}

	public void reply(String message) {
		Chatter chatter = chatters.get(replyTo);
		if(chatter == null) {
			player.sendMessage(ChatColor.RED + "No one to reply to.");
		} else {
			message(chatter, player.getName(), message);
		}
	}
	
	// message a player
	public void message(Chatter other, String sender, String message) {
		player.sendMessage(config.getTellToColor() + "<to " + other.player.getName() + "> " + message);
		other.receive(sender, config.getTellFromColor() + "<from " + sender + "> " + message);
	}
	
	// receive private message
	public void receive(String sender, String message) {
		if(!ignored(sender)) {
			if(censoring) {
				message = config.censor(message);
			}
			player.sendMessage(message);
			replyTo = sender;
		}
	}
	
	public void ban(String other, String channel) {
		
	}

}
