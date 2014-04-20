#!/bin/sh

java -Xmx100m -server -cp lib/\*:target/classes:target/test-classes $*

