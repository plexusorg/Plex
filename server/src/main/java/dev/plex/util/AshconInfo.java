package dev.plex.util;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class AshconInfo
{
    private String uuid;
    private String username;

    @SerializedName("username_history")
    private UsernameHistory[] usernameHistories;

    private Textures textures;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UsernameHistory
    {
        private String username;
        @SerializedName("changed_at")
        private ZonedDateTime zonedDateTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Textures
    {
        private boolean custom;
        private boolean slim;
        private SkinData raw;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SkinData
    {
        private String value;
        private String signature;
    }
}
