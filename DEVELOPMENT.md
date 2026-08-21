Kurzzusammenfassung ChatGPT was es gibt
Einfügen Claude Code  
	App kann bereits...
	sqlite datenbank einfügen, die trackt wie lange die workouts, mit jeweils übungen gemacht wurden
Parallel über Skills einarbeiten
Neue Funktionen für bessere Usability, schöneres Design
	Workout abbrechen einfügen
	Bei Übung zu Workout hinzufügen direkt Zeit auswählen lassen
	Bei Übung erstellen/Bearbeiten vorschaubild, statt Dateiname anzeigen
	Videos in Dauerschleife abspielen
	Bilder in der usprünglichen ausrichtung anzeigen (Nicht drehen)
	Gibt es sonst noch was du auf anhieb besser machen würdest?
		Soll "Workout abbrechen" eine Bestätigung verlangen, oder sofort abbrechen? - Mit Bestätigungsdialog
		Wie soll die Zeit-Auswahl beim Hinzufügen einer Übung zum Workout aussehen?	- Freies Zahlenfeld
		Sollen diese zwei zusätzlichen Verbesserungen mit ins Paket? - Bestätigung vor Löschen, Echtes Drag & Drop zum Umsortieren
/anthropic-skills:wayfinder design & cleanup codebase, outsource repository informations to sqlite database
	setup fehlt ->
	Setup: /anthropic-skills:setup-matt-pocock-skills
		-> erstmal alle Skills installieren
		X Fehlversuche in cmd, Windows Powershel, Git Bash
		claude: plugins install mattpocock-skills
	GitGub CLI installieren - grilling/domain-modeling/research/prototype 
		+ authentifizieren - gh auth login
	Klärung "Repository-Informationen in SQLite auslagern"
		Rest (Exercises & Workouts) war doch schon in sqlite
		-> Verfifizierung mit Android Studio Gemini alles in sqlite?
			Database Inspector
	Claude Hinweis DatabaseProvider.java fallbackToDestructiveMigration(), was bei zukünftigen Schema-Änderungen ohne passende Migration alle Daten löschen würde
	/anthropic-skills::wayfinder about potential software design improvements
		/grilling Abfrage 
			alle Tickets als einzige, festgelegte Architektur-Entscheidung
			Umfang: reines Software-Design für die ganze App
			Rahmenbedingungen: App bleibt Java, Vorschläge leichtgewichtig
			konkretere Ziel-Architektur 5Qs
			Frontier-Runde (breitflächig) 2Qs
		Lightweight MVVM Migration (Karte)
			Design the AppContainer, Application wiring & background-execution pattern — frei, unblockiert (Prototype-Ticket)
			Migrate WorkoutListFragment to ViewModel (pilot)
			Migrate PlayerFragment to ViewModel
			Migrate WorkoutEditFragment to ViewModel
			Migrate ExerciseListFragment to ViewModel
			Migrate ExerciseEditFragment to ViewModel
			Migrate HistoryListFragment to ViewModel
/wayfinder https://github.com/bettinawoste-wq/Workout/issues/1 		# Design the AppContainer, Application wiring & background-execution pattern
	-> Prototyp Signal Path Szenarien (Happy Path, Rotation, Screen verlassen während des Ladens, zwei schnelle Anfragen hintereinander)
/wayfinder https://github.com/bettinawoste-wq/Workout/issues/1	 	# Migrate WorkoutListFragment to ViewModel (pilot)
	/grilling Wie wird die Liste aktualisiert, wenn man von "Workout bearbeiten" zurückkommt? Room-eigenes reaktives LiveData
		Migrate HistoryListFragment to ViewModel schon vermerkt). Die Karte Lightweight MVVM Migration ist aktualisiert
/wayfinder https://github.com/bettinawoste-wq/Workout/issues/1  	# Migrate PlayerFragment to ViewModel


### TODO ###
Usability
	was wenn übung gelöscht, die in einem Workout ist
	Bilder rein/rauszoomen 
	Option für übungen links/rechts auswählen
	Bildformat quadratisch
	Standardbild, wenn keins angegeben
	abschluss success ton
	Übersicht Übungen der Workouts mit kleinem Bild