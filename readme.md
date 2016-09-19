## Setup IntelliJ

It is easiest to run using [IntelliJ IDEA](https://www.jetbrains.com/idea/).

Install kotlin 1.1 using [these](https://kotlinlang.org/docs/tutorials/command-line.html) instructions.

You'll also need to add the `libs` directory as a depencency, following [these](https://stackoverflow.com/questions/1051640/correct-way-to-add-external-jars-lib-jar-to-an-intellij-idea-project#1051705) instructions.

Then run `cli/app.kt`. Remember to add the `-enableassertions` VM option.


## Building executable JAR

	`make build` makes a JAR and `make run-ar` runs it. There should be no classpath needed.


## Running with gradle

	You can also use `make run` to directly run the project. This does not yet support assertions.

