package compile

import compile.err.*
import u.*
import n.*


private typealias GetTy = (ast.Ty) -> Ty

private fun fillInProperties(getTy: GetTy, properties: Arr<ast.Rt.Property>): Arr<Rt.Property> =
	properties.map { prop ->
		Rt.Property(prop.name, getTy(prop.ty))
	}

private fun fillInParameters(getTy: GetTy, parameters: Arr<ast.Parameter>): Arr<Ft.Parameter> =
	parameters.map { param ->
		Ft.Parameter(param.name, getTy(param.ty))
	}

//TODO: since signature didn't exist already, this is poorly named... same for fill_in_properties...
private fun fillInSignature(getTy: (ast.Ty) -> Ty, signature: ast.Signature): Ft.Signature =
	Ft.Signature(getTy(signature.returnTy), fillInParameters(getTy, signature.parameters))


//TODO:NAME
fun baz(getTy: GetTyFromParams): GetTy =
	getTy.partial(Arr.empty())


interface Instantiator<Ast, Ty> {
	fun nil(module: Module, ast: Ast): Ty
	fun fill(getTy: GetTyFromParams, ast: Ast, ty: Ty): Unit
}

private abstract class FillableGeneric<Generic : Gen<Concrete>, Concrete> {
	protected abstract fun originTys(inst: Concrete): Arr<Ty>
	abstract fun nilInstance(g: Generic, tys: Arr<Ty>): Concrete
	protected abstract fun fillInstance(g: Generic, inst: Concrete, substitute: (Ty) -> Ty): Unit

	fun instantiate(g: Generic, tys: Arr<Ty>): Concrete {
		val params = g.tyParams
		val cache = g.cache
		return cache[tys] ?: returning(nilInstance(g, tys)) { made ->
			cache[tys] = made
			val vars = Vars.ofKeysAndValues(params, tys)
			fillInstance(g, made, ::substitute.partial(vars))
		}
	}

	fun instantiateWithCheck(g: Generic, loc: Loc, tys: Arr<Ty>): Concrete {
		must(g.tyParams.sameSize(tys), loc, Err.GenInstParameters(g.tyParams.size, tys.size))
		return instantiate(g, tys)
	}

	//TODO:DOCUMENT
	protected fun fillThoseAlreadyInstantiated(g: Generic) {
		for (target in g.cache.values) {
			val tys = originTys(target)
			//TODO: substitute of vars.buildFromKeysAndValues is duplicate code
			fillInstance(g, target, ::substitute.partial(Vars.ofKeysAndValues(g.tyParams, tys)))
		}
	}
}

private abstract class SuperDuperGeneric<Ast, Generic : Gen<Concrete>, Concrete> : FillableGeneric<Generic, Concrete>(), Instantiator<Ast, Generic> {
	//TODO:NAME
	fun fillThis(getTy: GetTyFromParams, ast: Ast, g: Generic) {
		fill(getTy, ast, g)
		fillThoseAlreadyInstantiated(g)
	}
}


internal fun nilOfAst(module: Module, ty: ast.DeclTy): TyOrGen =
	when (ty) {
		is ast.Rt -> RtInstantiator.nil(module, ty)
		is ast.Vt -> VtInstantiator.nil(module, ty)
		is ast.Ft -> FtInstantiator.nil(module, ty)
		is ast.GenRt -> GenRtInstantiator.nil(module, ty)
		is ast.GenVt -> GenVtInstantiator.nil(module, ty)
		is ast.GenFt -> GenFtInstantiator.nil(module, ty)
	}

private typealias Vars = Lookup<GenVar, Ty>

private fun substitute(vars: Vars, ty: Ty): Ty {
	fun loop(ty: Ty): Ty? {
		fun<T> tryReplace(oldTyArguments: Arr<Ty>, replace: (Arr<Ty>) -> T): T? =
			oldTyArguments.mapIfAnyMaps(::loop) opMap replace
		return when (ty) {
			is Prim -> null
			is PrimInst ->
				TODO()
			is Rt ->
				ty.origin as? Rt.Origin.GenInst opMap {
					tryReplace(it.instWith, ::instantiateGenRt.partial(it.gen))
				}
			is Vt ->
				ty.origin as? Vt.Origin.GenInst opMap {
					tryReplace(it.instWith, ::instantiateGenVt.partial(it.gen))
				}
			is Ft ->
				ty.origin as? Ft.Origin.GenInst opMap {
					tryReplace(it.instWith, ::instantiateGenFt.partial(it.gen))
				}
			is GenVar ->
				vars.mustGet(ty)
		}
	}
	return loop(ty) ?: ty
}


