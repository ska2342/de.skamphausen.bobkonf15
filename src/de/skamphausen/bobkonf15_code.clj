(ns de.skamphausen.bobkonf15-code)

;; wrapping everything in a comment.
(comment

;;:value
"Ich bin ein String-Literal: ein Wert"
[:das :ist :ein :Vector :von :Keywords]
2001 ; Eine Zahl. Ein Jahr? Ein Filmtitel?

;;:identity
(def gruendungsjahr-fsfe)
(def einfuehrungsjahr-11-punkte-saetze)

;;:state
(def gruendungsjahr-fsfe 2001)
(def einfuehrungsjahr-11-punkte-saetze 2001)
(def identitaet "def verknüpft Wert und Identität")

;;:atom
(def atm (atom "Eins"))
(deref atm)
(swap! atm (constantly 1))
@atm

;;:ref-basics
(def eine-ref (ref 1))
(alter eine-ref inc)
(dosync (alter eine-ref inc))
(deref eine-ref)
(dosync (alter eine-ref + 5))
@eine-ref

;;:ref-example
;; Atom zum Zählen von Kollisionen
(def kollisionen (atom 0))
;; Zwei Refs
(def hochzaehler (ref 0))
(def runterzaehler (ref 100))
;; Funktionen mit Kollisions-Update
(defn inc-mit-zaehl [value]
  (swap! kollisionen inc)
  (inc value))
(defn dec-mit-zaehl [value]
  (swap! kollisionen inc)
  (dec value))
;; Die Transaktions
(defn stm-funktion []
  (dosync
   (alter hochzaehler   inc-mit-zaehl)
   (alter runterzaehler dec-mit-zaehl)))
;; Erzeuge 100 Thread Objekte (Runnable, Callable)
(dotimes [_ 100]
  (.start (new Thread stm-funktion)))
;; Ergebnisse
@hochzaehler ; 100
@runterzaehler ; 0
;; # ... und die Kollisionen?
;; - Ohne Wiederholungen müsste das Atom den Wert 200 liefern
@kollisionen ; > 200

;;:reftest
(defn reftest []
  (let [r1 (ref 0)                     ; test ref
        r2 (ref 100)                   ; other test ref
        cl (atom 0)                    ; collisions in update fn
        c1 (atom 0)                    ; repetitions dosync pre alter
        c2 (atom 0)                    ; repetitions dosync post alter
        ic (fn [x] (swap! cl inc) (inc x)) ; inc w/ collision
        dc (fn [x] (swap! cl inc) (dec x)) ; dec w/ collision
        f  (fn [x] 
             (dosync
              (swap! c1 inc)
              (alter r1  ic)
              (alter r2  dc)
              (swap! c2 inc)))]
    (dotimes [i 100]
      (.start (Thread. #(f i))))
    (Thread/sleep 1000) ; waiting
    [@r1  @r2  @cl @c1 @c2]))

;;:stresstest
(defn stresstest [hmin hmax]
  (let [r (ref 0 :min-history hmin :max-history hmax)
        langsame-versuche (atom 0)]
      
    (future      
      (dosync (swap! langsame-versuche inc)
              (Thread/sleep 200)
              @r)
      (println "ref ist:" @r
               "history:" (.getHistoryCount r)
               "nach:"    @langsame-versuche "Versuchen"))
      
    (dotimes [i 500]
      (Thread/sleep 10)
      (dosync (alter r inc))) 
    :done))

(stresstest 0 10)
(stresstest 0 30)
(stresstest 15 30)

;;:ignorethissection
)
