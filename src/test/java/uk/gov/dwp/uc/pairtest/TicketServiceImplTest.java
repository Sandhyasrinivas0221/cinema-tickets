package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

class TicketServiceImplTest {

    private static final long VALID_ACCOUNT_ID = 1L;

    private TicketPaymentService paymentService;
    private SeatReservationService seatReservationService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        seatReservationService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, seatReservationService);
    }

    private static TicketTypeRequest adults(int count) {
        return new TicketTypeRequest(ADULT, count);
    }

    private static TicketTypeRequest children(int count) {
        return new TicketTypeRequest(CHILD, count);
    }

    private static TicketTypeRequest infants(int count) {
        return new TicketTypeRequest(INFANT, count);
    }

    private void assertPurchaseRejected(Long accountId, TicketTypeRequest... requests) {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(accountId, requests));
        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Nested
    @DisplayName("Valid purchases")
    class ValidPurchases {

        @Test
        @DisplayName("One adult pays 25 and reserves 1 seat")
        void singleAdult() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(1));

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 25);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 1);
        }

        @Test
        @DisplayName("Adults and children are charged 25 and 15 each")
        void adultsAndChildren() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(2), children(3));

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 2 * 25 + 3 * 15);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 5);
        }

        @Test
        @DisplayName("Infants are free and are not allocated a seat")
        void infantsAreFreeAndHaveNoSeat() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(2), infants(2));

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 50);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 2);
        }

        @Test
        @DisplayName("Mixed purchase of all three ticket types")
        void allTicketTypes() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(3), children(2), infants(1));

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 3 * 25 + 2 * 15);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 5);
        }

        @Test
        @DisplayName("Several requests of the same type are added together")
        void multipleRequestsOfSameType() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(1), adults(2), children(1), children(1));

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 3 * 25 + 2 * 15);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 5);
        }

        @Test
        @DisplayName("Exactly 25 tickets is allowed")
        void exactlyMaximumTickets() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(10), children(10), infants(5));

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 10 * 25 + 10 * 15);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 20);
        }

        @Test
        @DisplayName("Infants count towards the 25 ticket limit but not towards seats")
        void maximumTicketsWithInfants() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(13), infants(12));

            verify(paymentService).makePayment(VALID_ACCOUNT_ID, 13 * 25);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 13);
        }

        @Test
        @DisplayName("Payment is taken once and seats are reserved once, in that order")
        void paymentThenReservationOnce() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, adults(1), children(1));

            var order = inOrder(paymentService, seatReservationService);
            order.verify(paymentService).makePayment(VALID_ACCOUNT_ID, 40);
            order.verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 2);
            verifyNoMoreInteractions(paymentService, seatReservationService);
        }

        @Test
        @DisplayName("Large account ids are accepted")
        void largeAccountId() {
            ticketService.purchaseTickets(Long.MAX_VALUE, adults(1));

            verify(paymentService).makePayment(Long.MAX_VALUE, 25);
            verify(seatReservationService).reserveSeat(Long.MAX_VALUE, 1);
        }
    }

    @Nested
    @DisplayName("Invalid account id")
    class InvalidAccountId {

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
        @DisplayName("Account id must be greater than zero")
        void rejectsInvalidAccountId(Long accountId) {
            assertPurchaseRejected(accountId, adults(1));
        }
    }

    @Nested
    @DisplayName("Invalid ticket requests")
    class InvalidTicketRequests {

        @Test
        @DisplayName("No ticket requests")
        void noRequests() {
            assertPurchaseRejected(VALID_ACCOUNT_ID);
        }

        @Test
        @DisplayName("Null request array")
        void nullRequestArray() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, (TicketTypeRequest[]) null);
        }

        @Test
        @DisplayName("Null request inside the array")
        void nullRequestInArray() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(1), null);
        }

        @Test
        @DisplayName("Null ticket type")
        void nullTicketType() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(1), new TicketTypeRequest(null, 1));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("Number of tickets must be greater than zero")
        void nonPositiveTicketCount(int count) {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(count));
        }

        @Test
        @DisplayName("Negative count cannot be used to cancel out another request")
        void negativeCountAlongsideValidRequest() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(30), adults(-10));
        }
    }

    @Nested
    @DisplayName("Business rule violations")
    class BusinessRuleViolations {

        @Test
        @DisplayName("More than 25 tickets")
        void moreThanMaximumTickets() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(26));
        }

        @Test
        @DisplayName("More than 25 tickets across several types")
        void moreThanMaximumAcrossTypes() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(10), children(10), infants(6));
        }

        @Test
        @DisplayName("Very large counts do not overflow past the limit check")
        void integerOverflow() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(Integer.MAX_VALUE), adults(Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("Large counts across ticket types do not overflow past the limit check")
        void crossTypeIntegerOverflow() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(1), children(Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("Child tickets without an adult")
        void childWithoutAdult() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, children(2));
        }

        @Test
        @DisplayName("Infant tickets without an adult")
        void infantWithoutAdult() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, infants(1));
        }

        @Test
        @DisplayName("Child and infant tickets without an adult")
        void childAndInfantWithoutAdult() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, children(1), infants(1));
        }

        @Test
        @DisplayName("More infants than adults")
        void moreInfantsThanAdults() {
            assertPurchaseRejected(VALID_ACCOUNT_ID, adults(1), infants(2));
        }
    }
}
