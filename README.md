Author: Do Kook Choe

This code is used for experiments described in "Naive Bayes Word Sense Induction."  
You can download the SemEval 2010 Word Sense Induction task dataset from: http://www.cs.york.ac.uk/semeval2010_WSI/datasets.html.  

USEAGE:   
1. cd src/  
2. ./compile.h  
3. ./run.h (with appropriate arguments)  

DESCRIPTIONS OF FILES  
in src:  
1. *.java are source files.  
2. compile.h compiles source files.  
3. run.h executes Experiment.class.   

in data:  
1. smart_common_words.txt contains a list of stopwords from SMART IR engine.  
2. punctuation.txt contains a list of punctuation.  
3. nouns.txt and verbs.txt contains lists of target nouns and verbs respectively. These files are need to execute   Experiment.

jars:  
1. dom4j.jar is to parse XML input. It is downloaded at http://dom4j.sourceforge.net/.  
2. stanford-corenlp-2012-07-09.jar is to tokenize sentences and lemmatize words. It is downloaded at http://nlp.stanford.edu/.  
