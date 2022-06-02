#!/bin/bash

find ../src/ -type f -name "*.java" | xargs javadoc -private  -link https://docs.oracle.com/en/java/javase/11/docs/api/ -d javadoc
