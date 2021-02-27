package start;

public class Secrets {
    String botKey;
    final String spotifyClientId;
    final String spotifyClientSecret;

    Secrets(final String botKey, final String spotifyClientId, final String spotifyClientSecret){
        this.botKey = botKey;
        this.spotifyClientId = spotifyClientId;
        this.spotifyClientSecret = spotifyClientSecret;
    }

    String getBotKey() {
        final String ret = this.botKey;
        this.botKey = null;
        return ret;
    }

    String getSpotifyClientId() {
        return this.spotifyClientId;
    }

    String getSpotifyClientSecret() {
        return this.spotifyClientSecret;
    }
}
