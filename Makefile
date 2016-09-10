default: run

clean:
	rm -r doc noze.jar

u.build:
	kotlinc src -include-runtime -d noze.jar

run: u.build
	java -jar noze.jar 

doc:
	java -jar dokka-fatjar.jar src -output doc

.PHONY: clean u.build run doc
