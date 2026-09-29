package com.lifeos.document.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private Local local = new Local();
    private long maxFileSizeBytes = 26214400L; // 25 MB
    private List<String> allowedMimeTypes = new ArrayList<>(List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain",
            "image/jpeg",
            "image/png",
            "image/webp"
    ));

    @Getter
    @Setter
    public static class Local {
        private String uploadDir = "./storage/uploads";
    }
}
