package compile.parse

import ast.LocalDeclare
import u.*
import compile.err.*
import compile.lex.Lexer
import compile.lex.Token

internal fun<T> Lexer.unexpected(start: Pos, token: Token): T =
	raise<T>(locFrom(start), Err.Unexpected(token))

internal fun Lexer.expect(start: Pos, expected: Token, actual: Token) {
	if (expected !== actual)
		unexpected<Unit>(start, actual)
}

internal fun Lexer.mustSkip(expected: Token) {
	val (start, actual) = posNext()
	expect(start, expected, actual)
}

internal fun Lexer.parseName(): Sym {
	val (start, next) = posNext()
	return when (next) {
		is Token.Name -> next.name
		else -> unexpected(start, next)
	}
}

internal fun Lexer.parseLocalDeclare(): LocalDeclare {
	val (loc, name) = parseNameWithLoc()
	return LocalDeclare(loc, name)
}

internal fun Lexer.parseNameWithLoc(): Pair<Loc, Sym> {
	val start = curPos()
	val name = parseName()
	return Pair(locFrom(start), name)
}

// TODO: move to parseTy.kt?
internal fun Lexer.parseTyName(): Sym {
	val (start, next) = posNext()
	return when (next) {
		is Token.TyName -> next.name
		else -> unexpected(start, next)
	}
}
