package uk.gov.dwp.uc.pairtest.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketTypeRequestTest {

    @Test
    void storesTypeAndNumberOfTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);

        assertEquals(TicketTypeRequest.Type.CHILD, request.getTicketType());
        assertEquals(3, request.getNoOfTickets());
    }

    @Test
    void isImmutable() {
        assertTrue(Modifier.isFinal(TicketTypeRequest.class.getModifiers()), "class should be final");
        for (Field field : TicketTypeRequest.class.getDeclaredFields()) {
            assertTrue(Modifier.isFinal(field.getModifiers()), field.getName() + " should be final");
        }
    }

    @Test
    void ticketPricesMatchBusinessRules() {
        assertEquals(25, TicketTypeRequest.Type.ADULT.getPrice());
        assertEquals(15, TicketTypeRequest.Type.CHILD.getPrice());
        assertEquals(0, TicketTypeRequest.Type.INFANT.getPrice());
    }
}
