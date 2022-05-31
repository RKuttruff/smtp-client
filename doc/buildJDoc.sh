#!/bin/bash

find ../src/ -type f -name "*.java" | xargs javadoc -private
