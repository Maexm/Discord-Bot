package start;

import java.io.Console;
import java.io.File;

import com.google.gson.Gson;

import config.FileManager;
import config.MainConfig;
import config.SecretsConfig;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import exceptions.StartUpException;

public class StartUp {

	public static void main(String[] args) {

		System.out.println("STARTING MEGUMIN BOT");

		StartUp.loadMainConfig();
		Secrets secrets = StartUp.loadSecrets();

		// Retrieve debug info, if available
		if (args.length >= 1 && args[0].toUpperCase().equals("DEBUG") || args.length >= 2 && args[1].toUpperCase().equals("DEBUG")) {
			RuntimeVariables.isDebug = true;
		}

		// Retrieve token
		String TOKEN = "";
		if (secrets != null && secrets.getBotKey() != null && secrets.getBotKey() != ""){
			TOKEN = secrets.getLastBotKey();
		}
		else if (args.length >= 1 && !args[0].toUpperCase().equals("DEBUG")) {
			TOKEN = args[0];
		} else {
			System.out.println("Please enter a token (won't be displayed): ");
			Console console = System.console();
			TOKEN = new String(console.readPassword());
		}

		if (TOKEN == null || TOKEN.equals("")) {
			throw new StartUpException("Token missing");
		}
		//reactor.util.Loggers.useJdkLoggers();

		// Objects
		GlobalDiscordHandler[] discordHandlerWrapper = new GlobalDiscordHandler[1];

		final GatewayDiscordClient client = DiscordClientBuilder.create(TOKEN).build().login().block();

		// ########## On client login ##########

		client.getEventDispatcher().on(ReadyEvent.class).subscribe(ready -> {
			System.out.println("LOGGED IN AS: '" + ready.getSelf().getUsername() + "'");
			System.out.println("Currently member of " + ready.getGuilds().size() + " guilds");

			if(RuntimeVariables.firstLogin){
				client.updatePresence(Presence
					.doNotDisturb(Activity.playing("Starten")))
					.block();
				// Create entry point for event handling
				discordHandlerWrapper[0] = new GlobalDiscordHandler(ready, secrets);

				// Notify owner if debug
				if(RuntimeVariables.isDebug){
					client.getApplicationInfo()
					.flatMap(appInfo -> appInfo.getOwner())
					.flatMap(owner -> owner.getPrivateChannel())
					.flatMap(ownerChannel -> ownerChannel.createMessage("Debug Session aktiv!"))
					.block();
				}
				RuntimeVariables.firstLogin = false;
			}
			else{
				client.getApplicationInfo()
					.flatMap(appInfo -> appInfo.getOwner())
					.flatMap(owner -> owner.getPrivateChannel())
					.flatMap(ownerChannel -> ownerChannel.createMessage(":warning: Ein Verbindungsfehler ist aufgetreten... Jetzt bin ich aber wieder verbunden!"))
					.block();
			}
		});

		// ########## On received message ##########

		client.getEventDispatcher().on(MessageCreateEvent.class).log().subscribe(event -> {
			try{
				if (RuntimeVariables.isDebug) {
				System.out.println("Event received!");
				}
				if(discordHandlerWrapper[0] != null){
					discordHandlerWrapper[0].acceptEvent(event);
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
		});

		client.getEventDispatcher().on(InteractionCreateEvent.class).subscribe(event -> {
			try{
				if (RuntimeVariables.isDebug) {
				System.out.println("Event received!");
				}
				if(discordHandlerWrapper[0] != null){
					discordHandlerWrapper[0].acceptEvent(event);
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
		});

		// ########## Guild events ##########

		client.getEventDispatcher().on(GuildCreateEvent.class).subscribe(event ->{
			if(discordHandlerWrapper[0] != null){
				try{
					discordHandlerWrapper[0].addGuild(event.getGuild());
				}
				catch(Exception e){
					System.out.println("Something went terribly wrong!");
					e.printStackTrace();
				}
			}
		});

		client.getEventDispatcher().on(GuildDeleteEvent.class).subscribe(event ->{
			try{
				// Event might fire, when guild has an outage, ignore this case
				if(!event.isUnavailable() && discordHandlerWrapper[0] != null){
					discordHandlerWrapper[0].removeGuild(event.getGuild().get());
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
		});

		client.getEventDispatcher().on(MemberLeaveEvent.class).subscribe(event -> {
			try{
				if(discordHandlerWrapper[0] != null){
					discordHandlerWrapper[0].onMemberLeavesGuild(event);
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
			
		});

		// ########## Voice events ##########

		client.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event -> {
			try{
				if(discordHandlerWrapper[0] != null){
					discordHandlerWrapper[0].onVoiceStateEvent(event);
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
			
		});

		client.getEventDispatcher().on(VoiceChannelDeleteEvent.class).subscribe(event -> {
			try{
				if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].onVoiceChannelDeleted(event);
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
			
		});

		client.getEventDispatcher().on(TextChannelDeleteEvent.class).subscribe(event -> {
			try{
				if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].onTextChannelDeleted(event);
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
		});

		client.getEventDispatcher().on(RoleDeleteEvent.class).subscribe(event -> {
			try{
				if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].onRoleDeleted(event);
				}
			}
			catch(Exception e){
				System.out.println("Something went terribly wrong!");
				e.printStackTrace();
			}
		});
		
		// ########## On logout ##########
		client.onDisconnect().block();
	}

	public static boolean loadMainConfig(){
		final String configFileName = "./botConfig/mainConfig.json";

		// Read config file
		String content = FileManager.read(new File(configFileName));
		Gson gson = new Gson();
		MainConfig config = gson.fromJson(content, MainConfig.class);

		if(config == null){
			System.out.println("Failed to read config file " + configFileName);
			return false;
		}

		RuntimeVariables.createInstance(config);
		return true;
	}

	private static Secrets loadSecrets(){
		final String configFileName = "./botConfig/secrets.json";

		// Read config file
		String content = FileManager.read(new File(configFileName));
		Gson gson = new Gson();
		SecretsConfig config = gson.fromJson(content, SecretsConfig.class);

		if(config == null){
			System.out.println("Failed to read secrets file " + configFileName);
			return null;
		}

		return new Secrets(config);
	}
}
