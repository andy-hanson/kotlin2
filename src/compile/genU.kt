package compile

import u.*
import n.*

private fun fillInProperties(getTy: (ast.Ty) -> Ty, properties: Arr<ast.Property>): Arr<Rt.Property> =
	properties.map { prop ->
		Rt.Property(prop.name, getTy(prop.ty))
	}

private fun fillInParameters(getTy: (ast.Ty) -> Ty, parameters: Arr<ast.Parameter>): Arr<Ft.Parameter> =
	parameters.map { param ->
		Ft.Parameter(param.name, getTy(param.ty))
	}

//TODO: since signature didn't exist already, this is poorly named... same for fill_in_properties...
private fun fillInSignature(getTy: (ast.Ty) -> Ty, signature: ast.Signature): Ft.Signature =
	Ft.Signature(getTy(signature.returnTy), fillInParameters(getTy, signature.parameters))


internal fun nilOfAst(module: Module, ty: ast.DeclTy): TyOrGen =
	TODO()

internal fun instantiateGeneric(loc: Loc, gen: Gen, args: Arr<Ty>): Ty {
	TODO()
}

typealias GetTy = (Arr<GenVar>, ast.Ty) -> Ty

internal fun fillGeneric(getTy: GetTy, ast: ast.DeclTy, ty: TyOrGen) {
	TODO()
}

internal fun fillInFnSignature(getTy: GetTy, signatureAst: ast.Signature, fn: Fn.Declared) {
	TODO()
}

internal data class UnInstantiated(val expectedInstWith: Arr<Ty>, val actualInstWith: Arr<Ty>)
internal fun unInstantiateForCompare(a: Ty, b: Ty): UnInstantiated? {
	TODO()
}

internal fun instantiateGenRt(genRt: GenRt, tys: Arr<Ty>): Rt {
	TODO()
}

internal fun instantiateFn(genFn: Fn, tys: Arr<Ty>): Pair<Fn, Ft> {
	TODO()
}

internal fun instantiateGenFt(genFt: GenFt, tys: Arr<Ty>): Ft {
	TODO()
}

internal fun instantiateGenPrim(genPrim: GenPrim, tys: Arr<Ty>): PrimInst {
	TODO()
}
