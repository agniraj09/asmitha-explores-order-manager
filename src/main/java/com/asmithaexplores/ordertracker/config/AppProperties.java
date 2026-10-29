package com.asmithaexplores.ordertracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Seed demo data when the database is empty. */
    private boolean seedData = true;

    /** Shop name used in UI and WhatsApp messages. */
    private String shopName = "Asmitha Explores";

    /**
     * Courier tracking page shared with the customer when an order is marked as shipped.
     * Pre-fills the "Share with customer" card and can be overridden per order in the browser.
     */
    private String trackingUrl = "https://franchexpress.com/courier-tracking/";

    /** Automatic delivery detection for shipped orders. */
    private Tracking tracking = new Tracking();

    /** Seller details printed on the bill. */
    private Bill bill = new Bill();

    @Getter
    @Setter
    public static class Bill {

        /** Business name on the "From" block; falls back to {@link AppProperties#shopName}. */
        private String sellerName;

        /** Address of the shop, one entry per printed line. */
        private List<String> addressLines = new ArrayList<>(List.of("OMR", "Chennai - 600097"));

        /** Contact number printed under the shop address. */
        private String mobile = "9445077125";

        /** Optional email, left out of the bill when blank. */
        private String email;

        /** Optional GST registration number, left out of the bill when blank. */
        private String gstin;

        /** Prefix of the bill number, e.g. {@code AE-000017} for order 17. */
        private String numberPrefix = "AE-";

        /** Closing line at the bottom of the bill. */
        private String footerNote = "Thank you for shopping with us!";
    }

    @Getter
    @Setter
    public static class Tracking {

        /** Poll the courier in the background and move delivered orders on by itself. */
        private boolean autoSyncEnabled = true;

        /** Endpoint behind the courier's public tracking page. */
        private String apiUrl = "https://franchexpress.com/proxy.php";

        /** Shipped orders older than this are no longer polled - the courier forgets them too. */
        private int maxAgeDays = 45;

        /** Pause between two courier lookups in one run, so the sync stays a polite visitor. */
        private long requestDelayMs = 1000;

        /** How often the background sync runs, in milliseconds (default: every 6 hours). */
        private long intervalMs = 21_600_000;

        /** Grace period after startup before the first run, in milliseconds. */
        private long initialDelayMs = 120_000;
    }
}
