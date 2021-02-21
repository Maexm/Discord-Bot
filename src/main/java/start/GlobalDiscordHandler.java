package start;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import config.FileManager;
import config.GuildConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
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
import musicBot.MusicWrapper;
import spotify.SpotifyResolver;
import survey.Survey;
import system.GuildHandler;

public class GlobalDiscordHandler {

    private final HashMap<Snowflake, GuildHandler> guildMap;
    private final GuildHandler privateHandler;
    private final GlobalDiscordProxy globalProxy;
    private final GatewayDiscordClient client;
    private final ArrayList<Survey> surveys;
    private final AudioPlayerManager playerManager;

    public GlobalDiscordHandler(ReadyEvent readyEvent) {
        this.globalProxy = new GlobalDiscordProxy(this);
        this.client = readyEvent.getClient();

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
        this.privateHandler = new GuildHandler(null, this.globalProxy, null);

        this.surveys = new ArrayList<>();

        this.client.updatePresence(Presence
					.online(Activity.playing(RuntimeVariables.getStatus())))
					.block();
    }

    private SpotifyResolver createSpotifyResolver(){
        return new SpotifyResolver(RuntimeVariables.getInstance().DANGEROUSLY_getSpotifyClientId(), RuntimeVariables.getInstance().DANGEROUSLY_getSpotifyClientSecret());
    }

    public HashMap<Snowflake, GuildHandler> getGuildMap(){
        return this.guildMap;
    }

    public void acceptEvent(MessageCreateEvent event) {
        if(event.getGuildId().isPresent()){
            this.guildMap.get(event.getGuildId().get()).onMessageReceived(event);
        }
        else{
            this.privateHandler.onMessageReceived(event);
        }
    }

    void addGuild(Guild guild){
        if(!this.guildMap.containsKey(guild.getId())){
            System.out.println("Bot was added to guild "+guild.getName());
            MusicWrapper musicWrapper = new MusicWrapper(this.playerManager, this.createSpotifyResolver());
            GuildHandler guildHandler = new GuildHandler(guild.getId(), this.globalProxy, musicWrapper);
            this.guildMap.put(guild.getId(), guildHandler);
        }
    }

    void removeGuild(Guild guild){
        if(this.guildMap.containsKey(guild.getId())){
            System.out.println("Removing guild "+guild.getName());
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
        System.out.println("LOGGING OUT");
		this.getClient().logout().block();
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
        System.out.println("Saving guild config for "+this.guildMap.size()+" guild(s)");
        ArrayList<GuildConfig> guildConfigList = new ArrayList<>();
        this.guildMap.forEach((guildId, guildHandler) -> {
            try{
               guildConfigList.add(guildHandler.createGuildConfig());
            }
            catch(Exception e){
                System.out.println("Failed to create guildconfig for guild "+guildHandler.getGuild().getName());
            }
        });

        Gson gson = new Gson();
        final String guildConfigsString = gson.toJson(guildConfigList.toArray());
        File configFile = new File("guildConfig.json");
        boolean success = FileManager.write(configFile, guildConfigsString);
        
        System.out.println(success ? "Successfully persisted guild data" : "Failed to persist guild data");
    }
}
