package n

import u.*

data class Code(
	val bytecodes: Arr<Bc>,
	val locs: CodeLocs) {
}

sealed class Bc {
	/** Call the lambda at the top of the stack. */
	object Call : Bc()

	/** Perform some conversion. */
	class Cnv(val conversion: Conversion) : Bc()

	/** Array of jump targets, indexed by a Vv tag. */
	class Cs(val tagToCodeIndex: Arr<Int>) : Bc()

	/** Push some constant value onto the stack. */
	class Const(val value: V) : Bc()

	/** Pop a value and push its parts. */
	class Destruct(val patterns: Arr<Pattern>) : Bc()

	/** Pop a value and ignore it. */
	object Drop : Bc()

	/** Pop a value, check that it's an rc, and get its property with the given name. */
	class GetProperty(val name: Sym) : Bc()

	/** Load a value from `peekAmount` entries earlier in the stack. */
	class Load(val peekAmount: Int) : Bc()

	/** Goto the bytecode at the given index. */
	class Goto(val index: Int) : Bc()

	/** Pop a value, check that it's a bool, and goto the given index if that bool is false. */
	class GotoIfFalse(val index: Int) : Bc()

	/** Pop the return value, pop all parameters and ignore them, put the return value back, and go up a function. */
	object Return : Bc()

	/**
	For `a = b; c`, we push `b`, then eval `c` (which may fetch `a`); then remove `a` from under it.
	For `a = b; c = d; e` we set [nLets] to 2.
	*/
	class UnLet(val nLets: Int) : Bc()

	/** Pop n-1 arguments off the stack and interpolates them between the given strings. */
	class Partial(val nArgs: Int) : Bc()

	/** Pop `strings.size - 1` arguments off the stack and interpolates them between the given strings. */
	class Quote(val strings: Arr<String>) : Bc()

	/** Pop a bool and assert it. */
	object Check : Bc()

	/** Pop n values and push a list of them. */
	class ListLiteral(val nElements: Int) : Bc()

	/** Temporary value used during codegen. */
	object Nil : Bc()
}

// Location for each bytecode.
typealias CodeLocs = Arr<Loc>
