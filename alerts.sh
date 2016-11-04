#! /bin/bash

export VAULT_ADDR="https://vault.example.com:8200" # Path to your Vault
export VAULT_ROLE="thisismyfancyID" # Your Vault AppRole ID
export VAULT_SECRET="thisismyfancysecret" # Your Vault AppRole SECRET

java -jar alerts.jar > /dev/null 2>&1
# STOUT can be ignored as log information is saved to log file.

# Environment variables don't need to by unset, because they don't exist outside of this session
