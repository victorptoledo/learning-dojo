#!/bin/sh
# Prepares the container, then starts the catalog. Written in a hurry before the launch.
catalog migrate
catalog serve >> /var/log/catalog.log 2>&1
