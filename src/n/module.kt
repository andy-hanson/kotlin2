package n

import compile.err.*
import u.*

class Module(
	/** Content of the file when this module was read. This protects us against file changes. */
	val fileContent: String,
	/** Logical path, e.g. "a/b" */
	val path: Path,
	/** Actual resolved path, e.g. "a/b.nz" or "a/b/main.nz" */
	val fullPath: Path,
	//TODO:DOC
	val imports: Imports) {

	val members: ModuleMembers = TODO()
	//val moduleIls:

	fun getExport(loc: Loc, name: Sym): ModuleMember =
		members[name] ?: raise(loc, Err.ModuleHasNoMember(this, name))
}

typealias ModuleMembers = Lookup<Sym, ModuleMember>

typealias Imports = Lookup<Sym, Imported>

//TODO: The relation between this and Ast.imports is confusing, because this comes from an Ast.imports arr.
//TODO: every modul_member should have a pointer to its module?
class Imported(
	/** Location of the import identified */
	val importLoc: Loc,
	val path: RelPath,
	val content: ModuleMember)

sealed class ModuleMember {
	class Ty(val ty: TyOrGen) : ModuleMember()
	// TODO: fn values already carry their type with them, so can we represent this better?
	class TypedV(val ty: TyOrGen, val v: V) : ModuleMember()
	class MemberPy(val py: Py) : ModuleMember()
}
