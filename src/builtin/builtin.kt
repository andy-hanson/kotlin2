package builtin

import u.*
import n.*
import java.util.*

private var members = HashMap<Sym, ModuleMember>()
private var ils = HashMap<Py, HashMap<Arr<Ty>, Il>>()

private fun addTy(name: Sym, ty: TyOrGen) {
	members.add(name, MemberTy(ty))
}
private fun ty(name: String, ty: TyOrGen) {
	addTy(name.sym, ty)
}

private fun addVal(name: Sym, value: MemberV) {
	members.add(name, value)
}
private fun v(name: String, value: MemberV) {
	addVal(name.sym, value)
}

private fun addIl(il: Il) {
	//TODO:multimapadd HELPER
	val x = ils[il.py]
	when (x) {
		null ->
			// Add the first implementation.
			ils.add(il.py, hashMapOf(il.tys to il))
		else ->
			// Add another implementation
			x.add(il.tys, il)
	}
}

private fun addPy(name: String, genArgName: String, make: (Ty, Ty) -> Arr<Pair<String, Ty>>): Py =
	TODO()



//TODO:MOVE
internal object Builtin {
	val allMembers: Lookup<Sym, ModuleMember> = Lookup.fromHashMap(members)
	val allIls: Lookup<Py, Lookup<Arr<Ty>, Il>> = Lookup.fromHashMapMapped(ils) { k, v -> k to Lookup.fromHashMap(v) }
}
