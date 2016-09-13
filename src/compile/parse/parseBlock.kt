package compile.parse

import u.*
import ast.*
import compile.err.*
import compile.lex.*

private val x = Ctx.ExprOnly

private enum class Ctx {
	Line,
	/** Like Line, but forbid `=` because we're already in one. */
	ExprOnly,
	/** Parse an expression and expect `)` at the end */
	Paren,
	/** Look for a QuoteEnd */
	Quote,
	CsHead,
	List
}

private sealed class Next {
	class NewlineAfterEquals(val pattern: Pattern) : Next()
	object NewlineAfterStatement : Next()
	object EndNestedBlock : Next()
	object CtxEnded : Next()
}

private fun partsToPattern(loc: Loc, parts: MutableList<Expr>): Pattern {
	fun<T> fail(): T =
		raise<T>(loc, Err.PrecedingEquals)
	fun partToPattern(part: Expr): Pattern =
		when (part) {
			is Access ->
				Pattern.Single(LocalDeclare(part.loc, part.name))
			else -> fail()
		}
	return when (parts.size) {
		0 -> fail()
		1 -> partToPattern(parts[0])
		else -> Pattern.Destruct(loc, Arr.fromMapped(parts, ::partToPattern))
	}
}

//TODO:NEATER!
//TODO: name parse_expr_with_start
private data class ExprRes(val expr: Expr, val next: Next)
private fun Lexer.parseExprWithNext(exprStart: Pos, startToken: Token, ctx: Ctx): ExprRes {
	val parts = mutableListOf<Expr>()
	fun addPart(part: Expr) { parts.add(part) }
	fun anySoFar() = !parts.isEmpty()
	fun finishLoc() = locFrom(exprStart)
	fun finishRegular(): Expr {
		val loc = finishLoc()
		return when (parts.size) {
			0 -> raise(loc, Err.EmptyExpression)
			1 -> parts[0]
			else -> {
				val head = parts[0]
				val tail = Arr.tail(parts)
				Call(loc, head, tail)
			}
		}
	}
	fun dotDot(left: Expr): ExprRes {
		val (rightStart, next) = posNext()
		return when (next) {
			Token.Colon -> TODO("..:")
			else -> {
				val (right, next) = parseExprWithNext(rightStart, next, ctx)
				ExprRes(Partial(finishLoc(), left, Arr.of(right)), next)
			}
		}
	}

	//TODO:???
	var loopStart = exprStart
	var loopNext = startToken
	fun<T> notExpected(): T = unexpected(loopStart, loopNext)
	fun readAndLoop() {
		loopStart = curPos()
		loopNext = nextToken()
	}
	//Null means: keep looping
	fun afterType(ty: Ty): ExprRes? {
		val (start, next) = posNext()
		when (next) {
			Token.At, Token.AtAt -> {
				val kind = if (next === Token.At) AtKind.Convert else AtKind.Exact
				val (expr, next) = parseExpr(ctx)
				val loc = locFrom(exprStart)
				return ExprRes(At(loc, kind, ty, expr), next)
			}
			else -> {
				addPart(ExprTy(ty))
				loopStart = start
				loopNext = next
				return null
			}
		}
	}

	while (true) {
		// Need to make it a val so smart casts will work
		val loopCurrentToken = loopNext
		when (loopCurrentToken) {
			Token.Backslash -> {
				val signature = parseSignatureThen(Token.Indent)
				val body = parseBlock()
				addPart(Lambda(locFrom(loopStart), signature, body))
				return ExprRes(finishRegular(), Next.EndNestedBlock)
			}

			Token.Cs, Token.Ts -> {
				must(ctx === Ctx.Line, locFrom(loopStart), Err.CsMustBeInLineContext)
				val expr = if (loopCurrentToken === Token.Cs) parseCs(loopStart) else parseTs(loopStart)
				addPart(expr)
				return ExprRes(finishRegular(), Next.EndNestedBlock)
			}

			Token.Colon -> {
				val (expr, next) = parseExpr(ctx)
				addPart(expr)
				return ExprRes(finishRegular(), next)
			}

			Token.Ck -> {
				must(!anySoFar() && ctx === Ctx.Line, locFrom(loopStart), Err.EqualsInExpression) // TODO: Err.CkMustBeAtStart
				val (cond, next) = parseExpr(Ctx.ExprOnly)
				return ExprRes(Check(locFrom(exprStart), cond), next)
			}

			Token.Equals -> {
				val loc = locFrom(loopStart)
				must(ctx === Ctx.Line, loc, Err.EqualsInExpression)
				val pattern = partsToPattern(loc, parts)
				val (expr, next) = parseExpr(Ctx.ExprOnly)
				must(next !== Next.CtxEnded, locFrom(loopStart), Err.BlockCantEndInDeclare)
				assert(next === Next.NewlineAfterStatement)
				return ExprRes(expr, Next.NewlineAfterEquals(pattern))
			}

			Token.Dot -> {
				val name = parseName()
				val loc = locFrom(loopStart)
				must(anySoFar(), loc, Err.EqualsInExpression) // TODO: Err.BeforeDot
				val prev = parts.pop()
				addPart(GetProperty(loc, prev, name))
				readAndLoop()
			}

			Token.DotDot ->
				return dotDot(finishRegular())

			is Token.TyName -> {
				//TODO:NEATER
				val a = afterType(parseTyInlineWithStart(loopStart, loopCurrentToken))
				if (a != null)
					return a
			}

			Token.Lbracket -> {
				val (start2, next2) = posNext()
				fun handleNamed(name: Sym) {
					val tys = parseGenInst()
					val loc = locFrom(loopStart)
					addPart(GenInst(loc, name, tys))
					readAndLoop()
				}
				when (next2) {
					is Token.Name -> handleNamed(next2.name)
					is Token.Operator -> handleNamed(next2.name)
					else -> {
						val genTy = parseTyInlineWithStart(start2, next2)
						val tys = parseGenInst()
						val ty = Ty.Inst(locFrom(loopStart), genTy, tys)
						val a = afterType(ty)
						if (a != null)
							return a
					}
				}
			}

			Token.LCurly -> {
				val (lst, next) = parseExpr(Ctx.List)
				assert(next === Next.CtxEnded)
				addPart(lst)
				readAndLoop()
			}

			is Token.Operator -> {
				val op = Access(locFrom(loopStart), loopCurrentToken.name)
				return if (anySoFar()) {
					val left = finishRegular()
					val (right, next) = parseExpr(ctx)
					return ExprRes(Call(locFrom(exprStart), op, Arr.of(left, right)), next)
				} else {
					val (rightStart, next) = posNext()
					when (ctx) {
						Ctx.ExprOnly ->
							when (next) {
								Token.DotDot -> return dotDot(op)
								Token.Newline, Token.Indent, Token.Dedent -> TODO("operator ExprOnly")
								else -> unexpected(rightStart, next)
							}
						Ctx.List -> TODO("Operator in list")
						Ctx.Paren ->
							when (next) {
								// `(+)` refers to the function itself
								Token.Rparen -> ExprRes(op, Next.CtxEnded)
								Token.DotDot -> dotDot(op)
								else -> unexpected(rightStart, next)
							}
						else -> notExpected()
					}
				}
			}

			Token.Lparen -> {
				val (a, next) = parseExpr(Ctx.Paren)
				addPart(a)
				assert(next === Next.CtxEnded)
				readAndLoop()
			}

			Token.Rparen -> {
				if (ctx !== Ctx.Paren)
					notExpected<Unit>()
				return ExprRes(finishRegular(), Next.CtxEnded)
			}

			Token.Newline, Token.Dedent -> {
				val next =
					when (ctx) {
						Ctx.Line, Ctx.ExprOnly ->
							when (loopCurrentToken) {
								Token.Newline -> Next.NewlineAfterStatement
								Token.Dedent -> Next.CtxEnded
								else -> TODO() //unreachable
							}
						else -> notExpected()
					}
				return ExprRes(finishRegular(), next)
			}

			Token.Indent ->
				when (ctx) {
					Ctx.CsHead -> ExprRes(finishRegular(), Next.CtxEnded)
					Ctx.Line -> {
						val expr = parseBlock()
						addPart(expr)
						ExprRes(finishRegular(), Next.EndNestedBlock)
					}
					else -> notExpected()
				}

			is Token.QuoteStart -> {
				addPart(parseQuote(loopStart, loopCurrentToken.head))
			}

			Token.RCurly -> {
				val expr =
					when (ctx) {
						Ctx.List -> List(finishLoc(), Arr.from(parts))
						Ctx.Quote -> finishRegular()
						else -> notExpected()
					}
				ExprRes(expr, Next.CtxEnded)
			}

			else -> {
				val loc = locFrom(loopStart)
				val e =
					when (loopCurrentToken) {
						is Token.Name -> Access(loc, loopCurrentToken.name)
						is Token.Literal -> Literal(loc, loopCurrentToken.value)
						else -> notExpected()
					}
				addPart(e)
				readAndLoop()
			}
		}
	}
}

