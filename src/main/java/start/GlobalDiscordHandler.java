package start;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Optional;

import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import config.FileManager;
import config.GuildConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import logging.QuickLogger;
import musicBot.MusicWrapper;
import schedule.RefinedTimerTask;
import schedule.TaskManager;
import spotify.SpotifyResolver;
import survey.Survey;
import system.DecompiledMessage;
import system.GuildHandler;
import util.StringUtils;
import util.Time;
import weather.Weather;

public class GlobalDiscordHandler {

    private final HashMap<Snowflake, GuildHandler> guildMap;
    private final GuildHandler privateHandler;
    private final GlobalDiscordProxy globalProxy;
    private final GatewayDiscordClient client;
    private final ArrayList<Survey> surveys;
    private final AudioPlayerManager playerManager;
    private final Optional<Secrets> secrets;
    private final Weather weatherService;
    private final TaskManager<RefinedTimerTask> globalTasks;

    public GlobalDiscordHandler(ReadyEvent readyEvent, Optional<Secrets> secrets) {
        this.globalProxy = new GlobalDiscordProxy(this);
        this.client = readyEvent.getClient();
        this.secrets = secrets;
        this.weatherService = new Weather(secrets.isPresent() ? secrets.get().getWeatherApiKey() : null);
        this.globalTasks = new TaskManager<>();
        

        this.playerManager = new DefaultAudioPlayerManager();
		playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);

        // Prepare guilds
        this.guildMap = new HashMap<>();

        readyEvent.getGuilds().forEach(guild -> {
            MusicWrapper musicWrapper = new MusicWrapper(this.playerManager, this.createSpotifyResolver());
            GuildHandler guildHandler = new GuildHandler(guild.getId(), this.globalProxy, musicWrapper);
            this.guildMap.put(guild.getId(), guildHandler);
        });
        this.saveGuilds();
        this.privateHandler = new GuildHandler(null, this.globalProxy, null);

        this.surveys = new ArrayList<>();

        this.client.updatePresence(Presence
					.online(Activity.playing(RuntimeVariables.getStatus())))
					.block();

