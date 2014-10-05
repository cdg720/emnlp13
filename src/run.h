#!/bin/bash

if [ "$#" -ne 10 ]
then
    echo "usage: ./run.sh outputPath power alpha beta senseNum windowSize maxIter burnIn training_data/ test_data/"
    echo "example: ./run.sh out.txt .4 .02 .1 4 50 2000 1000 training_data test_data/"
    exit
fi

java -cp ../stanford-corenlp-2012-07-09.jar:../dom4j-2.0.0-ALPHA-2.jar:. Experiment $1 $2 $3 $4 $5 $6 $7 $8 $9 ${10}


