package com.github.intangir.GalacticChat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.github.intangir.GalacticChat.Utility.join;

import lombok.Getter;

import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Config;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;

@Getter
public class MainConfig extends Config {

	@Comment("Channels everyone automatically joins on the first time, and their aliases")
	private Map<String, String> defaultChannels;

	@Comment("Default focus new players have when they first join or if they leave their existing focus")
	private String defaultFocus;

	@Comment("list of channels which pass through to the local server (lowercase)")
	private List<String> localChannels;

	@Comment("list of channels whos names are not allowed (lowercase)")
	private List<String> unusableChannels;

	@Comment("List of commands")
	private Map<String, String> commands;
	
	@Comment("max number of channels they can join")
	private Integer channelLimit;

	@Comment("default channel prefix color and join/focus/leave color")
	private String defaultColor;

	@Comment("private message sent color")
	private String tellToColor;

	@Comment("private message received color")
	private String tellFromColor;

	@Comment("Censor filters, replaced for people who have censoring on")
	private Map<String, String> censors;

	@Comment("Have chat fall back to local when the focus channel is invalid")
	private boolean localIfNoFocus;

	@Comment("Print debugging information")
	private boolean debug;

	public MainConfig(Plugin plugin) {
		CONFIG_FILE = new File(plugin.getDataFolder(), "config.yml");
		
		defaultChannels = new HashMap<String, String>();
		defaultChannels.put("Galactic", "g");
		
		defaultFocus = "Galactic";
		
		localChannels = new ArrayList<String>();
		localChannels.add("local");
		localChannels.add("l");
		localChannels.add("system");
		localChannels.add("s");
		
		unusableChannels = new ArrayList<String>();
		unusableChannels.add("help");
		
		commands = new HashMap<String, String>();
		commands.put("join", "join focus accept");
		commands.put("leave", "leave part quit");
		commands.put("invite", "invite");
		commands.put("censor", "censor censoring");
		commands.put("channel", "channel info");
		commands.put("ignore", "ignore mute squelch");
		commands.put("unignore", "unignore unmute unsquelch");
		commands.put("tell", "tell msg w");
		commands.put("reply", "reply r");
		commands.put("retell", "retell rt");
		commands.put("ban", "gcban gcunban gcpardon");
		commands.put("say", "say");
		
		channelLimit = 9;
		
		defaultColor = ChatColor.YELLOW.toString();
		tellToColor = ChatColor.DARK_GREEN.toString();
		tellFromColor = ChatColor.GREEN.toString();

		censors = new HashMap<String, String>();
		censors.put("fuck", "frak");
		censors.put("shit", "poop");
		censors.put("dick", "dork");
		censors.put("cock", "cork");
		censors.put("cunt", "creep");
		censors.put("nigg", "nagg");
		
		localIfNoFocus = true;
		
		debug = false;
	}
	
	private Pattern censorsPattern;
	
	@Override
	public void init() throws InvalidConfigurationException {
		super.init();
		
		String pattern = join(new ArrayList<String>(censors.keySet()), "|");
		
		censorsPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	}
	
	public String censor(String message) {
		
		Matcher m = censorsPattern.matcher(message);
	
		while(m.find()) {
			message = message.replaceAll(m.group(), censors.get(m.group().toLowerCase()));
		}
		return message;
	}
	
	public String getCommand(String key) {
		return commands.get(key).split(" ")[0];
	}
	
	public String[] getAliases(String key) {
		return commands.get(key).split(" ");
	}
}
