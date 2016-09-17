package compile

import n.*

//TODO: this doesn't belong here...
internal fun typedVFromFn(fn: Fn.Declared): ModuleMember.MemberV.TypedV =
	ModuleMember.MemberV.TypedV(fn.ty.toTyOrGen(), fn)

