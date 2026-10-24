package uk.gov.dwp.uc.pairtest;

import java.util.Map;
import java.util.logging.Logger;


import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    private static final Logger log = Logger.getLogger(TicketServiceImpl.class.getName());

    private final TicketPurchaseValidator validator;
    private final TicketPaymentService paymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService paymentService,
                             SeatReservationService seatReservationService) {
        this(new TicketPurchaseValidator(), paymentService, seatReservationService);
    }

    TicketServiceImpl(TicketPurchaseValidator validator,
                      TicketPaymentService paymentService,
                      SeatReservationService seatReservationService) {
        this.validator = validator;
        this.paymentService = paymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
         validator.validate(accountId, ticketTypeRequests);

        Map<TicketTypeRequest.Type, Integer> ticketCounts = TicketCounter.countByType(ticketTypeRequests);
        int totalAmount = calculateTotalAmount(ticketCounts);
        int totalSeats = calculateTotalSeats(ticketCounts);

        paymentService.makePayment(accountId, totalAmount);
        log.info("Payment made for GBP " + totalAmount);

        seatReservationService.reserveSeat(accountId, totalSeats);
        log.info("Seats reserved: " + totalSeats);
    }

    private int calculateTotalAmount(Map<TicketTypeRequest.Type, Integer> ticketCounts) {
        int totalAmount = 0;
        for (Map.Entry<TicketTypeRequest.Type, Integer> entry : ticketCounts.entrySet()) {
            totalAmount += entry.getKey().getPrice() * entry.getValue();
        }
        return totalAmount;
    }

    private int calculateTotalSeats(Map<TicketTypeRequest.Type, Integer> ticketCounts) {
        // Infants sit on an adult's lap, so they are not allocated a seat.
        return ticketCounts.get(TicketTypeRequest.Type.ADULT)
                + ticketCounts.get(TicketTypeRequest.Type.CHILD);
    }


}
