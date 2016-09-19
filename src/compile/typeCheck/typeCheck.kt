package compile.typeCheck

import compile.*
import compile.err.*
import u.*
import n.*

/**
Returns the 'type' of the type when used as a value.
For example, an rt may be used as a function.
*/
private fun checkTyAsExpr(scope: Scope, expected: Expected, tyAst: ast.Ty): Expr {
	val ty = scope.getTy(tyAst)
	val loc = tyAst.loc
	val rt = ty as? Rt ?: TODO("error message")
	val ft = ftOfRtCtr(rt)
	val value = Value(loc, Fn.Ctr(rt))
	return checkAny(loc, expected, ft, value)
}

//TODO:MOVE TO UTIL
fun checkPattern(ty: Ty, ast: ast.Pattern): Pattern =
	when (ast) {
		is ast.Pattern.Ignore ->
			Pattern.Ignore(ast.loc)
		is ast.Pattern.Single -> {
			val (loc, name) = ast.declare
			Pattern.Single(LocalDeclare(loc, name, ty))
		}
		is ast.Pattern.Destruct -> {
			val rt = ty as? Rt ?: TODO("error message")
			if (!rt.properties.sameSize(ast.destructed))
				TODO("error message")
			val patterns = rt.properties.zip(ast.destructed) { property, destructed ->
				checkPattern(property.ty, destructed)
			}
			Pattern.Destruct(ast.loc, patterns)
		}
	}

private fun checkImplicitCast(scope: Scope, expected: Ty, expr: ast.Expr): Expr =
	checkWorker(scope, Expected.ImplicitCast(expected), expr)

internal fun checkParameter(scope: Scope, parameter: Ft.Parameter, expr: ast.Expr): Expr =
	checkImplicitCast(scope, parameter.ty, expr)

internal fun checkAndInfer(scope: Scope, ast: ast.Expr): Pair<Ty, Expr> {
	val infer = Expected.Infer()
	val expr = checkWorker(scope, infer, ast)
	return Pair(infer.get(), expr)
}

