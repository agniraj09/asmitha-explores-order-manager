package com.asmithaexplores.ordertracker.tracking;

import java.util.Optional;

/** Reads the live status of a parcel from the courier. */
public interface CourierTrackingClient {

    /**
     * @param trackingId air waybill number printed on the parcel
     * @return what the courier knows, or empty when the id is unknown or the courier could not be
     * reached - callers must treat an empty result as "no news", never as "not delivered"
     */
    Optional<TrackingSnapshot> track(String trackingId);
}
