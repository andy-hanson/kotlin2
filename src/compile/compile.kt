package compile

import compile.err.*
import compile.typeCheck.typeCheck
import u.*
import n.*

//TODO:RENAME XXX
internal fun compile(getModuleRel: (RelPath) -> Module, xxx: LinearizedModule): Module {
	val (logicalPath, fullPath, ast) = xxx
	val imports = getImports(getModuleRel, ast.imports)
	val module = Module(logicalPath, fullPath, imports)
	val (tys, fns) = writeNilDeclarations(module, ast.decls)
	val baseScope = BaseScope.get(module)
	fillInStuff(baseScope, tys, fns)
	typeCheck(baseScope, fns)
	codeGen(module)
	return module
}

internal fun getImports(getModuleRel: (RelPath) -> Module, imports: Arr<ast.Imports>): Imports =
	Lookup.build<Sym, Imported> { tryAdd ->
		fun add(loc: Loc, path: RelPath, name: Sym, member: ModuleMember) {
			if (tryAdd(name, Imported(loc, path, member)) != null)
				raise<Unit>(loc, Err.NameAlreadyBound(name))
		}

		for (import in imports) {
			when (import.path) {
				is ast.ImportPath.Global -> TODO()
				is ast.ImportPath.Relative -> {
					val relPath = import.path.path
					val module = getModuleRel(relPath)
					for ((loc, name) in import.imported)
						add(loc, relPath, name, module.getExport(loc, name))
				}
			}
		}
	}

internal fun fillInStuff(baseScope: BaseScope, tys: Arr<TyAndAst>, fns: Arr<FnAndAst>) {
	fun getTy(genVars: Arr<GenVar>, ty: ast.Ty): Ty =
		baseScope.getTyGivenTyParams(genVars, ty)
	for ((ast, ty) in tys)
		fillGeneric(::getTy, ast, ty)
	for ((ast, fn) in fns)
		fillInFnSignature(::getTy, ast.signature, fn)
}

internal fun codeGen(module: Module) {
	TODO()
}
