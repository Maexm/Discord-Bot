package start;

import java.util.ArrayList;
import java.util.HashSet;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import exceptions.IllegalMagicException;
import survey.Survey;
import survey.VoteEndReason;
import util.Pair;
import weather.Weather;

public class GlobalDiscordProxy {

    private final GlobalDiscordHandler parent;

    GlobalDiscordProxy(GlobalDiscordHandler parent) {
        this.parent = parent;
    }

    public void purgeAllGuilds() {
        this.parent.purgeAllGuilds();
    }

    public void logout(){
        this.parent.logout();
    }

    public GatewayDiscordClient getClient() {
        return this.parent.getClient();
    }

    public Survey getSurveyForKeyVerbose(String keyword){
        for (Survey survey : this.parent.getSurveys()) {
            if (survey.key.equals(keyword)) {
                return survey;
            }
        }
        return null;
    }

    public Survey getSurveyForKeyGuildScoped(String keyword, Snowflake guildId){
        Survey survey = this.getSurveyForKeyVerbose(keyword);
        return survey.guildId.equals(guildId) ? survey : null;
    }

    public Survey getSurveyForKeyUserScoped(String keyword, Snowflake userId){
        Survey survey = this.getSurveyForKeyVerbose(keyword);
        Member member;
        try{
            member = this.parent.getClient().getGuildById(survey.guildId).flatMap(guild -> guild.getMemberById(userId)).block();
        }
        catch(Exception e){
            member = null;
        }
        return member != null ? survey : null;
    }

    public void createSurvey(Survey survey){
        if(this.getSurveyForKeyVerbose(survey.key) != null){
            survey.stop(VoteEndReason.BROKEN);
            throw new IllegalMagicException("Survey key collision detected for key: "+survey.key);
        }
        this.parent.getSurveys().add(survey);
    }

    public ArrayList<Survey> getSurveysVerbose(){
        return this.parent.getSurveys();
    }

    public HashSet<Pair<Guild, VoiceChannel>> getSubscribedGuildChannelPairs(Snowflake userId){
        HashSet<Pair<Guild, VoiceChannel>> ret = new HashSet<>();

        // Search all guilds
        this.parent.getGuildMap().forEach((guildId, guildHandler) -> {
            // User is in guild
            if(guildHandler.hasUser(userId)){
                // Search each subscribed voice channel
                guildHandler.getVoiceSubscriptions().forEach((channelId, userSet) ->{
                    if(userSet.contains(userId)){
                        ret.add(new Pair<>(guildHandler.getGuild(), (VoiceChannel) guildHandler.getGuild().getChannelById(channelId).block()));
                    }
                });
            }
        });

        return ret;
    }

    public ArrayList<MessageChannel> getGlobalSystemChannels(){
        ArrayList<MessageChannel> ret = new ArrayList<>();
        this.parent.getGuildMap().forEach((guildId, guildHandler) ->{
            ret.add(guildHandler.getResponseType().getSystemChannel());
        });

        return ret;
    }

    public ArrayList<MessageChannel> getGlobalPsaChannels(){
        ArrayList<MessageChannel> ret = new ArrayList<>();
        this.parent.getGuildMap().forEach((guildId, guildHandler) ->{
            if(guildHandler.getResponseType().getPsaChannel() != null){
                ret.add(guildHandler.getResponseType().getPsaChannel());
            }
        });

        return ret;
    }

    public void saveAllGuilds(){
        this.parent.saveGuilds();
    }

    public Weather getWeatherService(){
        return this.parent.getWeatherService();
    }
}
