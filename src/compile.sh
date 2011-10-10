#!/bin/sh
echo "Compile Wolfbot..."
javac -classpath .:../lib/pircbot.jar *.java
echo "Done Compiling..."
echo "Moving files..."
mv Vote.class ../org/jibble/pircbot/llama/werewolf/objects/
mv *.class ../org/jibble/pircbot/llama/werewolf/
echo "Done Moving!"
echo "Bot should be compiled and ready now ;)"