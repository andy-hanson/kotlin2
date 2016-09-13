package compile.parse

import u.*
import ast.*
import compile.lex.*

internal fun Lexer.parseDecl(start: Pos, next: Token): Decl =
	when (next) {
		Token.Rt ->
			parseRt(start)
		Token.Vt ->
			parseVt(start)
		Token.Ft ->
			parseFt(start)
		Token.Fn ->
			parseFn(start)
		else ->
			unexpected(start, next)
	}

private fun Lexer.parseRt(start: Pos): DeclTy {
	val head = parseTyNameOrGeneric()
	val props = buildLoop {
		val start = curPos()
		val name = parseName()
		val (ty, cont) = parseTyThenNewlineOrDedent()
		val prop = Property(locFrom(start), name, ty)
		Pair(prop, cont)
	}
	val loc = locFrom(start)
	return when (head) {
		is Fn.Head.Plain -> Rt(loc, head.name, props)
		is Fn.Head.Generic -> GenRt(loc, head.name, head.params, props)
	}
}

private fun Lexer.parseVt(start: Pos): DeclTy {
	val head = parseTyNameOrGeneric()
	val tys = buildLoop(this::parseTyThenNewlineOrDedent)
	val loc = locFrom(start)
	return when (head) {
		is Fn.Head.Plain -> Vt(loc, head.name, tys)
		is Fn.Head.Generic -> GenVt(loc, head.name, head.params, tys)
	}
}

private fun Lexer.parseFt(start: Pos): DeclTy {
	val head = parseTyNameOrGeneric()
	val signature = parseSignatureThen(Token.Dedent)
	val loc = locFrom(start)
	return when (head) {
		is Fn.Head.Plain -> Ft(loc, head.name, signature)
		is Fn.Head.Generic -> GenFt(loc, head.name, head.params, signature)
	}
}

// Returns 'true' iff it ended on a newline (as opposed to a dedent
private fun Lexer.parseTyThenNewlineOrDedent(): Pair<Ty, Bool> {
	val (ty, nextStart, next) = parseTyFree()
	return Pair(ty, when (next) {
		Token.Newline -> true
		Token.Dedent -> false
		else -> unexpected(nextStart, next)
	})
}

private fun Lexer.parseFn(start: Pos): Fn {
	val head = parseFnHead()
	val signature = parseSignatureThen(Token.Indent)
	val value = parseBlock()
	return Fn(locFrom(start), head, signature, value)
}

private fun Lexer.parseFnHead(): Fn.Head {
	val (start, next) = posNext()
	return when (next) {
		is Token.Name ->
			Fn.Head.Plain(next.name)
		Token.Lbracket -> {
			val name = parseName()
			val params =
				//TODO: this is similar to code in parseTyOrGeneric
				buildUntilNull {
					val (start, next) = posNext()
					when (next) {
						is Token.TyName -> TyParam(locFrom(start), next.name)
						Token.Rbracket -> null
						else -> unexpected(start, next)
					}
				}
			assert(!params.isEmpty)
			Fn.Head.Generic(name, params)
		}
		else ->
			unexpected(start, next)
	}
}
