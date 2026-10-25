package com.asmithaexplores.ordertracker.tracking;

import java.time.LocalDateTime;

/**
 * A single scan in the courier's journey of a parcel.
 *
 * @param at          when the scan happened, {@code null} when the courier sent an unparsable date
 * @param description what happened, e.g. "Out for Delivery"
 * @param location    branch / hub the scan was made at, may be blank
 */
public record TrackingEvent(LocalDateTime at, String description, String location) {
}
