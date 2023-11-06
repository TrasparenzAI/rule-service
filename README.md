# Rules Service
## Introduzione
Rules Service è parte della suite di servizi per la verifica delle informazioni sulla Trasparenza dei siti web 
delle Pubbliche amministrazioni italiane.
Definisce e implementa le regole relative al D.Lgs. n. 33-2013 sulla trasparenza nella PA.

## Informazioni generali
Fornisce l'albero delle regole definito in [application.yaml](src/main/resources/application.yaml) 
che quindi può essere modificato o ampliato attraverso variabili di ambiente o della JVM prima di avviare il servizio. 

É possibile interagire con il servizio attraverso degli endpoint REST che permettono la consultazione dell'albero
delle regole in formato _json_, e la verifica di una o più regole su un contenuto _html_  

## Docker
Il servizio è dotato di un [Dockerfile](Dockerfile) che permette di effettuare la _build_ e fornisce 
l'immagine per l'esecuzione tramite _docker_
```
docker build . --tag rule-service:latest
docker run -p 8080:8080 -ti rule-service:latest
```
É possibile visualizzare l'albero delle regole in formato _json_ alla URL: http://localhost:8080/v1/rules

Oppure verificare la regola _root_ ad esempio attraverso una [cURL](https://it.wikipedia.org/wiki/Curl)
```
curl -X POST http://localhost:8080/v1/rules -H 'Content-type:application/json' --data 'PGh0bWw+CiAgICA8aGVhZD4KICAgICAgICA8dGl0bGU+R2VuZXJpY2EgQW1taW5pc3RyYXppb25lPC90aXRsZT4KICAgIDwvaGVhZD4KICAgIDxib2R5PgogICAgICAgIDxwPlBhcnNlZCBIVE1MIGludG8gYSBkb2MuPC9wPgogICAgICAgIDxhIGhyZWY9Ii9hbW1pbmlzdHJhemlvbmUiPkFtbWluaXN0cmF6aW9uZSBUcmFzcGFyYW50ZTwvYT4KICAgICAgICA8YSBocmVmPSIvcHJvZ3JhbW1hdHJhc3BhcmVuemEiPlByb2dyYW1tYSBwZXIgbGEgVHJhc3BhcmVuemE8L2E+CiAgICA8L2JvZHk+CjwvaHRtbD4='| jq .
```