package spotify;

public class SpotifyObjects {
    
    public class ISpotifyObject{
        public int popularity;
    }

    public class TokenResponse{
        public String access_token;
        public String token_type;
        public int expires_in;
        public String scope;
    }

    public class SpotifyAlbumResponse extends SpotifyAlbum{
        public SpotifyAlbumTracksWrapper tracks;
    }

    public class SpotifyTrackResponse extends SpotifyTrack{
        public SpotifyAlbum album;
    }

    public class SpotifyArtistResponse extends SpotifyArtist{

    }

    public class SpotifyTrack extends ISpotifyObject{
        public SpotifyArtist[] artists;
        public int disc_number;
        public long duration_ms;
        public boolean explicit;
        public String href;
        public String id;
        public boolean is_local;
        public String name;
        public int track_number;
        public String type;
    }

    public class SpotifyAlbum extends ISpotifyObject{
        public String album_type;
        public SpotifyArtist[] artists;
        public String name;
        public String release_date;
        public String release_date_precision;
        public int total_tracks;
    }

    public class SpotifyArtist extends ISpotifyObject{
        public String name;
        public String type;
        public String id;
        public String href;
    }

    public class SpotifyAlbumTracksWrapper{
        SpotifyTrack[] items;
    }
}