internal fun checkWorker(scope: Scope, expected: Expected, xast: ast.Expr): Expr =
	when (xast) {
		is ast.At -> {
			val (loc, kind, tyAst, exprAst) = xast
			val ty = scope.getTy(tyAst)
			val subExpected =
				//TODO: rename Ast.Exact to Ast.ImplicitCast
				when (kind) {
					ast.At.Kind.Exact -> Expected.ImplicitCast(ty)
					ast.At.Kind.Convert -> Expected.ExplicitCast(ty)
				}
			val expr = checkWorker(scope, subExpected, exprAst)
			checkAny(loc, expected, ty, expr)
		}
		is ast.ExprTy ->
			checkTyAsExpr(scope, expected, xast.ty)
		is ast.Access -> {
			val v = scope.getV(xast.loc, xast.name)
			val (ty, expr) =
				when (v) {
					is Binding.Global -> {
						val (tyOrGen, value) = v.member as? TypedV ?: TODO("error message: can't have a py here")
						val ty = tyOrGen as? Ty ?: TODO("error message: can't have a generic here")
						Pair(ty, Value(xast.loc, value))
					}
					is Binding.Local ->
						Pair(v.declare.ty, v.declare)
				}
			checkAny(xast.loc, expected, ty, expr)
		}
		is ast.Call ->
			checkCall(scope, expected, xast.loc, xast.target, xast.args)
		is ast.Cs ->
			checkCs(scope, expected, xast)
		is ast.Ts ->
			checkTs(scope, expected, xast)
		is ast.GetProperty -> {
			val (loc, target, propertyName) = xast
			val (exprTy, expr) = checkAndInfer(scope, target)
			val ty = propertyTy(loc, exprTy, propertyName)
			checkAny(loc, expected, ty, GetProperty(loc, expr, propertyName))
		}
		is ast.Lambda -> {
			val (loc, signatureAst, bodyAst) = xast
			val returnTy = scope.getTy(signatureAst.returnTy)
			val parameters = signatureAst.parameters.map { param ->
				//TODO: duplicate code in typeCheckFn
				LocalDeclare(param.loc, param.name, scope.getTy(param.ty))
			}
			val (body, closureParameters) = scope.inClosure(parameters) {
				checkImplicitCast(scope, returnTy, bodyAst)
			}
			val ft = Ft(
				Ft.Origin.Lambda(loc),
				Ft.Signature(returnTy, parameters.map { param -> Ft.Parameter(param.name, param.ty) }))
			val lambda =
				Fn.Lambda(loc, parameters, closureParameters, body, ft)
			checkAny(loc, expected, ft, Lambda(lambda))
		}
		is ast.Let -> {
			//val (loc, patternAst, valueAst, exprAst) = xast
			val (valueTy, value) = checkAndInfer(scope, xast.value)
			val pattern = checkPattern(valueTy, xast.assigned)
			val expr = scope.addPattern(pattern) {
				checkWorker(scope, expected, xast.expr)
			}
			Let(xast.loc, pattern, value, expr)
		}
		is ast.List -> {
			val (loc, elementAsts) = xast
			//TODO:FACTOR OUT
			val elements =
				when (expected) {
					is Expected.ImplicitCast -> {
						val ty = expected.ty as? PrimInst ?: TODO("not a list")
						if (ty.gen == GenPrim.List) TODO("not a list")
						val elementTy = expected.ty.gen.tyParams.single()
						elementAsts.map { checkImplicitCast(scope, elementTy, it) }
					}
					is Expected.Infer -> {
						if (elementAsts.isEmpty)
							TODO("error message: can't infer type of empty list")
						val (elementTy, first) = checkAndInfer(scope, elementAsts.first)
						val elements = buildFromFirstAndMapTail(first, elementAsts) {
							checkImplicitCast(scope, elementTy, it)
						}
						val listTy = instantiateGenPrim(GenPrim.List, Arr.of(elementTy))
						expected.set(loc, listTy)
						elements
					}
					else ->
						TODO("other contexts")
				}
			EList(loc, elements)
		}
		is ast.Literal -> {
			val v = xast.value
			val (ty, value) =
				when (v) {
					is ast.LiteralValue.Int -> Pair(Prim.Int, V.Prim.Int(v.value))
					is ast.LiteralValue.Float -> Pair(Prim.Float, V.Prim.Float(v.value))
					is ast.LiteralValue.Str -> Pair(Prim.Str, V.Prim.String(v.value))
				}
			checkAny(xast.loc, expected, ty, Value(xast.loc, value))
		}
		is ast.Seq -> {
			val action = checkImplicitCast(scope, Prim.Void, xast.first)
			val expr = checkWorker(scope, expected, xast.then)
			Seq(xast.loc, action, expr)
		}
		is ast.Partial -> {
			val (loc, targetAst, argAsts) = xast
			val (fnTy, fn) = checkAndInfer(scope, targetAst)
			val ft = fnTy as? Ft ?: TODO("error message")
			val parameters = ft.signature.parameters
			if (parameters.size < argAsts.size)
				TODO("error message")
			val args = parameters.partialZip(argAsts) { param, arg -> checkParameter(scope, param, arg) }
			val ty = partiallyApplyFt(ft, args.size)
			checkAny(loc, expected, ty, Partial(loc, fn, args))
		}

		is ast.Quote -> {
			val (loc, head, partAsts) = xast
			val parts = partAsts.map { part ->
				val interpolated = checkImplicitCast(scope, Prim.Str, part.interpolated)
				Quote.Part(interpolated, part.text)
			}
			checkAny(loc, expected, Prim.Str, Quote(loc, head, parts))
		}

		is ast.Check -> {
			val (loc, checkedAst) = xast
			val checked = checkImplicitCast(scope, Prim.Bool, checkedAst)
			checkAny(loc, expected, Prim.Void, Check(loc, checked))
		}

		is ast.GenInst -> {
			val (loc, name, tyAsts) = xast
			val (ty, expr) = genInst(scope, loc, name, tyAsts)
			checkAny(loc, expected, ty, expr)
		}
	}

