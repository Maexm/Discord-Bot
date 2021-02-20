package config;

public class GuildConfig {
    
    public long guildId;
    
    public long musicChannelId;

    public VoiceSubscription[] voiceSubscriptions;

    public long announcementChannelId;

    public boolean updateNote;

    public boolean psaNote;
    
    public long specialRoleId;

    public String homeTown;

    public static class VoiceSubscription{
        public long voiceChannelId;
        public long[] userIds;
    }
}
