#!/bin/bash

RUNS=100

for i in $(seq 1 $RUNS); do
  echo "=== Run $i/$RUNS ==="
  ./mvnw test -Dtest=tools.jackson.databind.GitHub5615JavaTest \
      --file pom.xml \
      --settings ~/.m2-test/settings.xml

  EXIT_CODE=$?
  if [ $EXIT_CODE -ne 0 ]; then
    echo "❌ mvn test FAILED at run $i"
    exit $EXIT_CODE
  fi
done

echo "✅ All $RUNS runs passed successfully"