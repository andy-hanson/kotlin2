package compile.lex

import u.*

data class LexedEntry(val token: Token, val loc: Loc)
fun lexToArray(source: Input): Arr<LexedEntry> {
	return build {
		lexPlain(Lexer(source))
	}
}

// Returns whether we hit EOF
private inline fun ArrayBuilder<LexedEntry>.lexInQuote(l: Lexer): Bool {
	while (true) {
		//TODO: l.locNext
		val (start, next) = l.posNext()
		val loc = l.locFrom(start)
		when (next) {
			Token.EOF ->
				return true
			Token.RCurly -> {
				add(LexedEntry(Token.RCurly, loc))
				//TODO: use text
				val quoteDone = l.nextQuotePart().isEndOfQuote
				if (quoteDone)
					return false
			}
			else ->
				add(LexedEntry(next, loc))
		}
	}
}

private inline fun ArrayBuilder<LexedEntry>.lexPlain(l: Lexer) {
	while (true) {
		val (start, next) = l.posNext()
		when (next) {
			Token.EOF -> return
			is Token.QuoteStart -> {
				add(LexedEntry(next, l.locFrom(start)))
				if (lexInQuote(l))
					return
			}
			else ->
				add(LexedEntry(next, l.locFrom(start)))
		}
	}
}
