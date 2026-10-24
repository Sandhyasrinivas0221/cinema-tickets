package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.EnumMap;
import java.util.Map;

/**
 * Totals the number of tickets requested for each ticket type.
 */
final class TicketCounter {

    private TicketCounter() {
    }

    static Map<TicketTypeRequest.Type, Integer> countByType(TicketTypeRequest... ticketTypeRequests) {
        Map<TicketTypeRequest.Type, Integer> counts = new EnumMap<>(TicketTypeRequest.Type.class);
        for (TicketTypeRequest.Type type : TicketTypeRequest.Type.values()) {
            counts.put(type, 0);
        }
        for (TicketTypeRequest request : ticketTypeRequests) {
            try {
                counts.merge(request.getTicketType(), request.getNoOfTickets(), Math::addExact);
            } catch (ArithmeticException e) {
                throw new InvalidPurchaseException("Too many tickets requested");
            }
        }
        return counts;
    }

}
