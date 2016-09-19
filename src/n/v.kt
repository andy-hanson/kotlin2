package n

import compile.partiallyApplyFt
import u.*

sealed class V {
	sealed class Prim : V() {
		class Bool(val value: Boolean) : V()
		class Float(val value: Double) : V()
		class Int(val value: Long) : V()
		class String(val value: kotlin.String) : V()
		object Void

		class List(val elements: Arr<V>) : V()
	}
}

/** This stores the rt for debugging purposes only. One day we should just store properties. */
class Rc(val ty: Rt, val properties: Arr<V>)
/** This stores the vt for debugging purposes only. */
class Vv(val ty: Vt, val tag: Int, val value: V)

sealed class Fn : V() {
	abstract val ty: FtOrGen

	/** A function declared as code in a module. */
	class Declared(val origin: CodeOrigin, override val ty: FtOrGen) : Fn() {
		var parameters: Arr<LocalDeclare> by Late()
		var body: Expr by Late()
		var code: Code by Late()
	}

	/**
	The backing function for a lambda expression.
	Actual lambda instances will be partial applications of this.
	*/
	class Lambda(
		val origin: Loc,
		val explicitParameters: Arr<LocalDeclare>,
		val closureParameters: Arr<LocalDeclare>,
		val body: Expr,
		val ft: Ft) : Fn()  {

		override val ty: FtOrGen
			get() = FtOrGen.F(ft)

		var code: Code by Late()
	}

	/** A builtin function. */
	class Builtin(val name: Sym, override val ty: FtOrGen, val exec: Exec) : Fn()

	/** Partial application of any other function. */
	class Partial(val partiallyApplied: Fn, val partialArgs: Arr<V>) : Fn() {
		override val ty: FtOrGen
			get() = FtOrGen.F(partiallyApplyFt((partiallyApplied.ty as FtOrGen.F).ft, partialArgs.size))
	}

	class Ctr(val rt: Rt) : Fn() {
		//TODO: store the ty here upon creation. And create the Ctr upon creating the rt.
		override val ty: FtOrGen
			get() = TODO()
	}

	class Instance(val instantiated: Fn, val tyArgs: Arr<Ty>, val ft: Ft) : Fn() {
		override val ty: FtOrGen
			get() = FtOrGen.F(ft)
	}
}

//TODO:MOVE
typealias Exec = Int /* TODO: Action<StackState> */
