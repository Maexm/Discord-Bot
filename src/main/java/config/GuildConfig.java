package config;

public class GuildConfig {
    
    public Long guildId;
    
    public Long musicChannelId;

    public VoiceSubscription[] voiceSubscriptions;

    public Long announcementChannelId;

    public boolean updateNote;

    public boolean psaNote;
    
    public Long specialRoleId;

    public String homeTown;

    public String prefix;

    public static class VoiceSubscription{
        public Long voiceChannelId;
        public Long[] userIds;
    }
}
