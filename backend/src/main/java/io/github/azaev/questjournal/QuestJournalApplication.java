package io.github.azaev.questjournal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QuestJournalApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuestJournalApplication.class, args);
    }
}
