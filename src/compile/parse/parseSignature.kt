package compile.parse

import u.*
import ast.*
import compile.lex.*

internal fun Lexer.parseSignatureThen(expectFollowedBy: Token): Signature {
	val start = curPos()
	val (returnTy, nextStart, nextToken) = parseTyFree()
	val (params, next) =
		//TODO:buildloop2 helper?
		buildAndReturn<Parameter, Token> { add ->
			loop2<Pos, Token, Token>(nextStart, nextToken) { start, token ->
				when (token) {
					Token.Indent, Token.Newline, Token.Dedent -> {
						Loop2.done(token)
					}
					is Token.Name -> {
						val (ty, nextStart, next) = parseTyFree()
						add(Parameter(locFrom(start), token.name, ty))
						Loop2.cont(nextStart, next)
					}
					else ->
						unexpected(start, token)
				}
			}
		}
	expect(start, expectFollowedBy, next)
	return Signature(locFrom(start), returnTy, params)
}
