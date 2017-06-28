#!/bin/bash

if [ $# -lt 1 ]; then
  echo "Usage: test_loop.sh <sleep seconds> <gradle params>"
  echo "e.g. test_loop.sh 10 -x :test :indigo-examples:test --debug" 
  exit 1
fi

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

sleep=$1
shift
params="$*"

echo -e "${CYAN}Sleep:  ${sleep} s${NC}"
echo -e "${CYAN}Params: ${params}${NC}"

targetBase=build/looped
mkdir -p $targetBase 

while [ true ]; do
  date=`date +%F-%H%M%S`
  ./gradlew cleanTest $params
  exitCode=$?
  if [ "${exitCode}" == "1" ]; then
    target=${targetBase}/${date}
    mv build/reports/tests/test $target
    echo -e "${RED}Gradle process completed with code ${exitCode};${NC} saved test results in ${target}"
    exit 1
  elif [ "${exitCode}" == "130" ]; then
    echo -e "\n${YELLOW}Gradle process interrupted; exiting.${NC}"
    exit 130
  else
    echo -e "${GREEN}Gradle process completed with code ${exitCode}.${NC}"
  fi
  sleep $sleep
done
