package n

import compile.err.*
import u.*

class Module(
	/** Logical path, e.g. "a/b" */
	val path: Path,
	/** Actual resolved path, e.g. "a/b.nz" or "a/b/main.nz" */
	val fullPath: Path,
	//TODO:DOC
	val imports: Imports) {
	var members: ModuleMembers by Late()

	fun getExport(loc: Loc, name: Sym): ModuleMember =
		members[name] ?: raise(loc, Err.ModuleHasNoMember(this, name))
}

typealias ModuleMembers = Lookup<Sym, ModuleMember>

typealias Imports = Lookup<Sym, Imported>

//TODO: The relation between this and Ast.imports is confusing, because this comes from an Ast.imports arr.
//TODO: every modul_member should have a pointer to its module?
class Imported(
	/** Location of the import identified */
	val loc: Loc,
	val path: RelPath,
	val content: ModuleMember)

sealed class ModuleMember {
	class Ty(val ty: TyOrGen) : ModuleMember()
	sealed class MemberV : ModuleMember() {
		// TODO: fn values already carry their type with them, so can we represent this better?
		data class TypedV(val ty: TyOrGen, val v: V) : MemberV()

		class MemberPy(val py: Py) : MemberV()
	}
}
