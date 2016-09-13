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

internal fun Lexer.parseTyFree(): ParseTyFreeResult {
	val (start, next) = posNext()
	return parseTyFreeWithStart(start, next)
}

data class ParseTyFreeResult(val ty: Ty, val pos: Pos, val next: Token)
internal fun Lexer.parseTyFreeWithStart(start: Pos, next: Token): ParseTyFreeResult {
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
	return ParseTyFreeResult(ty, nextStart, nextNext)
}

internal fun Lexer.parseTyNameOrGeneric(): Fn.Head {
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
		Fn.Head.Plain(name)
	else
		Fn.Head.Generic(name, params)
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

internal fun Lexer.parseGenInst(): Arr<Ty> =
	buildUntilNull<Ty> {
		val (start, next) = posNext()
		when (next) {
			Token.Rbracket -> null
			else -> parseTyInlineWithStart(start, next)
		}
	}

//TODO:KILL
internal fun Lexer.parseTyInlineWithStart(start: Pos, next: Token): Ty {
	val part = parsePart(start, next)
	return when (part) {
		is TyOrNon.Ty -> part.ty
		is TyOrNon.NonTy -> unexpected(start, next)
	}
}

private fun Lexer.inline(): Ty {
	val (start, next) = posNext()
	return parseTyInlineWithStart(start, next)
}
