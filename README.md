# Rules Service
[![license](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?logo=gnu&style=for-the-badge)](https://github.com/consiglionazionaledellericerche/cool-jconon/blob/master/LICENSE)
[![Supported JVM Versions](https://img.shields.io/badge/JVM-21-brightgreen.svg?style=for-the-badge&logo=Java)](https://openjdk.java.net/install/)
![Build width Gradle](https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Docker Image](https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white)
[![Build Status](https://github.com/cnr-anac/rule-service/actions/workflows/build.yml/badge.svg)](https://github.com/cnr-anac/rule-service/actions/workflows/build.yml)

## Introduzione
Rules Service è parte della suite di servizi per la verifica delle informazioni sulla Trasparenza dei siti web 
delle Pubbliche amministrazioni italiane.
Definisce e implementa le regole relative al D.Lgs. n. 33-2013 sulla trasparenza nella PA.

## Informazioni generali
Fornisce l'albero delle regole definito in [application.yaml](src/main/resources/application.yaml) oppure all'interno del [config-service](../../../config-service),
che quindi può essere modificato o ampliato sia attraverso variabili di ambiente o della JVM prima di avviare il servizio,
oppure aggiornando la configurazione per poi successivamente invocare l'endpoint dell'[actuator](http://localhost:8080/actuator/refresh) per recepire le modifiche. 

É possibile interagire con il servizio attraverso degli endpoint REST che permettono la consultazione dell'albero
delle regole in formato _json_, e la verifica di una o più regole su un contenuto _html_  

## Docker
Il servizio è dotato del plugi _jib_  che permette di effettuare la _build_ e fornisce l'immagine per l'esecuzione tramite _docker_
```bash
./gradlew jibDockerBuild
docker run -p 8080:8080 -ti rule-service:{version}
```
La documentazione relativa ai servizi REST è consultabile [qui](http://localhost:8080/api-docs) ed è possibile interagire
con i servizi attraverso **Swagger** alla seguente [URL](http://localhost:8080/swagger-ui/index.html)

É possibile visualizzare l'albero delle regole in formato _json_ da: http://localhost:8080/v1/rules

Oppure verificare la regola _root_ ad esempio attraverso una [cURL](https://it.wikipedia.org/wiki/Curl) con un html di esempio:
```bash
curl -X POST http://localhost:8080/v1/rules -H 'Content-type:application/json' --data 'PGh0bWw+CiAgICA8aGVhZD4KICAgICAgICA8dGl0bGU+R2VuZXJpY2EgQW1taW5pc3RyYXppb25lPC90aXRsZT4KICAgIDwvaGVhZD4KICAgIDxib2R5PgogICAgICAgIDxwPlBhcnNlZCBIVE1MIGludG8gYSBkb2MuPC9wPgogICAgICAgIDxhIGhyZWY9Ii9hbW1pbmlzdHJhemlvbmUiPkFtbWluaXN0cmF6aW9uZSBUcmFzcGFyYW50ZTwvYT4KICAgICAgICA8YSBocmVmPSIvcHJvZ3JhbW1hdHJhc3BhcmVuemEiPlByb2dyYW1tYSBwZXIgbGEgVHJhc3BhcmVuemE8L2E+CiAgICA8L2JvZHk+CjwvaHRtbD4='| jq .
```
In alternativa scaricare il contenuto del Sito istituzionale di una Pubblica Amministrazione
```bash
curl "https://www.anticorruzione.it"|base64 > base64.html
curl -X POST http://localhost:8080/v1/rules -H 'Content-type:application/json' --data @base64.html |jq .
```
La risposta _json_ del servizio: 
```json
{
  "url": "https://www.anticorruzione.it/amministrazione-trasparente",
  "ruleName": "amministrazione-trasparente",
  "isLeaf": false,
  "status": 200,
  "score": 3.6377878
}
```