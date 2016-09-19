default: run

clean:
	rm -r build .gradle

build:
	./gradlew

run:
	# TODO: enableAssertions
	./gradlew run

run-jar:
	java -enableassertions -jar build/libs/shadow-all.jar

.PHONY: clean build run
