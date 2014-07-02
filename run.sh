#!/bin/sh

java -Xmx256m -server -cp lib/\*:target/classes:target/test-classes $*

