#!/bin/sh

template="{\"timestamp\": \"%s\", \"severity\": \"%s\", \"message\": \"%s\"}"

while [ 1 ]
do
   WAIT=$(shuf -i $1-$2 -n 1)
   sleep $(echo "scale=4; $WAIT/1000" | bc)
   I=$(shuf -i 1-4 -n 1)
   timestamp=`date -Iseconds`
   output="will be replaced in cases below"
   case "$I" in
      "1") output=$(printf "${template}" "${timestamp}" "ERROR" "An error is usually an exception that has been caught and not handled.")
      ;;
      "2") output=$(printf "${template}" "${timestamp}" "INFO" "This is less important than debug log and is often used to provide context in the current task.")
      ;;
      "3") output=$(printf "${template}" "${timestamp}" "WARN" "A warning that should be ignored is usually at this level and should be actionable.")
      ;;
      "4") output=$(printf "${template}" "${timestamp}" "DEBUG" "This is a debug log that shows a log that can be ignored.")
      ;;
   esac
   echo "${output}"
done