private fun genInst(scope: Scope, loc: Loc, name: Sym, tyAsts: Arr<ast.Ty>): Pair<Ty, Expr> {
	val binding = scope.getV(loc, name)
	val global = binding as? Binding.Global ?: TODO("error message: can't instantiate local")
	val memberV = global.member
	return when (memberV) {
		is MemberPy -> {
			val py = memberV.py
			val il = scope.getPyImpl(loc, py, tyAsts.map(scope::getTy))
			val ft = il.ft()
			//TODO: duplicate code in CallPy handler in checkCall
			val expr = Value(loc, il.fn)
			Pair(ft, expr)
		}
		is TypedV -> {
			val (tyOrGen, value) = memberV
			val genTy = tyOrGen as? Gen<*> ?: TODO("error message: not instantiable")
			val ty = scope.instantiateGeneric(loc, genTy, tyAsts)
			Pair(ty, Value(loc, value))
		}
	}
}

private fun checkCs(scope: Scope, expected: Expected, ast: ast.Cs): Expr {
	val (casedTy, cased) = checkAndInfer(scope, ast.cased)
	val casedVt = casedTy as? Vt ?: raise(ast.cased.loc, Err.CanOnlyCsVt(casedTy))
	val remainingTys = mutableListFrom(casedVt.variants)
	val parts = ast.parts.map { part ->
		val (partLoc, test, resultAst) = part
		val (testLoc, testTyAst, patternAst) = test
		val testTy = scope.getTy(testTyAst)
		//TODO:DUPLICATE CODE (addPattern)
		val pattern = checkPattern(testTy, patternAst)
		scope.addPattern(pattern) {
			val test = Cs.Test(testLoc, testTy, pattern)
			if (!remainingTys.tryRemoveUnordered(testTy))
				raise<Unit>(testLoc, Err.CsPartType(Arr.from(remainingTys), testTy))
			val result = checkWorker(scope, expected, resultAst)
			Cs.Part(partLoc, test, result)
		}
	}
	must(remainingTys.isEmpty(), ast.loc, Err.CasesUnhandled(Arr.from(remainingTys)))
	return Cs(ast.loc, casedVt, cased, parts)
}

private fun checkTs(scope: Scope, expected: Expected, ast: ast.Ts): Expr {
	val parts = ast.parts.map { part ->
		val test = checkImplicitCast(scope, Prim.Bool, part.test)
		val result = checkWorker(scope, expected, part.result)
		Ts.Part(part.loc, test, result)
	}
	val elze = checkWorker(scope, expected, ast.elze)
	return Ts(ast.loc, parts, elze)
}

//TODO:MOVE
fun<T> mutableListFrom(elements: Iterable<T>): MutableList<T> =
	mutableListOf<T>().apply { addAll(elements) }
/** Returns whether the element was removed. */
fun <T> MutableList<T>.tryRemoveUnordered(element: T): Bool {
	for (i in indices)
		if (element == this[i]) {
			this[i] = this.last()
			this.pop()
			return true
		}
	return false
}


internal fun typeCheck(baseScope: BaseScope, fns: Arr<FnAndAst>) {
	TODO()
}


private fun propertyTy(loc: Loc, ty: Ty, propertyName: Sym): Ty {
	val rt = ty as? Rt ?: raise(loc, Err.NotARc(ty))
	val propTy = rt.properties.findMap { prop ->
		opIf(prop.name == propertyName) { prop.ty }
	}
	return propTy ?: raise(loc, Err.NoSuchProperty(rt ,propertyName))
}
