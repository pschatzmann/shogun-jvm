#!/bin/bash
svn update --username pschatzmann --password $1 /root/shogun-java
cd /shogun-java/utils/conda/shogun-jvm 
conda build . --debug