private fun Lexer.parseQuote(start: Pos, head: String): Expr {
	val parts =
		buildLoop {
			val (interpolated, next) = parseExpr(Ctx.Quote)
			assert(next === Next.CtxEnded)
			val (s, isDone) = nextQuotePart()
			Pair(Quote.Part(interpolated, s), !isDone)
		}
	return Quote(locFrom(start), head, parts)
}


private fun Lexer.parseExpr(ctx: Ctx): ExprRes {
	val (start, next) = posNext()
	return parseExprWithNext(start, next, ctx)
}

private fun Lexer.parseCs(start: Pos): Expr {
	val (cased, next) = parseExpr(Ctx.CsHead)
	assert(next === Next.CtxEnded)
	val parts = buildUntilNull { parseCsPart() }
	return Cs(locFrom(start), cased, parts)
}

private fun Lexer.parseCsPart(): Cs.Part? {
	val (start, next) = posNext()
	return when (next) {
		Token.Dedent -> null
		else -> {
			val (ty, ignored, next) = parseTyFreeWithStart(start, next)
			val pattern = parseCsPattern(start, next)
			val test = Cs.Test(locFrom(start), ty, pattern)
			val result = parseBlock()
			Cs.Part(locFrom(start), test, result)
		}
	}
}

private fun Lexer.parseCsPattern(start: Pos, next: Token): Pattern =
	when (next) {
		Token.AtAt -> {
			val declare = parseLocalDeclare()
			returning(Pattern.Single(declare)) {
				mustSkip(Token.Indent)
			}
		}
	//TODO: Lparen to parse nested destructures
		is Token.Name -> {
			val patterns =
				buildUntilNullWithFirst(Pattern.Single(LocalDeclare(locFrom(start), next.name))) {
					val (start, next) = posNext()
					when (next) {
						is Token.Name ->
							Pattern.Single(LocalDeclare(locFrom(start), next.name))
						Token.Indent ->
							null
						else ->
							unexpected(start, next)
					}
				}
			Pattern.Destruct(locFrom(start), patterns)
		}
		Token.Indent -> Pattern.Ignore(locFrom(start))
		else -> unexpected(start, next)
	}

