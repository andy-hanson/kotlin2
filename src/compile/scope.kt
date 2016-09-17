package compile

import u.*
import n.*
import java.util.*
import compile.*
import compile.err.*

private typealias ModuleTys = Lookup<Sym, TyOrGen>

internal class BaseScope private constructor(
	internal val moduleTys: ModuleTys,
	internal val moduleVals: Lookup<Sym, ModuleMember.MemberV>) {
	companion object {
		internal fun get(module: Module): BaseScope {
			val (moduleTys, moduleVals) = Lookup.build2<Sym, TyOrGen, Sym, ModuleMember.MemberV> { tryAddTy, tryAddVal ->
				fun addTy(loc: Loc, name: Sym, ty: TyOrGen) {
					if (tryAddTy(name, ty) != null)
						raise<Unit>(loc, Err.NameAlreadyBound(name))
				}
				fun addV(loc: Loc, name: Sym, v: ModuleMember.MemberV) {
					if (tryAddVal(name, v) != null)
						raise<Unit>(loc, Err.NameAlreadyBound(name))
				}

				for ((name, import) in module.imports)
					when (import.content) {
						is ModuleMember.Ty ->
							addTy(import.loc, name, import.content.ty)
						is ModuleMember.MemberV ->
							addV(import.loc, name, import.content)
					}
			}
			return BaseScope(moduleTys, moduleVals)
		}
	}

	fun getTyGivenTyParams(tyParams: Arr<GenVar>, ast: ast.Ty): Ty =
		getOnlyTy(moduleTys, createLocalTys(tyParams), ast)
}

private class Closure private constructor(val closureVals: HashMap<Sym, LocalDeclare>) {
	constructor() : this(HashMap())
	constructor(parameters: Arr<LocalDeclare>) : this(Hm.buildFromValues(parameters) { it.name })

	// For the outermost function, this will never be written to.
	val closureHiddenParameters = mutableListOf<LocalDeclare>()
}

private fun getVFromClosure(name: Sym, closures: List<Closure>): LocalDeclare? {
	val found = closures.findMapFromLast { it.closureVals[name] }
	if (found == null)
		return null
	val (indexOfClosure, local) = found
	for (innerClosureIndex in indexOfClosure..closures.size) {
		/*
		Since this was found from an outer closure, we need to add it to this closure's hidden parameters.
		Also add it to closure_vals to make sure we don't add it to the hidden parameters twice.Arg
		*/
		val closure = closures[innerClosureIndex]
		closure.closureVals[name] = local
		closure.closureHiddenParameters.add(local)
	}
	return local
}



//TODO:MOVE TO UTILS
private fun<T, U> List<T>.findMapFromLast(get: (T) -> U?): Pair<Int, U>? {
	//for ((index, element) in this.asReversed().withIndex()) {
	for (index in (this.size - 1) downTo 0) {
		val got = get(this[index])
		if (got != null)
			return Pair(index, got)
	}
	return null
}


private typealias LocalTys = Lookup<Sym, GenVar>
fun createLocalTys(genParams: Arr<GenVar>): LocalTys =
	Lookup.buildWithSize(genParams.size) { tryAdd ->
		for (param in genParams) {
			//TODO:NEATER (we can do this because we never type-check builtins...)
			val origin = (param as GenVar.Declared).origin
			if (tryAdd(origin.name, param) != null)
				raise<Unit>(origin.loc, Err.NameAlreadyBound(origin.name))
		}
	}

