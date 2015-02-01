#!/bin/bash
cd ..
sbt assembly
scp target/scala-2.11/server.jar $1:/root/herold-server.jar
ssh -t $1 "supervisorctl reload"
