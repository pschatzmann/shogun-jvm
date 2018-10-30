## Introduction ##
Shogun (http://shogun.ml/) is a unified and efficient machine learning library which has been implemented in C++ and supports
many languages (Python, Octave, R, Java/Scala, Lua, C#, Ruby, etc) 

Python is the most supported language if you want to implement some Machine Learning. It is much harder to find some good frameworks if you want
to implement Machine Learning in a JVM based environment. Therefore Shogun is am interesting framework because it also supports Java.  

Unfortunately it is very hard to install the libraries which are needed for java. 

## Goal ##

The goal of Shogun-JVM is 
- to make the relevant binaries available via Anaconda 
- to provide all Java dependencies via the Maven Repository


## Supported System Environments ##

Currently we only support the following system environments:
- osx
- linux


## Implementation Details ##

- We provide a Anaconda recipe which builds the Java Ingerface
- The shogin.jar is made available in the Maven Repository
- Shogun-JVM also provides a POM which refers to all dependencies 
- We provide a simple java layer which loads the c++ jndi libraries so that it is not necessary to
  set any additonal environmant variables (LD_LIBRARY_PATH, DYLD_LIBRARY_PATH, java.library.path)

## Documentation ##

The documentation for Shogun can be found at http://shogun.ml/api/latest/classes.html
The documentation for Shogun-JVM with exampels can be found at https://github.com/pschatzmann/shogun-jvm/tree/master/doc/workspace

## Versions ##

All the jars are available for JDK 1.8 and higher 
We provide the latest released Shogun version which currently is 6.1.3
 
## Installation Instruction ##

You can use conda to install the binaries:
	
	conda install -c pschatzmann shogun-jvm 
 
Alternatively you could also use Homebrew in OSX or LinuxBrew in linux:
	
	brew install shogun  


The jars can be installed via Maven:

	Repository: http://software.pschatzmann.ch/repository/maven-public
	groupId: 	org.shogun
	artifactId:	shogun-jvm
	version:	0.0.1-SNAPSHOT

