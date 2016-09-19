package compile

import n.*

//TODO: this doesn't belong here...
internal fun typedVFromFn(fn: Fn.Declared): TypedV =
	TypedV(fn.ty.toTyOrGen(), fn)

