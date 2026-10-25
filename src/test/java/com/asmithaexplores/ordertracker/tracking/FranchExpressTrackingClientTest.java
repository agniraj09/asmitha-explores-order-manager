package com.asmithaexplores.ordertracker.tracking;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FranchExpressTrackingClientTest {

    private FranchExpressTrackingClient client;

    @BeforeEach
    void setUp() {
        client = new FranchExpressTrackingClient(
                new RestTemplateBuilder(), new ObjectMapper(), new AppProperties());
    }

    @Test
    @DisplayName("A parcel still on the road is not reported as delivered")
    void readsAnUndeliveredParcel() {
        String json = """
                {"status":"success","data":{"dl_status":"-1","dl_status_txt":"Un Delivered",
                "bk_dtm":"23-10-2026 7:44 PM","delv_dtm":"-","tracking":[
                {"trans_dtm":"2026-10-24 12:38:55","trans_for":"Out for Delivery","from":"RAJAPALAYAM, TN"},
                {"trans_dtm":"2026-10-23 21:33:54","trans_for":"Processed & Forwarded to Hub","from":"Chennai  hub, TN"}]}}
                """;

        Optional<TrackingSnapshot> snapshot = client.parse(json);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().delivered()).isFalse();
        assertThat(snapshot.get().statusText()).isEqualTo("Un Delivered");
        assertThat(snapshot.get().deliveredAt()).isNull();
        assertThat(snapshot.get().events()).hasSize(2);
        assertThat(snapshot.get().latestEvent().at())
                .isEqualTo(LocalDateTime.of(2026, 10, 24, 12, 38, 55));
        assertThat(snapshot.get().latestEvent().location()).isEqualTo("RAJAPALAYAM, TN");
        assertThat(snapshot.get().summary()).isEqualTo("Out for Delivery");
    }

    @Test
    @DisplayName("A handed over parcel is reported as delivered with its delivery timestamp")
    void readsADeliveredParcel() {
        String json = """
                {"status":"success","data":{"dl_status":"1","dl_status_txt":"Delivered",
                "bk_dtm":"23-10-2026 7:44 PM","delv_dtm":"25-10-2026 11:20 AM","tracking":[
                {"trans_dtm":"2026-10-25 11:20:03","trans_for":"Delivered","from":"RAJAPALAYAM, TN"}]}}
                """;

        Optional<TrackingSnapshot> snapshot = client.parse(json);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().delivered()).isTrue();
        assertThat(snapshot.get().deliveredAt()).isEqualTo(LocalDateTime.of(2026, 10, 25, 11, 20));
    }

    @Test
    @DisplayName("A delivery scan counts even when the headline status has not caught up")
    void readsDeliveryFromTheScanHistory() {
        String json = """
                {"status":"success","data":{"dl_status":"-1","dl_status_txt":"In Transit","delv_dtm":"-",
                "tracking":[{"trans_dtm":"2026-10-25 11:20:03","trans_for":"Delivered","from":"RAJAPALAYAM, TN"}]}}
                """;

        Optional<TrackingSnapshot> snapshot = client.parse(json);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().delivered()).isTrue();
        // Falls back to the scan time when the courier left the delivery timestamp empty
        assertThat(snapshot.get().deliveredAt()).isEqualTo(LocalDateTime.of(2026, 10, 25, 11, 20, 3));
    }

    @Test
    @DisplayName("Markup and padding in the courier labels are stripped")
    void cleansCourierMarkup() {
        String json = """
                {"status":"success","data":{"dl_status_txt":"Un Delivered","delv_dtm":"-","tracking":[
                {"trans_dtm":"2026-10-24 09:05:10","trans_for":"Processed &  Forwarded","from":"Madurai hub,<br> TN"}]}}
                """;

        TrackingEvent event = client.parse(json).orElseThrow().latestEvent();

        assertThat(event.description()).isEqualTo("Processed & Forwarded");
        assertThat(event.location()).isEqualTo("Madurai hub, TN");
    }

    @Test
    @DisplayName("An unknown waybill or a failed response never looks like a delivery")
    void returnsEmptyForUnusableResponses() {
        assertThat(client.parse(null)).isEmpty();
        assertThat(client.parse("")).isEmpty();
        assertThat(client.parse("not json")).isEmpty();
        assertThat(client.parse("{\"status\":\"error\",\"message\":\"Invalid AWB\"}")).isEmpty();
        assertThat(client.parse("{\"status\":\"success\"}")).isEmpty();
    }

    @Test
    @DisplayName("A blank tracking id is never sent to the courier")
    void skipsBlankTrackingIds() {
        assertThat(client.track(null)).isEmpty();
        assertThat(client.track("  ")).isEmpty();
    }
}
