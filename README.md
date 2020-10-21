# Discord-Bot (aka Megumin Bot)

# Wie verwende ich diesen Bot?

## Voraussetzungen
Mindestens Java 8, damit das Projekt kompiliert.\
Mindestens Java 11, damit das Projekt vernünftig läuft.

Die Version muss ggf. in der pom.xml entsprechend angepasst werden:
```xml
<maven.compiler.source>11</maven.compiler.source>
<maven.compiler.target>11</maven.compiler.target>
```
## Kompilierung und Ausführung

1. Repo klonen
2. a) `mvn install` und dann `maven build clean compile assembly:single` ausführen. Eine executable jar befindet sich dann unter **./target/meguminBot-{version}-jar-with-dependencies.jar**. Diese kann mit `java -jar ./target/meguminBot-{version}-jar-with-dependencies.jar` ausgeführt werden. Tipp: Als erstes Argument kann direkt der Token mitgegeben werden.

**ODER**

2. b) In einer IDE als Mavenprojekt öffnen und von dort ausführen.

Nach dem Start erhält der User einen Bot-Token prompt. Hier einfach den Token für den eigenen Bot eingeben.
Der Bot ist auf *Unser Server* ausgerichtet und funktioniert vermutlich bei anderen Clients nicht.

Das Verhalten vom Bot, der auf mehreren Servern Mitglied ist, wurde **NICHT** getestet.

# Wie orientiere ich mich in dem Spaghetticode?

Offensichtlich beginnt der Bot bei **StartUp**. Von dort aus werden messageReceivedEvents an den **MessageResponsePicker** weitergegeben und von dort aus an eine Instanz von **ResponseType**, wo der Befehl dann schließlich verarbeitet wird.

# AB HIER DEPRECATED

## ResponseType
Eine abstrakte Klasse, welche messageReceivedEvents akzeptiert und gültige Befehle an die entsprechenden (abstrakten) Methoden weiterleitet. Zudem sind hier einige hilfreiche Methoden implementiert, welche z.B. eine Nachricht versenden, dem Autor der erhaltenen Nachricht privat antworten und Sonstiges.

## Megumin
Megumin extended **ResponseType** und implementiert alle abstrakten Methoden, welche bei entsprechenden Befehlen ausgelöst werden.
"Under the hood" Hilfsmethoden aus **ResponseType** kommen hier zum Einsatz :)

## MessageResponsePicker
Beinhaltet wichtige Objekte wie z.B. eine Musikplaylist, Umfrageliste und die aktuelle **ResponseType** Instanz (in der Regel **Megumin**).
Die Idee dahinter ist, dass die **ResponseType** Instanz während der Laufzeit geändert werden kann. Je nach verwendeter Instanz wird dann anders auf Befehle reagiert.

# BIS HIER DEPRECATED

## AudioEventHandler
Nimmt Musiktracks auf und übergibt diese an den **TrackLoader**. Zudem wird hier auf MusikEvents reagiert und entsprechende Nachrichten werden verfasst.

## Survey
Umfragen werden im **MessageResponsePicker** in einer ArrayList verwaltet. Solange eine Survey Instanz existiert, kann sie Antworten entgegennehmen und verarbeiten (Stimmen hinzufügen, löschen, überschreiben etc.), wobei live die Discord Nachricht zur Umfrage aktualisiert wird. In einer Umfrage befindet sich auch ein Timer Objekt, welcher nach Ablauf der Zeit eine Zusammenfassung auf Discord posted und die Umfrage aus der am Anfang erwähnten ArrayList löscht. Damit wird auch die verwendete Umfrage-ID wieder frei (Umfragen nehmen bei der Erstellung automatisch die nächstkleinere, freie ID.
