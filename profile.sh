#!/bin/sh

java -Xmx256m -server -cp lib/\*:target/classes:target/test-classes \
 -Xrunhprof:cpu=samples,depth=10,verbose=n,interval=2 \
$*

