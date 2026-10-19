package com.asmithaexplores.ordertracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Seed demo data when the database is empty. */
    private boolean seedData = true;

    /** Shop name used in UI and WhatsApp messages. */
    private String shopName = "Asmitha Explores";
}
