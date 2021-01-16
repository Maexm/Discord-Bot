package start;

import java.io.Console;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import exceptions.StartUpException;

public class StartUp {

	public static void main(String[] args) {

		System.out.println("STARTING MEGUMIN BOT");
		// Retrieve token
		String TOKEN = "";
		if (args.length >= 1) {
			TOKEN = args[0];
		} else {
			System.out.println("Please enter a token (won't be displayed): ");
			Console console = System.console();
			TOKEN = new String(console.readPassword());
		}

		// Retrieve weather api key
		if (args.length >= 2) {
			RuntimeVariables.WEATHER_API_KEY = args[1];
		}

		// Retrieve debug info, if available
		if (args.length >= 3 && args[2].toUpperCase().equals("DEBUG")) {
			RuntimeVariables.IS_DEBUG = true;
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

			client.updatePresence(Presence
					.online(Activity.playing(RuntimeVariables.getStatus())))
					.block();

			if(RuntimeVariables.firstLogin){
				// Create entry point for event handling
				discordHandlerWrapper[0] = new GlobalDiscordHandler(ready);

				// Notify owner if debug
				if(RuntimeVariables.IS_DEBUG){
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
			if (RuntimeVariables.IS_DEBUG) {
				System.out.println("Event received!");
			}
			if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].acceptEvent(event);
			}
		});

		// ########## Guild events ##########

		client.getEventDispatcher().on(GuildCreateEvent.class).subscribe(event ->{
			if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].addGuild(event.getGuild());
			}
		});

		client.getEventDispatcher().on(GuildDeleteEvent.class).subscribe(event ->{
			// Event might fire, when guild has an outage, ignore this case
			if(!event.isUnavailable() && discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].removeGuild(event.getGuild().get());
			}
		});

		client.getEventDispatcher().on(MemberLeaveEvent.class).subscribe(event -> {
			if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].onMemberLeavesGuild(event);
			}
		});

		// ########## Voice events ##########

		client.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event -> {
			if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].onVoiceStateEvent(event);
			}
		});

		client.getEventDispatcher().on(VoiceChannelDeleteEvent.class).subscribe(event -> {
			if(discordHandlerWrapper[0] != null){
				discordHandlerWrapper[0].onVoiceChannelDeleted(event);
			}
		});
		
		// ########## On logout ##########
		client.onDisconnect().block();
	}

}
