package compile.typeCheck

import u.*
import n.*
import compile.err.*

/**
A gen_var that may or may not have been inferred yet.
(If it has been inferred, `known` will have been written to.
 */
internal class GenVarInferring(val genVar: GenVar) {
	//var known: Ty by Late()
	private var known: Ty? = null

	fun getKnown(): Ty? =
		known

	fun setKnown(ty: Ty) {
		require(known == null)
		known = ty
	}
}

internal typealias Inferring = Arr<GenVarInferring>

/** Represents the contextual typing of an expression. */
internal sealed class Expected {
	/** Value must be exactly of the given type or, if ty is a union, a member of that union. */
	class ImplicitCast(val ty: Ty) : Expected()
	/** Expression is in `A @ e`, so it must be a value convertible to A. */
	class ExplicitCast(val ty: Ty) : Expected()
	/** Nothing particular is expected. */
	class Infer : Expected() {
		/** Holds the inferred type. */
		private var ty: Ty? = null

		fun get(): Ty =
			ty!!

		fun set(loc: Loc, inferredTy: Ty) {
			val t = ty
			if (t == null)
				ty = inferredTy
			else
				// In a cs or ts, we pass the same Infer to multiple branches.
				// Every branch must have the same type.
				must(t == inferredTy, loc, Err.CombineTypes(t, inferredTy))
		}
	}
	/**
	The expression is part of a generic call.
	If [ty] contains any [GenVar]s, they should be looked up in [vars].
	 */
	class InferGeneric(val vars: Arr<GenVarInferring>, val ty: Ty) : Expected()
}
