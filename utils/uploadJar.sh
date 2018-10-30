#!/bin/bash
# Uplaod shogun.jar to Maven repository
mvn deploy:deploy-file -Dpassword=$1 -DgroupId=org.shogun \
    -DartifactId=shogun \
    -Dversion=6.1.3 \
    -Dpackaging=jar \
    -Dfile=shogun.jar \
    -DgeneratePom=true \
    -DrepositoryId=work \
    -Durl=http://nuc.local:8081/repository/maven-releases/
