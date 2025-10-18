# V1

## Pre Coaching

### Geschatte Progress (in procent): 20%

### Status
_Waar sta je globaal? Wat loopt goed en minder goed? Hoe verloopt de samenwerking? Wie heeft globaal welke delen van de applicatie uitgewerkt? (details progress kunnen we bekijken op het issue board)_

We zijn ongeveer helemaal klaar met de restaurant service en zijn goed onderweg met de order service. Het algemeen coderen is tot nu toe wel leuk maar soms spendeer ik veel te veel tijd om sommige concepten te begrijpen vooraleer ik kan begginen met coderen.
De samenwerking verloopt goed, we hebben een goede taakverdeling en communiceren goed. Ik heb persoonlijk tot nu toe alleen die implementaties van Rania haar mooie uitgewerkte ERDs en API docs gemaakt. Daarnaast heb ik ook de restaurant-service: servicelaag, repository laag (beiden inmemory voor tests en Jpa) gemaakt en de http tests. Aan de order-service ben ik nog niet aan geweest buiten het skelet op te zetten.

### Stories
_(enkel voor stories die speciale aandacht vergen)_

[US5 ] In restaurant service is toch de bedoeling dat er juist helemaal GEEN interface is?

[US8 ] Ik weet nog niet goed hoe ik dit moet doen in de db? is dit iets op repo level met triggers? of is dit dat echt in de code moet staan?

### Quality
_Acties (refactorings,...) die nog gepland staan om de kwaliteit van je project te verhogen (maak hiervoor issues aan!): [issue nummer]: toelichting [issue nummer]: toelichting_

[]: nog geen issue hiervan gemaakt, maar ik heb nog niet genoeg validatie/logging overal gezet. Ik heb wel geprobeert zoveel mogenlijk consistent te zijn met code bvb lombok.

### Vragen
_Eventuele vragen voor de coach_

* Communication between repos
* wat wordt er juist verwacht van de frontend?
* hoe gaan we om met security? (owner)
* rabbitmq? is dit alleen voor comm tussen services of heeft dit ook een andere functie?

### Post Coaching

#### Feedback
DDD niet deftig begrepen en gevolgt, te weinig aandacht aan security, communicatie tussen services en frontend. Veel voorkomende fouten in code met betrekking tot best practices en code kwaliteit.
NOTES: 
-restaurant service: domein id bepalen
-insert wordt safe
-exceptions in plaats van optional
-geen try catch
-service specifiekere methoden namen
-get stream eigenlijk in database
-1 repo per aggregate;
-service laag pas dto
-service laag namen bussiness
-update etc is naam voor repo
-geen id in dto , domein moet id bepalen
-insert naam => save
-geen optionals teruggeven mr exceptions zijn leesbaarder
-geen getters & setters in domein, gedrag op entities
-juiste plek om uptedaten in service laag
-in service laag moet je de use stories implementeren
-niet in memory filteren meot in db gebeuren
-logica van domein in controllerlaag mag niet


# V2

## Pre Coaching

### Geschatte Progress (in procent): 60%

### Status
_Waar sta je globaal? Wat loopt goed en minder goed? Hoe verloopt de samenwerking? Wie heeft globaal welke delen van de applicatie uitgewerkt? (details progress kunnen we bekijken op het issue board)_

We zijn klaar met restaurant-service. Misschien nog feedback vragen over de security en rabbitmq implementatie. De order-service moet niet meer super veel werk meer hebben. Front end moet wel nog wat aan gedaan worden. De delivery-service hebben we nog niet veel aan gedaan omdat we wilden focussen op de andere services want als die werken en juiste code volgen dan kunnen we snel delivery-service afwerken. Ik (Ruben) heb alleen aan restaurant-service gewerkt en testing en security geimplementeerd. Ik heb ten opzichte van V1 eigenlijk alles moeten hermaken. Het waren deze weken wat moeilijker voor mij om samen te werken want ik heb andere projecten waar ik ook moeilijke deadlines voor heb waardoor ik buiten de lessen zelf een beetje moest zien wanneer ik kon werken en dat was meestal dan alleen en niet samen in een teams call of iets anders.
### Stories
_(enkel voor stories die speciale aandacht vergen)_

[US1 ] Moet dit gewoon in domain vna restaurant-service gebeuren?

[US3 ] Hoort dit bij de order-service frontend?

### Quality
_Acties (refactorings,...) die nog gepland staan om de kwaliteit van je project te verhogen (maak hiervoor issues aan!): [issue nummer]: toelichting [issue nummer]: toelichting_

[]: Zeker feedback over keycloak implementatie want vondt het moeilijk om het te laten werken met zoweinig code om op af te gaan.

### Vragen
_Eventuele vragen voor de coach_

* KeyCloak implementatie
* rabbitmq

### Post Coaching

#### Feedback