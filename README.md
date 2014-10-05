Author: Do Kook Choe

This code is used for experiments described in "Naive Bayes Word Sense Induction."

USEAGE: 
* cd src/ 
* ./compile.h 
* ./run.h (with appropriate arguments) 

DESCRIPTIONS OF FILES:

src:
*.java are source files
compile.h compiles source files, and run.h runs the compiled Experiment.

data:
smart_common_words.txt contains a list of stopwords from SMART IR engine.
punctuation.txt contains a list of punctuation.
nouns.txt and verbs.txt contains lists of target nouns and verbs respectively. These files are need to execute Experiment.

.:
dom4j.jar is to parse XML input. It is downloaded at http://dom4j.sourceforge.net/.
stanford-corenlp-2012-07-09.jar is to tokenize sentences and lemmatize words. It is downloaded at http://nlp.stanford.edu/.

