(ns greenhouse.util)

(defmacro dbg
  [x]
  `(let [x# ~x]
     (println '~x "=" x#)
     x#))

(defmacro dbg-with-fn
  [f x]
  `(let [x# ~x
         print-this# (~f x#)]
     (println '(~f ~x) "=" print-this#)
     x#))
