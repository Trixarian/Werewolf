#!/bin/sh
echo "Starting Wolfbot..."
nohup java -classpath .:lib/pircbot.jar org.jibble.pircbot.llama.werewolf.Werewolf > /dev/null 2>&1 &