package compile.typeCheck

import compile.err.*
import compile.unInstantiateForCompare
import u.*
import n.*

internal fun checkAny(loc: Loc, expected: Expected, actualTy: Ty, actualExpr: Expr): Expr {
	fun check(asserter: (Loc, Ty, Ty) -> Conversion?, expectedTy: Ty): Expr =
		asserter(loc, expectedTy, actualTy) opMap { Convert(it, actualExpr) } ?: actualExpr
	return when (expected) {
		is Expected.ImplicitCast -> check(::assertImplicitCast, expected.ty)
		is Expected.ExplicitCast -> check(::assertExplicitCast, expected.ty)
		is Expected.Infer -> {
			expected.set(loc, actualTy)
			actualExpr
		}
		is Expected.InferGeneric ->
			check({ loc, expectedTy, actual -> assertWithGenVars(expected.vars, loc, expectedTy, actual) }, expected.ty)
	}
}

//TODO: TypeCheckU.ml stuff here
/** Allows the second ty to be *exactly* the first, or if the first is a union, a member of it. */
private fun assertImplicitCast(loc: Loc, expected: Ty, actual: Ty): Conversion? =
	assertWithGenVars(Arr.empty(), loc, expected, actual)

/**
More permissive than `assert_exact`.
Allows the second ty to be convertible to the first.
*/
internal fun assertExplicitCast(loc: Loc, expected: Ty, actual: Ty): Conversion? {
	//TODO: Err.CantConvert
	fun<T> fail(): T =
		raise(loc, Err.NotExpectedTypeAndNoConversion(expected, actual))
	return when (expected) {
		is Prim, is PrimInst, is Vt ->
			//TODO: just throw an error because we're not doing a cast!
			assertImplicitCast(loc, actual, expected)
		is Rt ->
			assertConvertRt(loc, expected, actual as? Rt ?: fail())
		is Ft ->
			//TODO LATER: function conversion
			assertImplicitCast(loc, expected, actual)
		is GenVar ->
			TODO()
	}
}


private fun assertExact(loc: Loc, expected: Ty, actual: Ty) {
	assertExactWorker(Arr.empty(), loc, expected, actual)
}

/** Like `assert_implicit_cast` but with generic variables. */
private fun assertWithGenVars(inferring: Arr<GenVarInferring>, loc: Loc, expected: Ty, actual: Ty): Conversion? {
	fun<T> fail(): T =
		raise(loc, Err.NotExpectedType(expected, actual))
	return when (expected) {
		is Prim, is PrimInst, is Rt, is GenVar -> {
			assertExactWorker(inferring, loc, expected, actual);
			null
		}
		is Vt ->
			when (actual) {
				is Vt ->
					assertConvertVt(inferring, loc, expected, actual)
				is Prim, is PrimInst, is Rt, is Ft ->
					expected.tys.findIndex { isExact(inferring, it, actual) } opMap { Conversion.Vv(expected, it) } ?: fail()
				is GenVar ->
					TODO()
			}
		// Functions match structurally, so ignore names.
		is Ft -> {
			val sigExpected = expected.signature
			val sigActual = (actual as? Ft ?: fail()).signature
			assertExactWorker(inferring, loc, sigExpected.returnTy, sigActual.returnTy)
			if (!sigExpected.parameters.sameSize(sigActual.parameters))
				fail<Unit>() //TODO: better error message
			sigExpected.parameters.eachZip(sigActual.parameters) { expectedParam, actualParam ->
				assertExactWorker(inferring, loc, expectedParam.ty, actualParam.ty)
			}
			null
		}
	}
}


//TODO: When a recursive call writes to 'inferring', we should be able to back out of it if that failed.
private fun isExact(inferring: Inferring, expected: Ty, actual: Ty): Bool =
	when (expected) {
		is Prim ->
			expected == actual
		is GenVar -> {
			val matching = inferring.find { it.genVar == expected }
			when (matching) {
				null ->
					expected == actual
				else -> {
					val known = matching.getKnown()
					when (known) {
						is GenVar ->
							// Prevent infinite recursion.
							known == actual
						null -> {
							matching.setKnown(actual);
							true
						}
						else ->
							isExact(inferring, known, actual)
					}
				}
			}
		}
		else -> {
			val unInstantiated = unInstantiateForCompare(expected, actual)
			when (unInstantiated) {
				null -> expected == actual
				else -> {
					val (expectedInstWith, actualInstWith) = unInstantiated
					expectedInstWith.allZip(actualInstWith) { expected, actual ->
						isExact(inferring, expected, actual)
					}
				}
			}
		}
	}

private fun assertExactWorker(inferring: Inferring, loc: Loc, expected: Ty, actual: Ty) {
	if (!isExact(inferring, expected, actual))
		raise<Unit>(loc, Err.NotExpectedType(expected, actual))
}

private fun assertConvertVt(inferring: Inferring, loc: Loc, expected: Vt, actual: Vt): Conversion? {
	//TODO: try conversion...
	assertExactWorker(inferring, loc, expected, actual)
	return null
}

private fun assertConvertRt(loc: Loc, expected: Rt, actual: Rt): Conversion? =
	opIf (expected != actual) {
		val indexes = expected.properties.map { eprop ->
			val matchingIndex = actual.properties.findIndex { aprop ->
				returning(eprop.name == aprop.name) { areEqual ->
					if (areEqual)
						assertExact(loc, eprop.ty, aprop.ty)
				}
			}
			matchingIndex ?: raise(loc, Err.CantConvertRtMissingProperty(expected, actual, eprop.name))
		}
		Conversion.Rc(expected, indexes)
	}

