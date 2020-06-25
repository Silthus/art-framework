package net.silthus.art.api.config;

import de.exlll.configlib.annotation.Comment;
import de.exlll.configlib.annotation.ConfigurationElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@ConfigurationElement
public class ArtConfig {

    @Comment({
            "DO NOT CHANGE THIS LINE",
            "REMOVE THIS LINE IF COPIED"
    })
    private String id = UUID.randomUUID().toString();
    private String parser = "flow";
    private Options options = new Options();
    private List<String> art = new ArrayList<>();

    public ArtConfig() {
    }

    @Data
    @ConfigurationElement
    public static class Options {

        private List<String> worlds = new ArrayList<>();
    }
}
