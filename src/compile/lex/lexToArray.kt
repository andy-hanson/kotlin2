package compile.lex

import u.*

data class LexedEntry(val token: Token, val loc: Loc)
fun lexToArray(source: Input): Arr<LexedEntry> {
	return build<LexedEntry> { addEntry ->
		fun add(token: Token, loc: Loc) {
			addEntry(LexedEntry(token, loc))
		}
		lexPlain(::add, Lexer(source))
	}
}

// Returns whether we hit EOF
private inline fun lexInQuote(add: (Token, Loc) -> Unit, l: Lexer): Bool {
	while (true) {
		//TODO: l.locNext
		val (start, next) = l.posNext()
		val loc = l.locFrom(start)
		when (next) {
			Token.EOF ->
				return true
			Token.RCurly -> {
				add(Token.RCurly, loc)
				//TODO: use text
				val quoteDone = l.nextQuotePart().isEndOfQuote
				if (quoteDone)
					return false
			}
			else ->
				add(next, loc)
		}
	}
}

private inline fun lexPlain(add: (Token, Loc) -> Unit, l: Lexer): Unit {
	while (true) {
		val (start, next) = l.posNext()
		when (next) {
			Token.EOF -> return
			is Token.QuoteStart -> {
				add(next, l.locFrom(start))
				if (lexInQuote(add, l))
					return
			}
			else ->
				add(next, l.locFrom(start))
		}
	}
}
