package compile.lex

import ast.LiteralValue
import u.*

sealed class Token {
	class Name(val name: Sym) : Token() {
		override fun toString() = "Name($name)"
	}
	class TyName(val name: Sym) : Token() {
		override fun toString() = "TyName($name)"
	}
	class Operator(val name: Sym) : Token() {
		override fun toString() = "Operator($name)"
	}
	class Literal(val value: LiteralValue) : Token() {
		override fun toString() = value.toString()
	}
	class QuoteStart(val head: String) : Token() {
		override fun toString() = "QuoteStart(\"$head\"')"
	}

	abstract class Kw(name: String) : Token() {
		val name = name.sym

		override fun toString() =
			name.toString()
	}

	// Keyword that is *not* a possible identifier name. We want toString() to look nice.
	abstract class PlainKw(val name: String) : Token() {
		override fun toString() =
			name
	}

	// Keywords
	object At : Kw("@")
	object AtAt : Kw("@@")
	object Backslash : Kw("\\")
	object Equals : Kw("=")
	object Dot : PlainKw(".")
	object DotDot : PlainKw("..")
	object Import : Kw("import")
	object Underscore : Kw("_")

	// Expressions
	object Cs : Kw("cs")
	object Ts : Kw("ts")
	object Colon : PlainKw(":")
	object Ck : Kw("ck")

	// Declarations
	object Fn : Kw("fn")
	object Rt : Kw("rt")
	object Vt : Kw("vt")
	object Ft : Kw("ft")
	object Sn : Kw("sn")
	object Py : Kw("py")
	object Il : Kw("il")

	// Grouping
	object Indent : PlainKw("->")
	object Dedent : PlainKw("<-")
	object Newline : PlainKw("\\n")
	object Lparen : PlainKw("(")
	object Rparen : PlainKw(")")
	object Lbracket : PlainKw("[")
	object Rbracket : PlainKw("]")
	object LCurly : PlainKw("{")
	object RCurly : PlainKw("}")
	object EOF : PlainKw("EOF")

	companion object {
		//TODO: reflection?
		/*val allKeywords: Arr<Token> = Arr.of(
			At, AtAt, Backslash, Equals, Dot, DotDot, Import, Underscore,
			Cs, Ts, Colon, Ck,
			Fn, Rt, Vt, Ft, Sn, Py, Il,
			Indent, Dedent, Newline, Lparen, Rparen, Lbracket, Rbracket, LCurly, RCurly, EOF
		)*/

		val allNameKeywords: Arr<Kw> = Arr.of(
			At, AtAt, Equals, Cs, Ck, Import, Rt, Ts, Vt, Ft, Fn)

		private val nameToKw = Lookup.fromValues(allNameKeywords) { kw ->  kw.name  }
		fun opKeyword(name: Sym): Kw? =
			nameToKw[name]
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
