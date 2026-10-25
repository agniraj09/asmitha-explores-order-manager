package com.asmithaexplores.ordertracker.tracking;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What the courier knows about one air waybill right now.
 *
 * @param delivered   the parcel has reached the customer
 * @param statusText  courier wording for the current state, e.g. "Delivered" / "Un Delivered"
 * @param deliveredAt delivery timestamp, only set once {@link #delivered} is true
 * @param events      scans, newest first
 */
public record TrackingSnapshot(boolean delivered,
                               String statusText,
                               LocalDateTime deliveredAt,
                               List<TrackingEvent> events) {

    public TrackingSnapshot {
        events = events == null ? List.of() : List.copyOf(events);
    }

    /** Most recent scan, or {@code null} when the courier returned none. */
    public TrackingEvent latestEvent() {
        return events.isEmpty() ? null : events.get(0);
    }

    /**
     * Short line shown in the UI: the latest scan when there is one, otherwise the overall status.
     * Capped at 120 characters so it always fits the column it is stored in.
     */
    public String summary() {
        TrackingEvent latest = latestEvent();
        String text = latest != null && latest.description() != null && !latest.description().isBlank()
                ? latest.description()
                : statusText;
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 117) + "...";
    }
}
