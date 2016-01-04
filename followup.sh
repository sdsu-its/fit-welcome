#! /bin/bash

export KSPATH="ks.example.com" # Path to your Key Server
export KSKEY="thisismyfancykeyserverkey" # Your KeyServer Application Key

java -jar followup.jar > /dev/null 2>&1
# STOUT can be ignored as log information is saved to log file.

# Envoirment variables don't need to by unset, because they don't exist outside of this session
