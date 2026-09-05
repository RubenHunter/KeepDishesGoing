# Reflectie — Keep Dishes Going


## Zelfreflectie — 28/08/2026

### Pre (voorbereiding coachinggesprek)

**Wat goed ging**

- **DDD-structuur:** de drie services zijn consequent opgebouwd (api → application → domain → infrastructure). Elke service heeft duidelijke aggregates (Restaurant/Dish, Order/ShoppingCart, Delivery/DeliveryPerson/Payout) met gedrag in het domein, niet in de service-laag.
- **One-aggregate-per-transaction:** in de delivery-service lossen we cross-aggregate-wijzigingen op met Spring `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`. Dat dwingt de DDD-consistentiegrens af in plaats van erover te discussiëren.
- **Event-gedreven communicatie:** de volledige order-levenscyclus loopt via RabbitMQ (topic-exchange `kdg.events` + dead-letter queues), exact volgens de event-catalogus uit de opgave.
- **Security:** Keycloak JWT-resource-servers in alle drie de services; identiteit komt altijd uit het JWT-subject (nooit uit body/query).
- **Configuratie:** uitbetalingsbeleid, beslissingsvenster, prijstolerantie en cart-limiet zijn externalized in `application.properties`.

**Wat moeilijker ging**
- **RabbitMQ:** eerste keer dat ik dit moest gebruiken dus veel moeten opzoeken en uitleg vragen aan AI. 
- **Keycloak:** basis ervan ging zeer goed maar het moment dat het wat ingewikkelder werdt heb ik hulp moeten vragen. 
- **Openingsuren (US11/US13/US14):** het parsen van de vrije-tekst-wekelijkse planning (incl. nacht-overschrijding) en het correct combineren met de manuele open/sluit-status kostte meer tijd dan verwacht.
- **Self-invocation & transacties:** de scheduler-bug ("Query requires transaction be in progress") leerde ons dat `@Transactional` op `this`-calls niet werkt via de Spring-proxy; opgelost door de job-logica in een aparte bean te zetten.
- **US24 (auto-reject):** eerst werd auto-afwijzen per ongeluk als CANCELLED gemarkeerd in plaats van REJECTED; rechtgezet zodat de status en de reden kloppen met de opgave.

**Verbeterpunten voor de volgende ronde**

- Consistentie in REST: sommige actie-endpoints (`/open`, `/close`) zijn nog niet samengevoegd tot één resource-stijl `PATCH /status`.
- Reflectie/one-pager en git-tags (`v1` op delivery-service) moeten netjes afgewerkt worden vóór de deadline.

### Post (na coachinggesprek)

*(In te vullen na het gesprek.)*
