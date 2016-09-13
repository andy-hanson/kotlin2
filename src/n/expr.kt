package n

import u.*

sealed class Expr

/**
A local variable (parameter or let).
For a parameter the type was declared, for a local the type was inferred.
*/
data class LocalDeclare(val loc: Loc, val name: Sym, val ty: Ty) : Expr()

/** LHS of a let, or a pattern in a Cs. */
sealed class Pattern {
	/** A `_` pattern. */
	data class Ignore(val loc: Loc) : Pattern()
	// TODO: just have LocalDeclare : Pattern ?
	/** A simple variable pattern. */
	data class Single(val declare: LocalDeclare) : Pattern()
	/** A pattern with multiple sub-patterns. */
	data class Destruct(val loc: Loc, val destructedInto: Arr<Pattern>) : Pattern()
}

/** The test of a single CsPart. */
data class CsTest(val loc: Loc, val ty: Ty, val pattern: Pattern)

/** An implicit or explicit conversion. */
sealed class Conversion {
	data class Vv(
		/** Variant to put this value into. */
		val ty: Vt,
		/** Tag for this value's type in that variant. */
		val tag: Int) : Conversion()
	data class Rc(
		/** Type converted to. */
		val ty: Rt,
		/** Indices into the pre-converted record that will be retrieved to make the post-converted record. */
		val propertyIndices: Arr<Int>) : Conversion()
}

/** This node is inserted for a conversion. */
data class Convert(val conversion: Conversion, val converted: Expr) : Expr()

/** An Access expression for something not local becomes a Value. */
data class LocalAccess(
	val loc: Loc,
	/** Up-pointer to a previously declared local. */
	val declare: LocalDeclare) : Expr()

/** Call any function (whether builtin, declared_fn, or lambda). */
data class Call(val loc: Loc, val ft: Ft, val target: Expr, val arguments: Arr<Expr>) : Expr()

/** `cs` expression. */
data class Cs(val loc: Loc, val vt: Vt, val cased: Expr, val parts: Arr<Part>) : Expr() {
	data class Part(val loc: Loc, val test: Test, val expr: Expr)
	data class Test(val loc: Loc, val ty: Ty, val pattern: Pattern)
}

/** `ts` expression. */
data class Ts(val loc: Loc, val parts: Arr<Part>, val elze: Expr) : Expr() {
	data class Part(val loc: Loc, val test: Expr, val result: Expr)
}

/** `x.y` */
data class GetProperty(val loc: Loc, val record: Expr, val proeprtyName: Sym) : Expr()

/** `assigned = value \n then` */
data class Let(val loc: Loc, val assigned: Pattern, val value: Expr, val then: Expr) : Expr()

/** `action \n then` */
data class Seq(val loc: Loc, val action: Expr, val then: Expr) : Expr()

/** `target .. args */
data class Partial(val loc: Loc, val target: Expr, val args: Arr<Expr>): Expr()

/**
`"foo{bar}baz"`
There will always be 1 more string than interpolated value,
because we include head/tail strings even if they are empty.
*/
data class Quote(val loc: Loc, val head: String, val parts: Arr<Part>) : Expr() {
	data class Part(val interpolated: Expr, val text: String)
}

/** `ck expr` */
data class Check(val loc: Loc, val cond: Expr) : Expr()

/** `{expr expr expr}` */
data class EList(val loc: Loc, val elements: Arr<Expr>) : Expr()

/** `\x y ...` */
data class Lambda(val fn: Fn.Lambda) : Expr()
