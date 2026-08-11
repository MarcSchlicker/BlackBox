# BlackBox

BlackBox ist eine experimentelle NeoForge-Mod, die automatisierte Minecraft-Farmen nur kurzzeitig in isolierten Farmdimensionen ausführt. Nach einer Messung reproduziert eine kompakte Blackbox den gemessenen Netto-Input und -Output, ohne dass die ursprüngliche Farm dauerhaft geladen und getickt werden muss.

> **Projektstatus:** Work in Progress / spielbarer Prototyp. Die Kernschleife ist implementiert, benötigt aber noch ausgiebige Tests mit Multiplayer-Servern und großen Modpacks.

## Kompatibilität

| Komponente | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Modloader | NeoForge 21.1.190 oder neuer |
| Java | 21 |
| Seiten | Client und Server |

## Grundidee

Eine große Farm verursacht normalerweise auch dann Serverlast, wenn ihr Aufbau längst abgeschlossen ist. BlackBox trennt Aufbau, Messung und späteren Betrieb:

1. Eine Dimensionswerkbank wird aufgestellt und mit einem Dimensionskern bestückt.
2. Der Kern erhält eine eigene, dauerhafte Farm-ID und damit eine isolierte 3x3-Chunk-Zelle.
3. Die Farm wird in dieser Zelle gebaut und über den Input- und Outputblock angeschlossen.
4. Erst beim Verlassen der Zelle beginnt die automatische Analyse.
5. Nach einer internen Vorbereitungsphase werden 60 Sekunden Produktion gemessen.
6. Der programmierte Kern simuliert diese Produktion anschließend in der Werkbank oder in einer Blackbox.

## Implementierte Features

### Isolierte Farmzellen

- Jeder Dimensionskern besitzt eine UUID und eine eigene 3x3-Chunk-Zelle.
- Zellen liegen weit voneinander entfernt, damit Farmen sich nicht gegenseitig beeinflussen.
- Die Zelle besteht aus einer einzigen Ebene Dimensionsgrundgestein auf Y=0.
- Außerhalb der Farmzellen wird kein Boden generiert.
- Bauen und Abbauen ist auf die dem Spieler zugewiesene Zelle begrenzt.
- Input- und Outputblock werden beim ersten Betreten automatisch bereitgestellt.
- Ein Rechtsklick auf das Dimensionsgrundgestein öffnet den Rückweg.

### Vier Farmumgebungen

Ein Dimensionskern kann auf vier Umgebungen eingestellt werden. Die Farbe des Kerns zeigt die Auswahl direkt an:

| Umgebung | Kernfarbe | Eigenschaften |
| --- | --- | --- |
| Standard | Gelb | Neutrale BlackBox-Umgebung |
| Oberwelt | Grün | Oberwelt-Dimensionstyp und Plains-Biom |
| Nether | Rot | Nether-Eigenschaften und Nether-Wastes-Biom |
| End | Lila | End-Eigenschaften und End-Biom |

Die Umgebungen bleiben absichtlich flach und erzeugen keine normale Landschaft, Strukturen oder Erze.

### Messung von Input und Output

- Die Messung startet erst, nachdem der Spieler die fertig gebaute Farm verlassen hat.
- Während der internen Vorbereitung läuft die Farm bereits, diese Zeit zählt jedoch nicht zum Profil.
- Danach werden 60 Sekunden lang Inventare und Item-Handler in allen neun Chunks beobachtet.
- Der Inputblock erhält Items direkt aus den Eingangsslots der Dimensionswerkbank.
- Der Outputblock überträgt produzierte Items direkt in die Ausgangsslots der Werkbank.
- Gespeichert wird die Nettoänderung der gesamten Zelle. Das reine Verschieben einer vorhandenen Diamantkiste zählt daher nicht als Produktion.
- Produktionszeitpunkte werden mitgespeichert, damit Spitzen und Pausen später wiedergegeben werden können.
- Erneutes Betreten stoppt eine laufende Messung und löscht das bisherige Farmprofil, damit die Farm bearbeitet und neu vermessen werden kann.

