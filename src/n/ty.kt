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

sealed class Prim : Ty() {
	object Bool : Prim()
	object Float : Prim()
	object Int : Prim()
	object String : Prim()
	object Void : Prim()
	/** Dummy value. There is no 'nil' type. */
	object Nil : Prim()
}


/**
Instance of a GenPrim.
Do not directly construct this. Instead use instantiate_gen_prim.
*/
class PrimInst(val gen: GenPrim, val instWith: Arr<Ty>) : Ty()

class Rt(val origin: Origin, val properties: Arr<Property>) : Ty() {
	sealed class Origin {
		//TODO: Rt, Vt, Ft have these two origins in common.
		class Builtin(val name: Sym) : Origin()
		class Decl(val origin: CodeOrigin) : Origin()
		class GenInst(val gen: GenRt, val tys: Arr<Ty>) : Origin()
	}

	class Property(val name: Sym, val ty: Ty)
}

class Vt(val origin: Origin) : Ty() {
	var tys: Arr<Ty> by Late()

	sealed class Origin {
		class Builtin(val name: Sym)
		class Decl(val origin: CodeOrigin)
		class GenInst(val gen: GenVt, val tys: Arr<Ty>) : Origin()
	}
}

class Ft : Ty() {
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
		class GenInst(val gen: GenFt, val tys: Arr<Ty>) : Origin()
	}

	class Parameter(val name: Sym, val ty: Ty)
	data class Signature(val returnTy: Ty, val parameters: Arr<Parameter>)
}

// A parameter of a Gen.
sealed class GenVar : Ty() {
	class Builtin(val name: Sym) : GenVar()
	class Declared(val origin: CodeOrigin) : GenVar()
}

sealed class Gen() : TyOrGen()

/** Generic primitive. */
sealed class GenPrim(val stuff: GenStuff<PrimInst>) : Gen() {
	object List : GenPrim(GenStuff(Arr.of(GenVar.Builtin(Sym.ofString("A")))))

}

class GenRt(
	val origin: CodeOrigin,
	val stuff: GenStuff<Rt>) : Gen() {
	var properties: Arr<Rt.Property> by Late()
}

class GenVt(
	val origin: CodeOrigin,
	val stuff: GenStuff<Vt>) : Gen() {
	var tys: Arr<Ty> by Late()
}

class GenFt(val stuff: GenStuff<Ft>) : Gen() {
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

// Values associated with every generic. 'a is the concrete type.
class GenStuff<Concrete>(val params: Arr<GenVar>) {
	val cache = HashMap<Arr<Ty>, Concrete>()
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
