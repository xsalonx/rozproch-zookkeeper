#!/bin/bash
SCRIPT_DIR=$(dirname "$0")
"$SCRIPT_DIR"/../azbin/apache-zookeeper-3.7.1-bin/bin/zkCli.sh "$@"