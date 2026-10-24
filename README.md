# Cinema Tickets (Java)

## Running the tests

From the repository root, run:

```
mvn test
```

## Business rules implemented

| Ticket type | Price | Gets a seat |
|-------------|-------|-------------|
| ADULT       | £25   | Yes         |
| CHILD       | £15   | Yes         |
| INFANT      | £0    | No (sits on an adult's lap) |

A purchase is rejected with `InvalidPurchaseException` when:

- the account id is null or not greater than zero
- no ticket requests are supplied, or a request / ticket type is null
- any request asks for zero or a negative number of tickets
- more than 25 tickets are requested in total (infants included)
- child or infant tickets are requested without at least one adult ticket
- there are more infants than adults, based on the assumption that each infant needs an adult lap
- ticket totals overflow while combining requests

## Assumptions

- Infants count towards the 25 ticket limit because they are still tickets.
- Each infant needs its own adult lap, so infants cannot outnumber adults. This is an explicit implementation assumption because the brief does not specify the number of infants one adult can supervise.
- A request for zero tickets is treated as invalid rather than ignored.
- Ticket counts are combined using a wider numeric type before checking the purchase limit, so large counts across different ticket types cannot wrap around and bypass validation.
- Payment is requested before seats are reserved; neither service is called if validation fails.

## Edge-case coverage

Tests cover the 25-ticket boundary, totals above the limit across ticket types, infants counting toward the limit but not seat allocation, the one-infant-per-adult assumption, repeated requests of the same type, and integer overflow both within one type and across different types. Child and infant requests without an adult ticket are rejected.

## Design

- `TicketServiceImpl` – orchestrates validation, price/seat calculation and the third-party calls.
- `TicketPurchaseValidator` – holds all validation rules; injected so it can be replaced in tests.
- `TicketCounter` – totals tickets per type.
- `TicketTypeRequest` – immutable (`final` class and fields); each `Type` carries its own price.
