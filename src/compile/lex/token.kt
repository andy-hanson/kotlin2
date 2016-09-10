package compile.lex

import ast.LiteralValue
import u.*

sealed class Token {
	data class Name(val name: Sym) : Token()
	data class TyName(val name: Sym) : Token()
	data class Operator(val name: Sym) : Token()
	data class Literal(val value: LiteralValue) : Token()
	data class QuoteStart(val head: String) : Token()

	// Keywords
	object At : Token()
	object AtAt : Token()
	object Backslash : Token()
	object Equals : Token()
	object Dot : Token()
	object DotDot : Token()
	object Import : Token()
	object Underscore : Token()

	// Expressions
	object Cs : Token()
	object Ts : Token()
	object Colon : Token()
	object Ck : Token()

	// Declarations
	object Fn : Token()
	object Rt : Token()
	object Vt : Token()
	object Ft : Token()
	object Sn : Token()
	object Py : Token()
	object Il : Token()

	// Grouping
	object Indent : Token()
	object Dedent : Token()
	object Newline : Token()
	object Lparen : Token()
	object Rparen : Token()
	object Lbracket : Token()
	object Rbracket : Token()
	object LCurly : Token()
	object RCurly : Token()
	object EOF : Token()

	companion object {
		//TODO: reflection?
		val allKeywords: Arr<Token> = Arr.of(
			At, AtAt, Backslash, Equals, Dot, DotDot, Import, Underscore,
			Cs, Ts, Colon, Ck,
			Fn, Rt, Vt, Ft, Sn, Py, Il,
			Indent, Dedent, Newline, Lparen, Rparen, Lbracket, Rbracket, LCurly, RCurly, EOF
		)

		val allNameKeywords: Arr<Token> = Arr.of(
			At, AtAt, Equals, Cs, Ck, Import, Rt, Ts, Vt, Ft, Fn)

		fun opKeyword(name: Sym): Token? =
			TODO()
	}
}

/*
enum class Kw {
	At,
	AtAt,
	Backslash,
	Equals,
	Dot,
	DotDot,
	Import,
	Underscore,

	// Expressions
	Cs,
	Ts,
	Colon,
	Ck,

	// Declarations
	Fn,
	Rt,
	Vt,
	Ft,
	Sn,
	Py,
	Il,

	// Grouping
	Indent,
	Dedent,
	Newline,
	Lparen,
	Rparen,
	Lbracket,
	Rbracket,
	LCurly,
	RCurly,
	EOF
}
*/
