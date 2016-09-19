package n

import u.*
import java.util.HashMap

/**
Origin for something that was explicitly written down
(as opposed to builtin or created from generic application)
*/
class CodeOrigin(val module: Module, val loc: Loc, val name: Sym)

sealed class TyOrGen

/** A concrete type. */
sealed class Ty : TyOrGen()

sealed class Prim(nameStr: String) : Ty() {
	val name: Sym = nameStr.sym

	object Bool : Prim("Bool")
	object Float : Prim("Float")
	object Int : Prim("Int")
	object Str : Prim("String")
	object Void : Prim("Void")
	/** Dummy value. There is no 'nil' type. */
	object Nil : Prim("Nil")
}


interface GenInstOrigin {
	val gen: Gen<*>
	val instWith: Arr<Ty>
}

/**
Instance of a GenPrim.
Do not directly construct this. Instead use instantiate_gen_prim.
*/
class PrimInst(override val gen: GenPrim, override val instWith: Arr<Ty>) : Ty(), GenInstOrigin

class Rt(val origin: Origin) : Ty() {
	var properties: Arr<Property> by Late()

	sealed class Origin {
		//TODO: Rt, Vt, Ft have these two origins in common.
		class Builtin(val name: Sym) : Origin()
		class Decl(val origin: CodeOrigin) : Origin()
		data class GenInst(override val gen: GenRt, override val instWith: Arr<Ty>) : Origin(), GenInstOrigin
	}

	class Property(val name: Sym, val ty: Ty)
}

class Vt(val origin: Origin) : Ty() {
	var variants: Arr<Ty> by Late()

	sealed class Origin {
		class Builtin(val name: Sym) : Origin()
		class Decl(val origin: CodeOrigin) : Origin()
		class GenInst(override val gen: GenVt, override val instWith: Arr<Ty>) : Origin(), GenInstOrigin
	}
}

class Ft() : Ty() {
	constructor(origin: Origin) : this() {
		this.origin = origin
	}
	constructor(origin: Origin, signature: Signature) : this(origin) {
		this.signature = signature
	}

	var origin: Origin by Late()
	var signature: Signature by Late()

	sealed class Origin {
		class Builtin(val name: Sym) : Origin()
		class Decl(val origin: CodeOrigin) : Origin()
		class Lambda(val loc: Loc) : Origin() //TODO: what to store here?
		class FromDeclaredFn(val fn: Fn.Declared) : Origin()
		class FromBuiltinFn(val fn: Fn.Builtin) : Origin()
		class FromRt(val rt: Rt) : Origin()
		class FromPartial(val partiallyApplied: Ft) : Origin()
		class GenInst(override val gen: GenFt, override val instWith: Arr<Ty>) : Origin(), GenInstOrigin
	}

	class Parameter(val name: Sym, val ty: Ty)
	data class Signature(val returnTy: Ty, val parameters: Arr<Parameter>)
}

// A parameter of a Gen.
sealed class GenVar : Ty() {
	class Builtin(val name: Sym) : GenVar()
	class Declared(val origin: CodeOrigin) : GenVar()
}

sealed class Gen<Concrete>(val tyParams: Arr<GenVar>) : TyOrGen() {
	val cache = HashMap<Arr<Ty>, Concrete>()
}

/** Generic primitive. */
sealed class GenPrim(params: Arr<GenVar>) : Gen<PrimInst>(params) {
	object List : GenPrim(Arr.of(GenVar.Builtin("Element".sym)))

}

class GenRt(val origin: CodeOrigin, params: Arr<GenVar>) : Gen<Rt>(params) {
	var properties: Arr<Rt.Property> by Late()
}

class GenVt(val origin: CodeOrigin, params: Arr<GenVar>) : Gen<Vt>(params) {
	var variants: Arr<Ty> by Late()
}

class GenFt(params: Arr<GenVar>) : Gen<Ft>(params) {
	constructor(params: Arr<GenVar>, origin: Origin) : this(params) {
		this.origin = origin
	}
	constructor(params: Arr<GenVar>, origin: Origin, signature: Ft.Signature): this(params, origin) {
		this.signature = signature
	}

	var origin: Origin by Late()
	var signature: Ft.Signature by Late()

	sealed class Origin {
		class FromPy(val py: Py) : Origin()
		class Builtin(val name: Sym) : Origin()
		class Declared(val origin: CodeOrigin) : Origin()
		class FromDeclaredFn(val fn: Fn.Declared) : Origin()
		class FromBuiltinFn(val fn: Fn.Builtin) : Origin()
		class FromGenRt(val genRt: GenRt) : Origin()
	}
}



//TODO: would like to use a sealed interface...
sealed class FtOrGen {
	abstract fun toTyOrGen(): TyOrGen

	class F(val ft: Ft) : FtOrGen() {
		override fun toTyOrGen() = ft
	}
	class G(val genFt: GenFt) : FtOrGen() {
		override fun toTyOrGen() = genFt
	}
}


//TODO: TyUU stuff
