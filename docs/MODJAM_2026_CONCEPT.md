# BlackBox: Echoes of Industry

## Kurzpitch

Eine gebaute Farm hinterlaesst ein zeitliches Echo. Die BlackBox beobachtet nicht nur, **was** eine Maschine produziert, sondern **wie und wann** ihre Produktion geschieht. Danach kann ein Dimension Core dieses vergangene Verhalten wiedergeben, ohne die urspruengliche Farm dauerhaft zu laden.

## Warum das Thema passt

Das Projekt behandelt Vergangenheit als beweisbare Ressource:

- Eine Farm kann nur wiederholen, was sie zuvor wirklich getan hat.
- Der Core bewahrt Produktionsspitzen, Ruhephasen, Inputs und Verluste als Aufzeichnung.
- Die BlackBox spielt diese Aufzeichnung in der Gegenwart als "Echo" ab.
- Die Anti-Duplikations-Bilanz wird Teil der Erzaehlung: Die Vergangenheit kann nichts erzeugen, das dort nie existiert hat.
- Eine erneute Messung ueberschreibt die alte Erinnerung mit einer neuen Version der Farm.

Damit ist die Performance-Optimierung nicht nur Technik im Hintergrund, sondern die zentrale Spielidee: Der Spieler ersetzt eine permanent laufende Maschine durch ihre verifizierte Erinnerung.

## Spielerischer Ablauf

1. Der Spieler fertigt eine Dimensionswerkbank und einen leeren Dimension Core.
2. Er waehlt Zellengroesse und Umgebung und baut eine echte Farm in einer isolierten Zelle.
3. Beim Verlassen beginnt die Aufzeichnung. Items, Fluessigkeiten, Energie, Mob-Verbrauch sowie Produktionszeitpunkte werden bilanziert.
4. Nach erfolgreicher Messung leuchtet der Core und enthaelt das Echo der Farm.
5. In Werkbank oder BlackBox verbraucht das Echo dieselben Inputs und erzeugt dieselben Gesamtmengen, ohne die Farmzelle weiter zu laden.
6. Ohne Stabilitaetsupgrade bleiben Spitzen und Pausen erhalten und variieren leicht. Mit Stabilitaet wird das Echo geglaettet.
7. Der Spieler kann zur Farm zurueckkehren, sie umbauen und eine neue Erinnerung aufnehmen.

## Vorgeschlagene Jam-Praesentation

Diese Punkte sind Konzept und derzeit **nicht implementiert**:

- **Echo-Imprint:** Waehrend der Messung erscheinen kurze, transparente Nachbilder an aktiven Maschinen und Transportwegen.
- **Core Memory View:** Eine kleine Zeitleiste zeigt Peaks, Pausen und den letzten erfolgreichen Messdurchlauf.
- **Past/Present Audio:** Leise, rhythmische Impulse folgen der aufgezeichneten Produktion; das Stabilitaetsupgrade macht den Rhythmus gleichmaessig.
- **Archive Pedestal:** Programmierte Cores koennen als Sammlung vergangener Farmversionen ausgestellt und verglichen werden.
- **Echo Naming:** Begriffe wie "Record", "Imprint", "Echo" und "Stabilize" ersetzen rein technische Texte, ohne die Bedienung unklar zu machen.
- **Showcase Blueprint:** Eine kleine historische Eisenfarm demonstriert sichtbar den Wechsel von realer Maschine zu ausgelagertem Echo.

Die staerkste Demo waere ein Vorher/Nachher-Vergleich: zuerst eine sichtbar laufende Farm mit Entitaeten und Maschinen, danach dieselbe Produktion aus einer einzelnen BlackBox bei entladener Farmzelle.

## Mob-Echos

Mobs eignen sich als besonders verstaendliches Beispiel fuer die Bilanz:

- Ein dauerhaft verschwundener Villager wird als echter Input des Echos gespeichert.
- Natuerliche Spawns gelten nicht als vom Spieler bereitgestellte Ressource.
- Spawner-Mobs werden nicht berechnet, solange der erzeugende Spawner weiterhin Teil der Farm ist.
- Boot und Lore bilden eine sichtbare, vanilla-nahe Art, Mobs in eine Farmzelle oder als BlackBox-Input zu transportieren.

## Bewertungspotenzial

Die offiziellen Kriterien gewichten Originalitaet, Spielspass und Optik mit jeweils 30 Prozent sowie Downloads mit 10 Prozent.

- **Originalitaet:** Eine Performance-Mod wird als zeitbasierte Automationsmechanik spielbar gemacht.
- **Spielspass:** Bauen, messen, optimieren, vergleichen und erneut aufnehmen bilden eine klare Schleife.
- **Optik:** Nachbilder, farbige Core-Zustaende und eine lesbare Produktionszeitleiste koennen das abstrakte System sichtbar machen.
- **Downloads:** Der konkrete Nutzen fuer Modpacks und Server ist leicht erklaerbar und demonstrierbar.

## Teilnahme-Checkliste

- Die aktuelle Mod basiert auf Minecraft 1.21.1. Fuer die Teilnahme ist ein separater Port auf **Minecraft 26.1 oder neuer** zwingend erforderlich.
- Das eingereichte CurseForge-Projekt muss neu sein und darf nicht vor dem 21. Juli 2026 auf CurseForge veroeffentlicht worden sein.
- Die Einreichung muss bis **1. September 2026, 17:00 Uhr GMT+3** live und eingereicht sein. Wegen der CurseForge-Moderation sollte der Upload mehrere Tage vorher erfolgen.
- Java-Mods benoetigen einen sichtbaren GitHub-Link oder ein geteiltes privates Repository.
- KI-generierte Projekt-Avatare und Galerie-Bilder sind ausdruecklich verboten. Fuer die Einreichung sollten echte Ingame-Screenshots und selbst erstellte Grafiken verwendet werden.
- Teilnehmer muessen mindestens 18 Jahre alt beziehungsweise im eigenen Land volljaehrig sein.

Offizielle Quellen:

- [Minecraft ModJam 2026](https://mod.curseforge.com/minecraft/modjam2026/)
- [Terms and Conditions](https://mod.curseforge.com/minecraft/modjam2026/terms-and-conditions/)

## Empfohlener Projektumfang

Vor einer Jam-Einreichung sollte Stabilitaet vor neuen Nebenfunktionen stehen:

1. Port auf Minecraft 26.1 in einem eigenen Branch.
2. Messung, Chunk-Laden, Serverneustart und Anti-Duplikation mit mehreren Modpack-Maschinen testen.
3. Eine kurze, sichere Beispiel-Farm und ein gutes erstes Spielerlebnis fertigstellen.
4. Nur danach Echo-Nachbilder, Zeitleiste und Audio als sichtbare Jam-Schicht ergaenzen.

Der Kern des Konzepts funktioniert bereits ohne zusaetzliche Lore-Systeme: Die gespeicherte Produktionskurve **ist** das Echo der Vergangenheit.