        // ########## TASKS ##########
		// // TODO: Move to a dedicated file
		this.globalTasks.addTask(new RefinedTimerTask(null, Long.valueOf(Time.DAY),
				Time.getNext(0, 0, 0).getTime(), this.globalTasks) {

			@Override
			public void runTask() {
				try{
                client.updatePresence(Presence.online(Activity.playing(RuntimeVariables.getStatus()))).block();
                   Calendar now = Time.getNow();
                   switch(now.get(Calendar.MONTH) + 1){
                       case 3:
                       switch(now.get(Calendar.DAY_OF_MONTH)){
                        case 30:
                        case 31:
                            client.getApplicationInfo()
                            .flatMap(info -> info.getOwner())
                            .flatMap(owner -> owner.getPrivateChannel())
                            .flatMap(channel -> channel.createMessage(RuntimeVariables.getInstance().getConfig().data[0]))
                            .block();
                            break;
                    }
                    break;
                       case 4:
                        switch(now.get(Calendar.DAY_OF_MONTH)){
                            case 1:
                                String url = RuntimeVariables.getInstance().getConfig().data[1];
                                if(url != null && !url.equals("")){
                                    client.updatePresence(Presence.online(Activity.streaming(RuntimeVariables.getStatus(), url))).block();
                                }
                                break;
                        }
                        break;
                   }
                }
                catch(Exception e){
                    QuickLogger.logErr("Failed to update presence!");
                }
			}

		});
    }

    private SpotifyResolver createSpotifyResolver(){
        if(this.secrets.isPresent() && !StringUtils.isNullOrWhiteSpace(this.secrets.get().getSpotifyClientId()) && !StringUtils.isNullOrWhiteSpace(this.secrets.get().getSpotifyClientSecret())){
            return new SpotifyResolver(this.secrets.get().getSpotifyClientId(), this.secrets.get().getSpotifyClientSecret());
        }
        else{
            return new SpotifyResolver("", "");
        }
    }

    public HashMap<Snowflake, GuildHandler> getGuildMap(){
        return this.guildMap;
    }

    public void acceptEvent(MessageCreateEvent event) {
        DecompiledMessage msg = new DecompiledMessage(event);
        if(msg.isBroken()){
            return;
        }

        if(event.getGuildId().isPresent()){
            this.guildMap.get(event.getGuildId().get()).onMessageReceived(msg);
        }
        else{
            this.privateHandler.onMessageReceived(msg);
        }
    }

    public void acceptEvent(InteractionCreateEvent event){
        DecompiledMessage msg = new DecompiledMessage(event, RuntimeVariables.getInstance().getCommandPrefix());
        if(msg.isBroken()){
            return;
        }

        if(event.getInteraction().getGuildId().isPresent()){
            this.guildMap.get(event.getInteraction().getGuildId().get()).onMessageReceived(msg);
        }
        else{
            this.privateHandler.onMessageReceived(msg);
        }
    }

    void addGuild(Guild guild){
        if(!this.guildMap.containsKey(guild.getId())){
            QuickLogger.logInfo("Bot was added to guild "+guild.getName());
            MusicWrapper musicWrapper = new MusicWrapper(this.playerManager, this.createSpotifyResolver());
            GuildHandler guildHandler = new GuildHandler(guild.getId(), this.globalProxy, musicWrapper);
            this.guildMap.put(guild.getId(), guildHandler);
            this.saveGuilds();
        }
    }

    void removeGuild(Guild guild){
        if(this.guildMap.containsKey(guild.getId())){
            QuickLogger.logInfo("Removing guild "+guild.getName());
            this.guildMap.remove(guild.getId()).onPurge();
        }
    }

    void onVoiceStateEvent(VoiceStateUpdateEvent event){
        this.guildMap.get(event.getCurrent().getGuildId()).onVoiceStateEvent(event);
    }

    void purgeAllGuilds() {
        this.guildMap.forEach((guildId, handler) ->{
            handler.onPurge();
        });
    }

    void logout(){
        this.globalTasks.stopAll();
        QuickLogger.logInfo("LOGGING OUT");
		this.getClient().logout().block();
        System.exit(0);
    }

    ArrayList<Survey> getSurveys(){
        return this.surveys;
    }

    GatewayDiscordClient getClient() {
        return this.client;
    }

    public void onVoiceChannelDeleted(VoiceChannelDeleteEvent event){
        if(this.guildMap.containsKey(event.getChannel().getGuildId())){
            this.guildMap.get(event.getChannel().getGuildId()).onVoiceChannelDeleted(event);
        }
    }

    public void onMemberLeavesGuild(MemberLeaveEvent event){
        if(this.guildMap.containsKey(event.getGuildId())){
            this.guildMap.get(event.getGuildId()).onUserRemoved(event);
        }
    }

    public void onTextChannelDeleted(TextChannelDeleteEvent event){
        if(this.guildMap.containsKey(event.getChannel().getGuildId())){
            this.guildMap.get(event.getChannel().getGuildId()).onTextChannelDeleted(event);
        }
    }

    public void onRoleDeleted(RoleDeleteEvent event){
        if(this.guildMap.containsKey(event.getGuildId())){
            this.guildMap.get(event.getGuildId()).onRoleDeleted(event);
        }
    }

    void saveGuilds(){
        QuickLogger.logInfo("Saving guild config for "+this.guildMap.size()+" guild(s)");
        ArrayList<GuildConfig> guildConfigList = new ArrayList<>();
        this.guildMap.forEach((guildId, guildHandler) -> {
            try{
               guildConfigList.add(guildHandler.createGuildConfig());
            }
            catch(Exception e){
                QuickLogger.logFatalErr("Failed to create guildconfig for guild "+guildHandler.getGuild().getName());
                e.printStackTrace();
            }
        });

        Gson gson = new Gson();
        final String guildConfigsString = gson.toJson(guildConfigList.toArray(new GuildConfig[guildConfigList.size()]));
        File configFile = new File("./botConfig/guildConfig.json");
        boolean success = FileManager.write(configFile, guildConfigsString);
        
        if(success){
            QuickLogger.logInfo("Successfully persisted guild data");
        }
        else{
            QuickLogger.logFatalErr("Failed to persist guild data");
        }

    }

    void reloadGuilds(){
        QuickLogger.logInfo("Reloading "+this.guildMap.size()+" guild(s)");
        this.guildMap.forEach((guildId, guildHandler) -> {
            try{
               guildHandler.loadConfig();
            }
            catch(Exception e){
                QuickLogger.logFatalErr("Failed to load guild "+guildHandler.getGuild().getName());
                e.printStackTrace();
            }
        });

        this.saveGuilds();
    }

    Weather getWeatherService(){
        return this.weatherService;
    }
}
