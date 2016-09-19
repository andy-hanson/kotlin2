package compile.typeCheck

import compile.*
import compile.err.*
import u.*
import n.*

//TODO: This *must* be cached! Same for ft_of_rt_ctr!
private fun genFtOfGenRtCtr(genRt: GenRt): GenFt {
	val tyParams = genRt.tyParams
	val params = genRt.properties.map { Ft.Parameter(it.name, it.ty) }
	val signature = Ft.Signature(instantiateGenRt(genRt, tyParams), params)
	return GenFt(tyParams, GenFt.Origin.FromGenRt(genRt), signature)
}

private sealed class CallKind {
	class CallGeneric(val fn: Fn, val ft: GenFt) : CallKind()
	class CallGenericRt(val rt: GenRt) : CallKind()
	class CallPy(val py: Py) : CallKind()
	object CallNormal : CallKind()
}

//TODO:NEATER
private fun inferGenericCall(scope: Scope, loc: Loc, expected: Expected, called: Fn, calledTy: GenFt, argAsts: Arr<ast.Expr>): Expr {
	val (genericArguments, args) = inferGenericCallWorker(scope, loc, expected, calledTy, argAsts)
	val fn = instantiateFn(called, genericArguments)
	val expr = Call(loc, fn.ft, Value(loc, fn), args)
	return checkAny(loc, expected, fn.ft.signature.returnTy, expr)
}

// Remember, genReturnTy may contain un-inferrable variables.
private fun inferGenericsFromExpected(genReturnTy: Ty, inferring: Arr<GenVarInferring>, expected: Expected) {
	//TODO
}

//TODO:REFACTOR
//Returns, inferred generic arguments, and argument expressions
private fun inferGenericCallWorker(scope: Scope, loc: Loc, expected: Expected, calledTy: GenFt, argAsts: Arr<ast.Expr>): Pair<Arr<Ty>, Arr<Expr>> {
	val inferring = calledTy.tyParams.map { GenVarInferring(it) }
	inferGenericsFromExpected(calledTy.signature.returnTy, inferring, expected)
	val args = calledTy.signature.parameters.zip(argAsts) { param, argAst ->
		checkWorker(scope, Expected.InferGeneric(inferring, param.ty), argAst)
	}
	val genericArguments = inferring.map {
		it.getKnown() ?: TODO("error message: not inferrable")
	}
	return Pair(genericArguments, args)
}

//TODO:RENAME
private fun getCallKind(scope: Scope, ast: ast.Expr): CallKind =
	when (ast) {
		is ast.ExprTy -> {
			val gen = scope.tryGetGen(ast.ty)
			when (gen) {
				is GenRt -> CallKind.CallGenericRt(gen)
				else -> CallKind.CallNormal
			}
		}
		is ast.Access -> {
			val got = scope.getV(ast.loc, ast.name)
			when (got) {
				is Binding.Global -> {
					val memberV = got.member
					when (memberV) {
						is MemberPy ->
							CallKind.CallPy(memberV.py)
						is TypedV -> {
							val (tyOrGen, value) = memberV
							when (tyOrGen) {
								is GenFt ->
									CallKind.CallGeneric(value as Fn, tyOrGen)
								else -> CallKind.CallNormal
							}
						}
					}
				}
				is Binding.Local ->
					CallKind.CallNormal
			}
		}
		else ->
			CallKind.CallNormal
	}

internal fun checkCall(scope: Scope, expected: Expected, loc: Loc, calledAst: ast.Expr, argAsts: Arr<ast.Expr>): Expr {
	fun checkNParameters(parameters: Arr<Ft.Parameter>) {
		must(parameters.sameSize(argAsts), loc, Err.NumArgs(parameters.size, argAsts.size))
	}
	val kind = getCallKind(scope, calledAst)
	//TODO: reduce repetition between cases
	return when (kind) {
		is CallKind.CallGeneric -> {
			checkNParameters(kind.ft.signature.parameters)
			inferGenericCall(scope, loc, expected, kind.fn, kind.ft, argAsts)
		}
		is CallKind.CallGenericRt -> {
			val genFt = genFtOfGenRtCtr(kind.rt)
			checkNParameters(genFt.signature.parameters)
			val (genericArguments, args) = inferGenericCallWorker(scope, loc, expected, genFt, argAsts)
			val rt = instantiateGenRt(kind.rt, genericArguments)
			val called = Fn.Ctr(rt)
			val ft = ftOfRtCtr(rt)
			val expr = Call(loc, ft, Value(loc, called), args)
			checkAny(loc, expected, ft.signature.returnTy, expr)
		}
		is CallKind.CallPy -> {
			val py = kind.py
			checkNParameters(py.ty.signature.parameters)
			val (genericArguemnts, args) = inferGenericCallWorker(scope, loc, expected, py.ty, argAsts)
			val called = scope.getPyImpl(loc, py, genericArguemnts)
			val ft = called.ft()
			val expr = Call(loc, ft, Value(loc, called.fn), args)
			checkAny(loc, expected, ft.signature.returnTy, expr)
		}
		is CallKind.CallNormal -> {
			val (calledTy, called) = checkAndInfer(scope, calledAst)
			val calledFt = calledTy as? Ft ?: raise(loc, Err.NotAFunction(calledTy))
			val signature = calledFt.signature
			val parameters = signature.parameters
			checkNParameters(parameters)
			//TODO: use parameter name for helpful error info
			val args = parameters.zip(argAsts) { param, arg -> checkParameter(scope, param, arg) }
			val call = Call(loc, calledFt, called, args)
			checkAny(loc, expected, signature.returnTy, call)
		}
	}
}

