= Welcome to the Apache Camel Examples

== Introduction

This project contains the various examples for working with Apache
Camel. The examples can be run using Maven. When using the Maven
command, Maven will attempt to download the required dependencies from a
central repository to your local repository.
View the individual example READMEs for details.

=== Executing

Master branch should only be used for development purposes, which will be pointing
to the SNAPSHOT version of the next release.

To execute the examples, checkout into the tag for the latest release. For example:

`$ git checkout tags/camel-examples-4.0.0`

Then, install the root pom:

`$ mvn install`

After that, you should be able to execute the examples following each example's
readme's instructions.