internal fun fillGeneric(getTy: GetTyFromParams, ast: ast.DeclTy, ty: TyOrGen): Unit {
	when (ast) {
		is ast.Rt -> RtInstantiator.fill(getTy, ast, ty as Rt)
		is ast.Vt -> VtInstantiator.fill(getTy, ast, ty as Vt)
		is ast.Ft -> FtInstantiator.fill(getTy, ast, ty as Ft)
		is ast.GenRt -> GenRtInstantiator.fillThis(getTy, ast, ty as GenRt)
		is ast.GenVt -> GenVtInstantiator.fillThis(getTy, ast, ty as GenVt)
		is ast.GenFt -> GenFtInstantiator.fillThis(getTy, ast, ty as GenFt)
	}
}

internal fun instantiateGeneric(loc: Loc, gen: Gen<*>, args: Arr<Ty>): Ty =
	when (gen) {
		is GenPrim -> FillGenPrim.instantiateWithCheck(gen, loc, args)
		is GenRt -> GenRtInstantiator.instantiateWithCheck(gen, loc, args)
		is GenVt -> GenVtInstantiator.instantiateWithCheck(gen, loc, args)
		is GenFt -> GenFtInstantiator.instantiateWithCheck(gen, loc, args)
	}

typealias GetTyFromParams = (Arr<GenVar>, ast.Ty) -> Ty

internal fun fillInFnSignature(getTy: GetTyFromParams, signatureAst: ast.Signature, fn: Fn.Declared) {
	val signature =
		when (fn.ty) {
			is FtOrGen.F ->
				returning(fillInSignature(baz(getTy), signatureAst)) {
					fn.ty.ft.signature = it
				}
			is FtOrGen.G -> {
				val g = fn.ty.genFt
				returning(fillInSignature(getTy.partial(g.tyParams), signatureAst)) {
					g.signature = it
				}
			}
		}
	fn.parameters = signatureAst.parameters.zip(signature.parameters) { ast, param ->
		LocalDeclare(ast.loc, param.name, param.ty)
	}
}

internal data class UnInstantiated(val expectedInstWith: Arr<Ty>, val actualInstWith: Arr<Ty>)
//TODO: cut down on boilerplate here
internal fun unInstantiateForCompare(a: Ty, b: Ty): UnInstantiated? =
	when (a) {
		is PrimInst ->
			unInstantiateHelper(a, b) { it }
		is Rt ->
			unInstantiateHelper(a, b) { it.origin as? Rt.Origin.GenInst }
		is Vt ->
			unInstantiateHelper(a, b) { it.origin as? Vt.Origin.GenInst }
		is Ft ->
			unInstantiateHelper(a, b) { it.origin as? Ft.Origin.GenInst }
		else ->
			never()
	}

private inline fun<reified T> unInstantiateHelper(a: T, b: Ty, getOrigin: (T) -> GenInstOrigin?): UnInstantiated? =
	getOrigin(a) opMap { aOrigin ->
		b as? T opMap getOrigin opMap { bOrigin ->
			opIf(aOrigin == bOrigin) { UnInstantiated(aOrigin.instWith, bOrigin.instWith) }
		}
	}

internal fun instantiateFn(genFn: Fn, tys: Arr<Ty>): Fn.Instance {
	val genFt = (genFn.ty as FtOrGen.G).genFt
	return Fn.Instance(genFn, tys, instantiateGenFt(genFt, tys))
}

internal fun instantiateGenPrim(genPrim: GenPrim, tys: Arr<Ty>): PrimInst =
	FillGenPrim.instantiate(genPrim, tys)
internal fun instantiateGenRt(genRt: GenRt, tys: Arr<Ty>): Rt =
	GenRtInstantiator.instantiate(genRt, tys)
internal fun instantiateGenVt(genVt: GenVt, tys: Arr<Ty>): Vt =
	GenVtInstantiator.instantiate(genVt, tys)
internal fun instantiateGenFt(genFt: GenFt, tys: Arr<Ty>): Ft =
	GenFtInstantiator.instantiate(genFt, tys)


private object RtInstantiator : Instantiator<ast.Rt, Rt> {
	override fun nil(module: Module, ast: ast.Rt): Rt =
		Rt(Rt.Origin.Decl(CodeOrigin(module, ast.loc, ast.name)))

