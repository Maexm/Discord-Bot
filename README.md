# STOP: DIE README IST DEPRECATED UND MUSS AKTUALISIERT WERDEN

# Megumin-Bot

# Wie verwende ich diesen Bot?
1. Repo klonen
2. Mit `mvn build` bauen und die entstehende JAR Datei ausführen **ODER** mit Eclipse öffnen und von dort aus starten

Nach dem Start erhält der User einen Bot-Token prompt. Hier einfach den Token für den eigenen Bot eingeben.
Der Bot ist auf *Unser Server* ausgerichtet und funktioniert vermutlich bei anderen Clients nicht.

Das Verhalten vom Bot, der auf mehreren Servern Mitglied ist, wurde **NICHT** getestet.

# Wie orientiere ich mich in dem Spaghetticode?

Offensichtlich beginnt der Bot bei **StartUp**. Von dort aus werden messageReceivedEvents an den **MessageResponsePicker** weitergegeben und von dort aus an eine Instanz von **ResponseType**, wo der Befehl dann schließlich verarbeitet wird.

## ResponseType
Eine abstrakte Klasse, welche messageReceivedEvents akzeptiert und gültige Befehle an die entsprechenden (abstrakten) Methoden weiterleitet. Zudem sind hier einige hilfreiche Methoden implementiert, welche z.B. eine Nachricht versenden, dem Autor der erhaltenen Nachricht privat antworten und Sonstiges.

## Megumin
Megumin extended **ResponseType** und implementiert alle abstrakten Methoden, welche bei entsprechenden Befehlen ausgelöst werden.
"Under the hood" Hilfsmethoden aus **ResponseType** kommen hier zum Einsatz :)

## MessageResponsePicker
Beinhaltet wichtige Objekte wie z.B. eine Musikplaylist, Umfrageliste und die aktuelle **ResponseType** Instanz (in der Regel **Megumin**).
Die Idee dahinter ist, dass die **ResponseType** Instanz während der Laufzeit geändert werden kann. Je nach verwendeter Instanz wird dann anders auf Befehle reagiert.

## AudioEventHandler
Nimmt Musiktracks auf und übergibt diese an den **TrackLoader**. Zudem wird hier auf MusikEvents reagiert und entsprechende Nachrichten werden verfasst.

## Survey
Umfragen werden im **MessageResponsePicker** in einer ArrayList verwaltet. Solange eine Survey Instanz existiert, kann sie Antworten entgegennehmen und verarbeiten (Stimmen hinzufügen, löschen, überschreiben etc.), wobei live die Discord Nachricht zur Umfrage aktualisiert wird. In einer Umfrage befindet sich auch ein Timer Objekt, welcher nach Ablauf der Zeit eine Zusammenfassung auf Discord posted und die Umfrage aus der am Anfang erwähnten ArrayList löscht. Damit wird auch die verwendete Umfrage-ID wieder frei (Umfragen nehmen bei der Erstellung automatisch die nächstkleinere, freie ID.