### Dimensionskern

- Speichert Farm-ID, Farmname, Umgebung, Mobspawn-Einstellung und das gemessene Produktionsprofil.
- Ändert seine Farbe passend zur ausgewählten Umgebung.
- Erhält einen Verzauberungsschimmer, sobald ein gültiges Produktionsprofil gespeichert ist.
- Zeigt bei gedrückter Umschalttaste Input und Output pro Minute an.
- Ein vorbereiteter Eisenfarm-Beispielkern ist im Kreativinventar enthalten.

### Dimensionswerkbank und Blackbox

- Beide Maschinen besitzen einen Kernslot, neun Inputslots und achtzehn Outputslots.
- Die Dimensionswerkbank dient gleichzeitig zum Erstellen, Messen und späteren Simulieren einer Farm.
- Eine separate Blackbox startet automatisch, sobald ein programmierter Kern eingesetzt wird.
- Inputs werden nur verbraucht, wenn ein vollständiger Simulationszyklus finanziert werden kann.
- Bei vollem Outputinventar pausiert die Simulation, statt Items zu löschen.
- Hopper und andere Item-Transportsysteme können die Maschinen befüllen und leeren.
- Farmen können in der Werkbank benannt werden; der Haken speichert den Namen, der Pfeil betritt die Farmzelle.

### Upgrades

Alle Upgrades besitzen eigene, zusammengehörige Symbole und erklären ihre Funktion im Tooltip. Verbrauchbare Kern-Upgrades haben eine sechseckige Kernfassung. Wiederverwendbare Blackbox-Upgrades verwenden eine breite Modulform mit seitlichen Anschlüssen.

| Upgrade | Verwendung | Verhalten |
| --- | --- | --- |
| Standard-Umgebung | Kern / Dimensionswerkbank | Setzt den Kern auf die neutrale Umgebung |
| Oberwelt-Umgebung | Kern / Dimensionswerkbank | Setzt Oberwelt-Eigenschaften und -Biom |
| Nether-Umgebung | Kern / Dimensionswerkbank | Setzt Nether-Eigenschaften und -Biom |
| End-Umgebung | Kern / Dimensionswerkbank | Setzt End-Eigenschaften und -Biom |
| Mobspawn | Kern / Dimensionswerkbank | Erlaubt natürliche Mobspawns für diese Farmzelle |
| Stabilität | Blackbox / Werkbank | Verteilt den gemessenen Output gleichmäßig statt in gemessenen Schüben |

Umgebungs- und Mobspawn-Upgrades werden angewendet und verbraucht. Dabei wird ein bestehendes Messprofil gelöscht, weil die Farm unter den neuen Bedingungen erneut gemessen werden muss. Das Stabilitätsupgrade bleibt im Maschinenslot und ist wiederverwendbar.

### Blueprint-Bibliothek

- Das blaue Blueprint-Werkzeug ist wiederverwendbar und speichert selbst keine Farmdaten.
- Rechtsklick in die Luft öffnet eine durchsuchbare Bibliothek.
- Umschalt + Rechtsklick auf den Dimensionsboden erstellt immer eine neue Vorlage und fragt zuerst nach einem Namen.
- Ein normaler Rechtsklick auf den Boden wendet die aktuell ausgewählte Vorlage an.
- Lokale Blueprints sind der Standard und stehen auf demselben Client auch in anderen Welten und auf anderen Servern zur Verfügung.
- Server-Blueprints können zentral für alle Spieler bereitgestellt werden.
- Spieler können Servervorlagen lokal herunterladen.
- Operatoren können lokale Vorlagen in die Serverbibliothek veröffentlichen.
- Im Survival-Modus werden beim Anwenden die benötigten Baublöcke aus dem Spielerinventar verbraucht.
- Vorhandene Blöcke werden nicht überschrieben, sondern als Konflikte übersprungen.