private fun Lexer.parseTs(start: Pos): Expr {
	mustSkip(Token.Indent)
	//TODO: share code with parseCs
	val (parts, elze) =
		buildAndReturn<Ts.Part, Expr> { add ->
			//TODO:NEATER
			var res: Expr
			loop@while (true) {
				val (start, next) = posNext()
				when (next) {
					Token.Underscore -> {
						mustSkip(Token.Indent)
						res = returning(parseBlock()) {
							mustSkip(Token.Dedent)
						}
						break@loop
					}
					else -> {
						//TODO: rename CsHead then!
						val (test, next) = parseExprWithNext(start, next, Ctx.CsHead)
						assert(next === Next.CtxEnded)
						val result = parseBlock()
						add(Ts.Part(locFrom(start), test, result))
					}
				}
			}
			res
		}
	return Ts(locFrom(start), parts, elze)
}

private fun Lexer.parseBlockWithStart(start: Pos, first: Token): Expr {
	val (expr, next) = parseExprWithNext(start, first, Ctx.Line)
	return when (next) {
		is Next.NewlineAfterEquals -> {
			val pattern = next.pattern
			val rest = parseBlock()
			val loc = locFrom(start)
			Let(loc, pattern, expr, rest)
		}
		Next.NewlineAfterStatement -> {
			val rest = parseBlock()
			val loc = locFrom(start)
			Seq(loc, expr, rest)
		}
		Next.EndNestedBlock -> {
			val (start, first) = posNext()
			when (first) {
				Token.Dedent -> expr
				else -> {
					val rest = parseBlockWithStart(start, first)
					//TODO: duplicate of above
					val loc = locFrom(start)
					Seq(loc, expr, rest)
				}
			}
		}
		Next.CtxEnded ->
			expr
	}
}

internal fun Lexer.parseBlock(): Expr {
	val (start, next) = posNext()
	return parseBlockWithStart(start, next)
}
