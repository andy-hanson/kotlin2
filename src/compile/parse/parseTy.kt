package compile.parse

import u.*
import compile.err.*
import compile.lex.*
import ast.*

//TODO:KILL, just use `Ty?`
private sealed class TyOrNon {
	data class Ty(val ty: ast.Ty) : TyOrNon()
	data class NonTy(val token: Token) : TyOrNon()
}

internal fun Lexer.parseTyFree(): Triple<Ty, Pos, Token> {
	val (start, next) = posNext()
	return freeWithStart(start, next)
}

private fun Lexer.freeWithStart(start: Pos, next: Token): Triple<Ty, Pos, Token> {
	val first = run {
		val part = parsePart(start, next)
		when (part) {
			is TyOrNon.Ty -> part.ty
			is TyOrNon.NonTy -> unexpected(start, part.token)
		}
	}
	val (restParts, nextStartAndNext) =
		//TODO:NEATER
		buildAndReturn<Ty, Pair<Pos, Token>> { add ->
			loopUntilResult {
				val (start, next) = posNext()
				val part = parsePart(start, next)
				when (part) {
					is TyOrNon.Ty -> {
						add(part.ty);
						null
					}
					is TyOrNon.NonTy ->
						Pair(start, part.token)
				}
			}
		}
	val (nextStart, nextNext) = nextStartAndNext
	val ty =
		if (restParts.isEmpty)
			first
		else
			Ty.Inst(locFrom(start), first, restParts)
	return Triple(ty, nextStart, nextNext)
}

internal fun Lexer.parseTyNameOrGeneric(): FnHead {
	val name = parseTyName()
	val params =
		buildUntilNull {
			val (start, next) = posNext()
			when (next) {
				is Token.TyName ->
					TyParam(locFrom(start), next.name)
				Token.Indent ->
					null
				else ->
					unexpected(start, next)
			}
		}

	return if (params.isEmpty)
		FnHead.Plain(name)
	else
		FnHead.Generic(name, params)
}


private fun Lexer.parsePart(start: Pos, next: Token): TyOrNon =
	when (next) {
		is Token.TyName ->
			TyOrNon.Ty(Ty.Access(locFrom(start), next.name))
		Token.Lbracket -> {
			val head = inline()
			val args = parseGenInst()
			val loc = locFrom(start)
			must(!args.isEmpty, loc, Err.EmptyExpression) //TODO: better error
			TyOrNon.Ty(Ty.Inst(loc, head, args))
		}
		else -> TyOrNon.NonTy(next)
	}

private fun Lexer.parseGenInst(): Arr<Ty> =
	buildUntilNull {
		val (start, next) = posNext()
		when (next) {
			Token.Rbracket -> null
			else -> inlineWithStart(start, next)
		}
	}

//TODO:KILL
private fun Lexer.inlineWithStart(start: Pos, next: Token): Ty {
	val part = parsePart(start, next)
	return when (part) {
		is TyOrNon.Ty -> part.ty
		is TyOrNon.NonTy -> unexpected(start, next)
	}
}

private fun Lexer.inline(): Ty {
	val (start, next) = posNext()
	return inlineWithStart(start, next)
}