### Handbuch und Serververwaltung

- Ein Survival-Handbuch erklärt den vollständigen Ablauf und zeigt die Crafting-Rezepte.
- Ein Adminbuch verwaltet per Drag-and-drop eine Liste verbotener Blöcke für Farmzellen.
- Enderkisten, Blackboxes und Dimensionswerkbänke sind standardmäßig in Farmzellen gesperrt.
- Änderungen am Adminbuch werden in der Serverkonfiguration gespeichert.
- Das Adminbuch erfordert Operator-Berechtigung.

## Bedienung in Kurzform

1. Dimensionswerkbank, Dimensionskern, Inputblock, Outputblock und Handbuch herstellen.
2. Kern in die Werkbank einsetzen, Farmnamen eingeben und mit dem Haken speichern.
3. Optional ein Umgebungs- oder Mobspawn-Upgrade einsetzen.
4. Mit dem Pfeil die Farmzelle betreten und die Farm bauen.
5. Rohstoffe über den blauen Inputblock zuführen und Produkte in den grünen Outputblock leiten.
6. Über das Dimensionsgrundgestein zurückkehren und die Messung abwarten.
7. Den programmierten Kern in der Werkbank lassen oder in eine Blackbox einsetzen.

## Aktuelle Einschränkungen

- Es werden derzeit nur Items gemessen und simuliert. Flüssigkeiten, Energie, Erfahrungspunkte und andere mod-spezifische Ressourcen fehlen noch.
- Die Kompatibilität hängt davon ab, ob eine Maschine ihr Inventar als NeoForge-Item-Handler oder Vanilla-Container bereitstellt.
- Blueprints speichern Blockzustände, aber noch keine Block-Entity-Daten wie Maschinenkonfigurationen, Filter oder Inventarinhalte.
- Blueprints speichern keine Entities, Flüssigkeiten oder Redstone-Zustände außerhalb des normalen Blockzustands.
- Laufende Messungen werden noch nicht über einen Serverneustart hinweg fortgesetzt.
- Die Bibliothek besitzt noch keine Oberfläche zum Umbenennen, Löschen, Sortieren oder Versionieren von Vorlagen.
- Messdauer, Slotanzahl und Zellgröße sind noch nicht konfigurierbar.
- Automatisierte Kompatibilitäts- und Lasttests für große Modpacks fehlen noch.
- Die Benutzeroberflächen und Texturen befinden sich weiterhin im Ausbau.

## Installation

Es gibt noch keine stabile Release-Version. Für Tests muss die Mod aktuell aus dem Quellcode gebaut werden.

1. Java 21 installieren.
2. Repository klonen und den Ordner in VS Code öffnen.
3. Die empfohlenen Java- und Gradle-Erweiterungen installieren.
4. In VS Code die Aufgabe `Minecraft-Client starten (NeoForge)` ausführen.

Ein normaler Build kann unter Windows mit folgendem Befehl erstellt werden:

```powershell
.\gradlew.bat --no-problems-report build
```

Unter Linux oder macOS:

```bash
./gradlew --no-problems-report build
```

Die erzeugte JAR liegt anschließend in `build/libs/`.

## Entwicklung

Der Workspace wurde ursprünglich mit MCreator angelegt. Die zentrale Farm-, Mess-, Simulations-, Netzwerk- und Blueprint-Logik wird inzwischen als normaler Java-Code gepflegt. Die betroffenen MCreator-Elemente sind gesperrt, damit MCreator diese Dateien nicht versehentlich neu generiert.

Für Beiträge sind besonders Tests mit modifizierten Maschinen, Multiplayer-Szenarien und ungewöhnlichen Farmtypen hilfreich. Fehlerberichte und Pull Requests sind willkommen.

## Lizenz

Im Projekt ist derzeit noch keine Lizenz festgelegt. Vor einer Weiterverwendung oder Veröffentlichung sollte eine passende Open-Source-Lizenz ergänzt werden.
