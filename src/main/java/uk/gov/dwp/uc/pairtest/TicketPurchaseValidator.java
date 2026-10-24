package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Map;

/**
 * Validates a ticket purchase request against the business rules.
 * Throws {@link InvalidPurchaseException} when any rule is broken.
 */
class TicketPurchaseValidator {

    static final int MAX_TICKETS_PER_PURCHASE = 25;

    void validate(Long accountId, TicketTypeRequest... ticketTypeRequests) {
        validateAccountId(accountId);
        validateRequestsPresent(ticketTypeRequests);

        Map<TicketTypeRequest.Type, Integer> ticketCounts = TicketCounter.countByType(ticketTypeRequests);
        int adultCount = ticketCounts.get(TicketTypeRequest.Type.ADULT);
        int childCount = ticketCounts.get(TicketTypeRequest.Type.CHILD);
        int infantCount = ticketCounts.get(TicketTypeRequest.Type.INFANT);
        long totalTickets = (long) adultCount + childCount + infantCount;

        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException(
                    "A maximum of " + MAX_TICKETS_PER_PURCHASE + " tickets can be purchased at a time");
        }
        if (adultCount == 0) {
            throw new InvalidPurchaseException(
                    "Child and Infant tickets cannot be purchased without an Adult ticket");
        }
        if (infantCount > adultCount) {
            throw new InvalidPurchaseException(
                    "Each infant must sit on an adult's lap, so infants cannot outnumber adults");
        }
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account id must be greater than zero");
        }
    }

    private void validateRequestsPresent(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket request is required");
        }
        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null || request.getTicketType() == null) {
                throw new InvalidPurchaseException("Ticket request and ticket type must not be null");
            }
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException("Number of tickets in each request must be greater than zero");
            }
        }
    }

}
