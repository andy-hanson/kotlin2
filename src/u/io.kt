package u

class EndOfFile : Exception()

interface Input {
	// May throw EndOfFile
	fun readChar(): Char
}
