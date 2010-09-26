(ns static.io
  (:use static.config :reload-all)
  (:import (com.petebevin.markdown MarkdownProcessor)
	   (java.io File)
	   (org.apache.commons.io FileUtils)))

(defn- markdown [txt] (.markdown (MarkdownProcessor.) txt))

(defn- split-file [content]
  (let [idx (.indexOf content "---" 4)] 
    [(.substring content 4 idx) (.substring content (+ 3 idx))]))

(defn- prepare-metadata [metadata]
  (reduce (fn [h [_ k v]] 
	    (let [key (keyword k)]
	      (assoc h key v)))
	  {} (re-seq #"([^:]+): (.+)(\n|$)" metadata)))

(def read-markdown 
     (memoize
      (fn [file]
	(let [[metadata content] 
	      (split-file (slurp file :encoding (:encoding (config))))]
	  [(prepare-metadata metadata) (markdown content)]))))

(defn dir [dir]
  (cond (= dir :templates) (str (:in-dir (config)) "templates/")
	(= dir :public) (str (:in-dir (config)) "public/")
	(= dir :site) (str (:in-dir (config)) "site/")
	(= dir :posts) (str (:in-dir (config)) "posts/")
	:default (throw (Exception. "Unknown Directory."))))

(defn list-files [d]
  (let [d (File. (dir d))] 
    (if (.isDirectory d)
      (sort
       (filter
        #(let [[metadata _] (read-markdown %)
               published? (:published metadata)]
           (if (or (nil? published?)
                   (= published? "true"))
             true false))
        (FileUtils/listFiles d (into-array ["markdown"]) true))) [])))

(def read-template
     (memoize
      (fn [template]
	(-> (str (dir :templates) template)
	    (File.) 
	    (slurp :encoding (:encoding (config)))
	    read-string))))

(defn write-out-dir [file str]
  (FileUtils/writeStringToFile 
   (File. (:out-dir (config)) file) str (:encoding (config))))