internal class Scope private constructor(
	private val base: BaseScope,
	// Generic tys declared by the outermost function. (Currently closures cannot be generic.)
	private val localTys: LocalTys) {

	companion object {
		fun createForFn(base: BaseScope, fn: Fn.Declared): Scope {
			val localTys =
				(fn.ty as? FtOrGen.G) opMap { createLocalTys(it.genFt.stuff.params) } ?: Lookup.empty()
			return Scope(base, localTys).apply {
				for (param in fn.parameters)
					addLocal(param)
			}
		}
	}

	// Used like a stack.
	//TODO: this is non-empty, so use a better representation
	private val closures = mutableListOf<Closure>(Closure())

	internal fun getV(loc: Loc, name: Sym): Binding =
		getVFromClosure(name, closures) opMap { Binding.Local(it) } ?:
			Binding.Global(base.moduleVals[name] ?:
				(Builtin.allMembers[name] ?: raise(loc, Err.CantBind(name))) as ModuleMember.MemberV)

	private val moduleTys: ModuleTys
		get() = base.moduleTys

	internal fun tryGetGen(tyAst: ast.Ty): Gen? =
		getTyOrGen(moduleTys, localTys, tyAst) as? Gen

	internal fun getTy(tyAst: ast.Ty): Ty =
		getOnlyTy(moduleTys, localTys, tyAst)

	internal fun instantiateGeneric(loc: Loc, gen: Gen, tyArgs: Arr<ast.Ty>): Ty =
		instantiateGenericImpl(moduleTys, localTys, loc, gen, tyArgs)

	private fun addLocal(local: LocalDeclare) {
		val (loc, name) = local
		val closure = closures.last()
		closure.closureVals.addOr(name, local) {
			raise(loc, Err.NameAlreadyBound(name))
		}
	}

	private fun removeLocal(local: LocalDeclare) {
		closures.last().closureVals.surelyRemove(local.name)
	}

	internal fun<T> addPattern(pattern: Pattern, f: Thunk<T>): T {
		fun recursivelyDo(pattern: Pattern, f: Action<LocalDeclare>) {
			when (pattern) {
				is Pattern.Ignore -> {}
				is Pattern.Single -> f(pattern.declare)
				is Pattern.Destruct ->
					for (sub in pattern.destructedInto)
						recursivelyDo(sub, f)
			}
		}
		recursivelyDo(pattern, this::addLocal)
		val res = f()
		recursivelyDo(pattern, this::removeLocal)
		return res
	}

	internal data class ClosureResult<T>(val result: T, val closureHiddenParameters: Arr<LocalDeclare>)
	internal fun<T> inClosure(parameters: Arr<LocalDeclare>, f: Thunk<T>): ClosureResult<T> {
		closures.add(Closure(parameters))
		val res = f()
		val lastClosure = closures.pop()
		return ClosureResult(res, Arr.from(lastClosure.closureHiddenParameters))
	}

	internal fun getPyImpl(loc: Loc, py: Py, tys: Arr<Ty>): Il {
		//TODO: module-level il
		val ils = Builtin.allIls[py] ?: TODO("error: no implementations available")
		return ils[tys] ?: raise(loc, Err.NoIlInScope(py, tys, ils.keys()))
	}
}

internal sealed class Binding {
	// Builtin, import, or another declaration in the same file.
	class Global(val member: ModuleMember.MemberV) : Binding()
	class Local(val declare: LocalDeclare) : Binding()
}

//TODO:MOVE
internal object Builtin {
	val allMembers: Lookup<Sym, ModuleMember> = TODO()
	val allIls: Lookup<Py, Lookup<Arr<Ty>, Il>>
}

private fun getTyOrGen(moduleTys: ModuleTys, localTys: LocalTys, ast: ast.Ty): TyOrGen =
	when (ast) {
		is ast.Ty.Access -> {
			val (loc, name) = ast
			localTys[name] ?: moduleTys[name] ?: run {
				//TODO: duplicate code in getV
				val builtin = Builtin.allMembers[name] ?: raise(loc, Err.CantBind(name))
				(builtin as ModuleMember.Ty).ty
			}
		}
		is ast.Ty.Inst -> {
			val (loc, ty, tyArgs) = ast
			instantiateGenericImpl(moduleTys, localTys, loc, getOnlyGen(moduleTys, localTys, ty), tyArgs)
		}
	}

//TODO:RENAME
private fun getOnlyTy(moduleTys: ModuleTys, localTys: LocalTys, tyAst: ast.Ty): Ty {
	val ty = getTyOrGen(moduleTys, localTys, tyAst)
	return ty as? Ty ?: TODO("fail with some message")
}

private fun getOnlyGen(moduleTys: ModuleTys, localTys: LocalTys, tyAst: ast.Ty): Gen {
	val ty = getTyOrGen(moduleTys, localTys, tyAst)
	return ty as? Gen ?: TODO("fail with some message")
}

private fun instantiateGenericImpl(moduleTys: ModuleTys, localTys: LocalTys, loc: Loc, gen: Gen, tyArgs: Arr<ast.Ty>) =
	instantiateGeneric(loc, gen, tyArgs.map { getOnlyTy(moduleTys, localTys, it) })
