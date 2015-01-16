(ns de.skamphausen.bobkonf15
  (:require [clojure.java.io :as io] 
            [ring.adapter.jetty :as jetty]
            [compojure.core     :as comp]
            [compojure.handler  :as handler]
            [compojure.route    :as route]
            [hiccup.core        :as html]
            [hiccup.element     :as he]
            [hiccup.page        :as hpage])
  (:import [java.io StringReader]))

(def title "Clojures Implementation von STM")
(def author "Stefan Kamphausen")
(def description "BOB Konference, Berlin, January 2015")


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper
(defn slide [& body]
  [:section body])

;; just the same but I want a different name for it
(defn chapter [title & slides]
  (slide
   (slide [:h1 title])
   slides))


(defmacro defslide [name & body]
  `(defn ~name []
     (slide
      ~@body)))

(defn code-resource []
  (io/resource
   (str (-> *ns*
            .getName 
            name 
            (.replace \- \_)
            (.replace \. \/))
        "_code.clj")))

(defn is-example-label [line]
  (re-find #"^;;:" line))


;; I do not think, I'll ever be able to understand this piece of code
;; again.  Kinda grew at the REPL, sorry to my future self.
(defn code-example-map [lines]
  ;; create map from special comments to the code lines behind
  (into {}
        (for [[[label-comment] lines] 
              (partition 2 (drop-while #(not (is-example-label (first %)))
                                       (partition-by is-example-label
                                                     lines)))
              :let [label (second (re-find #";;:(.*)$" label-comment))]
              :when (not (nil? label))]
         [(keyword label) (apply str (interpose "\n" lines))])))

(def all-code (atom {}))

(defn load-example-code []
  (when-let [res (code-resource)]
    (swap! all-code
           (constantly
            (-> res
                slurp
                (StringReader.)
                (io/reader)
                (line-seq)
                (code-example-map))))))

(defn code [label]
  [:pre
   [:code {:class "clojure"}
    (or
     (get @all-code label)
     (str "MISSING CODE FOR " label))]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slides
(defslide s-title
  [:h1 title]
  [:h2 description]
  [:p [:small "Stefan Kamphausen"]]
  [:div {:style "vertical-align: bottom"}
   (for [[url src] [["http://bobkonf.de/2015/" "img/bob_head_tiny.png"]
                    ["http://www.clojure-buch.de" "img/cover.jpg"]
                    ["http://www.acrolinx.com" "img/acrolinx_logo.png"]
                    ["http://www.clojure.org" "img/clojure.gif"]]]
     [:a {:href url}
      (he/image {:style "margin-left: 2em; border: 0px"} src)])])

(defslide s-credits
  [:h2 "Credits"]
  (he/unordered-list 
   [(list 
     "Präsentation erstellt mit " 
     (he/link-to "https://github.com/hakimel/reveal.js" 
                 "reveal.js"))
    "Clojure Logo (c) Tom Hickey"
    "Acrolinx logo (c) Acrolinx GmbH, mit Erlaubnis verwendet"
    "Buchdeckel (c) 2009-2015 Stefan Kamphausen und dpunkt.verlag"
    "Alle Abbilungen (c) 2009-2015 Stefan Kamphausen"]))

(defslide s-clojure
  [:h2 "Clojure"]
  (he/unordered-list
   ["Lisp für die JVM"
    "Hosted (auch ClojureScript)"
    "Nebenläufige Programmierung eingebaut"
    "Funktional"]))

(defslide s-stm
  [:h2 "Software Transactional Memory - STM"]
  (he/unordered-list
   ["Nur eines der Features in Bezug auf Nebenläufigkeit"
    "Locking ist komplex"
    "Transaktionen wie in DB nun in Speicher"
    "Vergleich GC zu seiner Zeit, STM heute"
    (he/link-to 
     "http://joeduffyblog.com/2010/01/03/a-brief-retrospective-on-transactional-memory/"
     "und Microsoft hat quasi aufgegeben")]))

(defslide s-agenda
  [:h2 "Agenda"]
  (he/ordered-list
   ["Wert, Identität, Zustand"
    "Persistente Datenstrukturen"
    "Referenztypen"
    "Refs"
    "Transaktionen"
    "Fallstricke"
    "Fazit"]))


(defslide s-values
  [:h2 "Werte"]
  (he/unordered-list
   ["Werte sind unveränderlich"
    "Intuitiv für primitive Typen wie Integer"
    "Java-Programmierer kennen das auch von Strings"
    "In Clojure auch Listen, Vektoren, Hash-Maps"
    "Beispiel \"minimal notwendige Anzahl Punkte, um im Tischtennis
einen Satz zu gewinnen\""])
  (code :value))

(defslide s-identity
  [:h2 "Identität"]
  (he/unordered-list
   ["Eine stabile, logische Entität"
    "Kann mit einem Wert verknüpft sein"])
  (code :identity))

(defslide s-state
  [:h2 "Zustand"]
  (he/unordered-list
   ["Verknüpfung einer Identität mit einem Wert zu einem bestimmten
  Zeitpunkt"
    "Wenn die Zeit fortschreitet, kann sich die Verknüpfung ändern"
    "In Clojure modellieren verschiedene Referenztypen die Verknüpfung"])
   (code :state))

(defslide s-persistintro
  [:h2 "Übersicht"]
  (he/unordered-list
   ["Persistenz: erhalte alte Versionen"
    "In Clojure sind Vektoren, Sets, Listen und Hash-Maps persistent"
    "Manipulation -> neue Version"
    "Naiv: Copy on Write -> Performance Charakteristik?"
    "Bitpartitioned Trees ..."]))

(defslide s-persisttree
  [:h2 "Bitpartitioned Trees"]
  (he/image "img/persistent_bitpartition_tree2.png"))

(defslide s-persistchange
  [:h2 "Änderungen"]
  (he/unordered-list
   ["Unveränderliche Daten"
    "Kopiere lediglich Pfad bis zum Wurzelknoten"
    "Shared Structure"
    "Near constant time, log_32(N)"
    "Plus Optimierungen, (Tail)"])
  (he/image "img/persistent_changed_tree.png"))

(defslide s-refintro
  [:h2 "Übersicht"]
  (he/unordered-list
   ["Modellierung der Zeit: Zustände ändern sich"
    "Indirekte Referenz"
    "Leicht atomar zu ändern"
    "long Atomar?"
    "Atom, Ref, Var, Agent"
    "Dereferenzieren"])
  (he/image "img/iref.png"))

(defslide s-atom
  [:h2 "Referenztyp Atom"]
  (he/unordered-list
   ["Einfachster Referenztyp"
    [:code "java.util.concurrent.atomic.AtomicReference"]
    "Zustand ist auch ein Wert: Neue Werte werden berechnet, nicht
  gesetzt (CAS)"
    (list "Berechnung mit " [:code "swap!"])
    "Update-Funktionen ohne Seiteneffekte wegen Wiederholungen"
    (list [:code "deref"] ", Read-Syntax: " [:code "@"])])
  (code :atom))

(defslide s-ref
  [:h2 "Referenztyp Ref"]
  (he/unordered-list
   ["Ähnlich Atom, koordiniert aber Änderungen an mehreren Refs"
    "STM"
    "Lesen immer möglich"
    "Schreiben nur mit Transaktion"
    "Koordiniert lesen ebenfalls mit Transaktion"
    "Validierungen und Benachrichtigungen möglich"]))

(defslide s-ref-basics
  [:h2 "Ref - Grundlegende Beispiele"]
  (he/unordered-list
   [(list "Anlegen mit " [:code "ref"])
    (list "Neu berechnen mit " [:code "alter"])
    (list "(Auch " [:code "commute"] ")")
    "Aber nur in Transaktionen"
    [:code "dosync"]
    "Weitere Argumente gehen an Update-Funktion"
    (list "Keine Seiteneffekte im Body von " [:code "dosync"])])
  (code :ref-basics))

(defslide s-ref-example
  [:h2 "Ein Beispiel mit zwei Refs"]
  (code :ref-example))

(defslide s-ref-background
  [:h2 "Hintergrund"]
  (he/unordered-list
   [(list "Implementation in Java: " 
          [:code "LockingTransaction.java"] ", "
          [:code "Ref.java"] ", "
          "sowie Agents")
    (list [:code "ThreadLocal"] ": Ein Thread nimmt nur an einer
  Transaktion teil")
    "Transaktionen auf verschiedenen Ebenen des Stacks verschmelzen"
    (list [:code "final static AtomicLong" "Absoluter Zähler für
  readPoints und commitPoints"])
    "MVCC, Snapshot-Isolation"
    "Integriert mit Agents"]))

(defslide s-transaction 
  [:h2 "Ablauf einer Transaktion"]
  (he/image "img/stm2.png"))

(defslide s-reftest
  [:h2 "Wiederholungen im Body"]
  (he/unordered-list 
   ["Nicht nur die Update Funktion wird wiederholt,
 sondern der ganze Body"
    "Zwei Refs zum Testen, ein Atom für Kollisionen"
    "Zusätzlich zwei Atoms für Wiederholungen"
    (list "Je eines vor und nach den Aufrufen von " [:code "alter"])]))

(defslide s-reftest-code
  [:h2 "Wiederholungen im Body"]
  (code :reftest))

(defslide s-ouch
  [:h2 "Häufige Probleme"]
  (he/unordered-list
   ["Ein neues Programmierkonstrukt -> neue Probleme"
    "Write Skew: Kann einzelne Refs validieren, aber nicht das
  Ensemble"
    "Verschiedene Geschwindigkeiten von Transaktionen"
    "Historie von Refs"]))

(defslide s-history
  [:h2 "Entwicklung der Historie einer Ref"]
   (he/unordered-list
    ["Zeigt die Auswirkungen auf die interne Historie von Refs"
     "Dank an Chris Houser im IRC"
     "Refs speichern ihre Historie intern, MVCC"
     "Dynamische Anpassung der Historie, wenn kein Snapshot für einen
  Lesezugriff verfügbar"
     (list [:code "future"] 
           " läuft in Thread-Pool irgendwo, irgendwann und führt
  langsame, nur lesende Transaktion aus ")
     (list [:code "dotimes"]
           "-Schleife mit vielen schnellen schreibenden Transaktionen
  im aktuellen Thread"
           )]))

(defslide s-history-code
  [:h2 "Entwicklung der Historie einer Ref"]
  (code :stresstest))

(defslide s-summary
  [:h2 "Try"]
  (he/unordered-list
   ["Einfache Semantik für die koordinierte Manipulation von
mehreren Zuständen"
    "Kein Locking"
    (list 
     "Möglich durch das Zusammenspiel"
     (he/unordered-list
      ["unveränderliche Daten"
       "Shared Structure"
       "performante Implementation von persistenten Datenstrukturen"
       "Historie von Werten"
       "Snapshots"]))]))

(defslide s-summary-but
  [:h2 "Catch"]
  (he/unordered-list
   ["Es gibt neue Probleme"
    "In der Praxis sehr selten notwendig"
    (list "Linus " 
          (he/link-to
           "http://www.realworldtech.com/forum/?threadid=146066&curpostid=146227"
           "sagt"))]))

(defslide s-end
  [:h2 "Finally"
   (he/unordered-list
    ["https://github.com/ska2342/de.skamphausen.bobkonf15"
     "http://clojure.org/state"
     "@stka23"
     "http://www.clojure-buch.de/ (Das komplette Buch auch online)"
     "Bis morgen auf der :clojureD"
     "http://www.clojured.de/"])])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Putting it all together and deliver it
(defn slides []
  [:div {:class "slides"}
   (slide (s-title)
          (s-credits)
          (s-agenda)
          (s-clojure)
          (s-stm))
   (chapter "Wert, Identität, Zustand"
            (s-values)
            (s-identity)
            (s-state))
   (chapter "Persistente Datenstrukturen"
            (s-persistintro)
            (s-persisttree)
            (s-persistchange))
   (chapter "Referenztypen"
            (s-refintro)
            (s-atom))
   (chapter "Refs"
            (s-ref)
            (s-ref-basics)
            (s-ref-example))
   (chapter "Transaktionen"
            (s-transaction)
            (s-ref-background)
            (s-reftest)
            (s-reftest-code))
   (chapter "Fallstricke"
            (s-ouch)
            (s-history)
            (s-history-code))
   (chapter "Fazit"
            (s-summary)
            (s-summary-but)
            (s-end))])


(defn presentation []
  (html/html 
   [:head
    [:title title]
    (hpage/include-css "css/reveal.min.css"
                       "css/theme/moon.css"
                       "lib/css/zenburn.css")
    [:meta {:name "viewport" 
            :content (str "width=device-width, initial-scale=1.0, "
                          "maximum-scale=1.0, user-scalable=no")}]
    [:meta {:name "apple-mobile-web-app-capable"
            :content "yes"}]
    [:meta {:name "apple-mobile-web-app-status-bar-style"
            :content "black-translucent"}]]
   [:body
    [:div {:class "reveal"} (slides)]
    

    (hpage/include-js "lib/js/head.min.js"
                      "js/reveal.min.js")
    (he/javascript-tag
     "Reveal.initialize({
      controls: true,
      center: true,
      history: true,
      theme: Reveal.getQueryHash().theme,
      transition: Reveal.getQueryHash().transition || 'default',
      dependencies: [
      { src: 'lib/js/classList.js', 
        condition: function() { return !document.body.classList; } },
      { src: 'plugin/highlight/highlight.js',
        async: true,
        callback: function() { hljs.initHighlightingOnLoad(); } },
      { src: 'plugin/zoom-js/zoom.js',
        async: true,
        condition: function() { return !!document.body.classList; } },
      ]

      });")]))



(comp/defroutes app
  (comp/GET "/" []   (presentation))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

(def jetty (atom nil))

(defn start []
  (when @jetty
    (.stop @jetty))
  (load-example-code)
  (swap! jetty
         (constantly
          (jetty/run-jetty #'app 
                           {:port 9999 :join? false}))))



