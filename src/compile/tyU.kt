package compile

import u.*
import n.*

//TODO: this file, man, this file!

internal fun ftOfRtCtr(rt: Rt): Ft {
	TODO()
}

internal fun partiallyApplyFt(ft: Ft, nPartialArgs: Int): Ft {
	val parameters = ft.signature.parameters.rtailN(nPartialArgs)
	return Ft(Ft.Origin.FromPartial(ft), ft.signature.copy(parameters = parameters))
}
