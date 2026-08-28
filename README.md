# Keep Dishes Going: Stand van zaken (finale versie)

Dit document vergelijkt de **huidige (finale) versie** van het project met het project na 1ste examenkans (de oude basislijn van vóór de herwerking).

---

## Samenvatting voor / na

| Service | ervoor                                                                                      | Nu (final)                                                                                                                                                 |
|---|---------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| restaurant-service | Backend "klaar", RabbitMQ **niet** gekoppeld, Keycloak **deels functioneel**, 11 tests      | RabbitMQ + Keycloak volledig werkend, openingsuren-VO (US11/13/14), 14 testklassen, coverage 79,8%                                                         |
| order-service | "Bestaat maar slecht, volledige herwerking nodig", geen security, RabbitMQ niet functioneel | Volledige DDD-herwerking, Keycloak-security, RabbitMQ-handlers, Stripe-betaling, 18 testklassen, coverage 79,7%. En een HELE nieuwe tof gedaan frontend :) |
| delivery-service | Enkel domein-skeleton, geen schema/RabbitMQ/Keycloak, 6 tests                               | Volledig geïmplementeerd, RabbitMQ + Keycloak + PDF-rapport, 20 testklassen, coverage 87,6%                                                                |

---

## restaurant-service

**Ervoor :** RabbitMQ "not wired", Keycloak "config bestaat, deels functioneel".

**Nu:**
- **Messaging (RabbitMQ):** `RabbitConfig` (topic-exchange `kdg.events` + DLQ), `AmqpEventPublisher`, `OrderPlacedHandler` consumeert `order.placed`. Publiceert `order.accepted`, `order.rejected`, `order.ready_for_pickup` (conform event-catalogus).
- **Security (Keycloak):** `SecurityConfig` (JWT resource server), `KeycloakRealmRoleConverter`, `OwnerGuard` (deny-by-default).
- **US11/US13/US14:** `OpeningHours`-value object + `Restaurant.isOpenOn/closingAt/nextOpeningAfter/isOpenThrough`, `OrderAcceptanceService` (accept-guard), `PrepTimeEstimator`.
- **Scheduler-fix:** `ScheduledPublishProcessor` → `ScheduledPublishJobRunner` (geen self-invocation meer; één `@Scheduled`, per-job transactie).
- **US17:** `validateMenuItem` verplaatst van controller naar `DishService`, prijstolerantie configureerbaar (`kdg.validation.price-tolerance`).
- **US39:** `RestaurantType` + prijscategorie-strategie (berekend in order-service).
- **Config:** `kdg.restaurant.max-published-dishes` (US10-cap, geen magic number meer), `kdg.validation.price-tolerance`.
- **REST:** verb-endpoints (`/reject`, `/ready`) verwijderd; enkel `PATCH .../status` blijft.
- **Code-kwaliteit:** `@AllArgsConstructor` op `Restaurant` vervangen door expliciete constructors; dode overloads (`create(String, UUID)`, `createRestaurant(String, UUID)`) verwijderd.
- **Coverage:** jacoco geconfigureerd; 79,8% lijncoverage (US40 ≥ 60% ✓).

---

## order-service

**Ervoor:** "bad, rewrite needed", geen security, RabbitMQ enkel in properties.

**Nu:**
- **Volledige DDD-herwerking:** `Order`-aggregate (state machine, US18 freeze), `ShoppingCart` (US16), `Customer`, value objects (`Money`, `Quantity`, `Address`, `Email`).
- **Frontend:** Hel nieuwe frontend gemaakt zodat het echt bruikbaar is en echt lijkt. Veel inspiratie wel genomen van takeaway.com en uber eats.
- **Security (Keycloak):** `SecurityConfig` + `KeycloakRealmRoleConverter`; identiteit altijd uit JWT-subject (nooit uit body).
- **Messaging (RabbitMQ):** 5 handlers (`order.accepted/rejected/ready_for_pickup/picked_up/delivered`), `EventPublisher`-poort.
- **Betaling (US20):** `PaymentGateway`-poort met `StubPaymentGateway` **en** `StripePaymentGateway` (`@ConditionalOnProperty`), webhook met signature-verificatie, idempotent.
- **US23/US24:** `AutoRejectScheduler` (config-driven window) — markeert nu correct **REJECTED** (was CANCELLED).
- **US11/US13:** `ensureRestaurantOpen` bij `placeOrder`; `RestaurantProxyController` + `RestaurantGateway` voor open/closed en status.
- **US39:** `PriceCategoryStrategy` (bean-collection injection) + `PriceCategoryResolver`.
- **US21:** tracking read-model (`TrackingService`, `OrderEventHistoryRepository`).
- **Config:** `kdg.order.max-cart-items` nu echt gekoppeld aan het domein; `kdg.order.price-tolerance` extern (geen magic numbers meer).
- **Coverage:** 79,7% lijncoverage.

---

## delivery-service

**Ervoor:** enkel domein-skeleton, geen RabbitMQ/Keycloak/schema, 6 tests.

**Nu:**
- **Volledig geïmplementeerd:** `Delivery`, `DeliveryPerson`, `Payout` aggregates + volledige lifecycle (US27–US32).
- **One-aggregate-per-tx:** Spring `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` (`InternalEvents`, `DriverAssignmentListener`, `PayoutService`).
- **Messaging (RabbitMQ):** `OrderAcceptedHandler` + `OrderReadyForPickupHandler`; publiceert `order.picked_up`/`order.delivered`.
- **Security (Keycloak):** `SecurityConfig` (rollen `driver`/`admin`), `DriverGuard`; identiteit uit JWT-subject.
- **US34/US36/US37:** `PayoutPolicy`-poort + `PayoutProperties` (basisvergoeding, per-minuut, min/max — volledig configureerbaar).
- **US38:** `AdminPayoutReportController` + `PdfPayoutReportGenerator` (openhtmltopdf) — PDF-uitbetalingsrapport voor admin.
- **Coverage:** 87,6% lijncoverage (US41 ≥ 80% ✓).

---

## Nog open (niet aangepakt)

- De services missen user story tags in de commits.
- `OrderCancelledEvent` is een extra event (niet in de opgave) — enkel gebruikt bij expliciete klant cancelation.
- delivery staat `PICKED_UP -> DELIVERED` toe zonder `IN_TRANSIT`.
- `idp_mysql_data` (docker-owned) staat nog in `restaurant-service/src/main/java/.../infrastructure`.
- dockerfile secrets staan hardcoded en niet in `.env`.

---

## Testen & kwaliteit

- Alle drie de backends: `./gradlew test` groen.
- Geen `System.out.println`, geen `@Setter` in domein, geen JPA/RabbitMQ-imports in domein, geen `try/catch` in controllers, geen `@Transactional` op testen, geen `@WebMvcTest`-slice-tests.
- ArchUnit (order + delivery) en jmolecules (restaurant) handhaven de lagenregels.
