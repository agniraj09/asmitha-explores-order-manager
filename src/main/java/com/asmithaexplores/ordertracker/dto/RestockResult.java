package com.asmithaexplores.ordertracker.dto;

/**
 * Outcome of the dashboard "Ordered" action.
 *
 * @param productName            product that was restocked
 * @param quantityAdded          units moved into stock
 * @param newQuantityInHand      stock after the update
 * @param remainingToBeOrdered   units still booked as to-be-ordered (normally 0)
 */
public record RestockResult(String productName,
                            int quantityAdded,
                            int newQuantityInHand,
                            int remainingToBeOrdered) {
}