	override fun fill(getTy: GetTyFromParams, ast: ast.Rt, ty: Rt) {
		ty.properties = fillInProperties(baz(getTy), ast.properties)
	}
}

private object VtInstantiator : Instantiator<ast.Vt, Vt> {
	override fun nil(module: Module, ast: ast.Vt): Vt =
		Vt(Vt.Origin.Decl(CodeOrigin(module, ast.loc, ast.name)))

	override fun fill(getTy: GetTyFromParams, ast: ast.Vt, ty: Vt) {
		ty.variants = ast.variants.map(baz(getTy))
	}
}

private object FtInstantiator : Instantiator<ast.Ft, Ft> {
	override fun nil(module: Module, ast: ast.Ft): Ft =
		Ft(Ft.Origin.Decl(CodeOrigin(module, ast.loc, ast.name)))

	override fun fill(getTy: GetTyFromParams, ast: ast.Ft, ty: Ft) {
		ty.signature = fillInSignature(baz(getTy), ast.signature)
	}
}

private object FillGenPrim : FillableGeneric<GenPrim, PrimInst>() {
	override fun originTys(inst: PrimInst) = inst.instWith

	override fun nilInstance(g: GenPrim, tys: Arr<Ty>): PrimInst =
		PrimInst(g, tys)

	override fun fillInstance(g: GenPrim, inst: PrimInst, substitute: (Ty) -> Ty) {}
}

private object GenRtInstantiator : SuperDuperGeneric<ast.GenRt, GenRt, Rt>() {
	override fun originTys(inst: Rt) =
		(inst.origin as Rt.Origin.GenInst).instWith

	override fun nilInstance(g: GenRt, tys: Arr<Ty>) =
		Rt(Rt.Origin.GenInst(g, tys))

	override fun fillInstance(g: GenRt, inst: Rt, substitute: (Ty) -> Ty) {
		inst.properties = g.properties.map { prop ->
			Rt.Property(prop.name, substitute(prop.ty))
		}
	}

	override fun nil(module: Module, ast: ast.GenRt): GenRt =
		GenRt(CodeOrigin(module, ast.loc, ast.name), createGenStuffFromParamAsts(module, ast.params))

	override fun fill(getTy: GetTyFromParams, ast: ast.GenRt, ty: GenRt) {
		ty.properties = fillInProperties(getTy.partial(ty.tyParams), ast.properties)
	}
}

private object GenVtInstantiator : SuperDuperGeneric<ast.GenVt, GenVt, Vt>() {
	override fun originTys(inst: Vt) =
		(inst.origin as Vt.Origin.GenInst).instWith

	override fun nilInstance(g: GenVt, tys: Arr<Ty>): Vt =
		Vt(Vt.Origin.GenInst(g, tys))

	override fun fillInstance(g: GenVt, inst: Vt, substitute: (Ty) -> Ty) {
		inst.variants = g.variants.map(substitute)
	}

	//TODO: duplicate code in nilGenRt, use inheritance between asts
	override fun nil(module: Module, ast: ast.GenVt): GenVt =
		GenVt(CodeOrigin(module, ast.loc, ast.name), createGenStuffFromParamAsts(module, ast.params))

	override fun fill(getTy: GetTyFromParams, ast: ast.GenVt, ty: GenVt) {
		ty.variants = ast.variants.map(getTy.partial(ty.tyParams))
	}
}

private object GenFtInstantiator : SuperDuperGeneric<ast.GenFt, GenFt, Ft>() {
	override fun originTys(inst: Ft) =
		(inst.origin as Ft.Origin.GenInst).instWith

	override fun nilInstance(g: GenFt, tys: Arr<Ty>) =
		Ft(Ft.Origin.GenInst(g, tys))

	override fun fillInstance(g: GenFt, inst: Ft, substitute: (Ty) -> Ty) {
		inst.signature = Ft.Signature(
			substitute(g.signature.returnTy),
			g.signature.parameters.map { p -> Ft.Parameter(p.name, substitute(p.ty)) })
	}

	override fun nil(module: Module, ast: ast.GenFt): GenFt =
		GenFt(
			createGenStuffFromParamAsts(module, ast.params),
			GenFt.Origin.Declared(CodeOrigin(module, ast.loc, ast.name)))

	override fun fill(getTy: GetTyFromParams, ast: ast.GenFt, ty: GenFt) {
		ty.signature = fillInSignature(getTy.partial(ty.tyParams), ast.signature)
	}
}
