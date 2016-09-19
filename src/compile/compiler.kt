package compile
import compile.err.*
import compile.lex.*
import compile.parse.parseModule
import compile.typeCheck.typeCheck
import n.*
import u.*

class Compiler(private val io: FileInput) {
	private var modules = hashMapOf<Path, Module>()

	private fun outputError(error: CompileError) {
		val message = error.output(this::translateLoc)
		System.err.println(message)
	}

	private fun<T> doWork(fullPath: Path, f: Thunk<T>): T =
		try {
			f()
		} catch (error: CompileError) {
			error.path = fullPath
			outputError(error)
			throw error
		}

	//TODO:NAME
	private fun<T> doWork2(f: Thunk<T>): T =
		try {
			f()
		} catch (error: CompileError) {
			outputError(error)
			throw error
		}

	private fun translateLoc(fullPath: Path, loc: Loc): LcLoc =
		FileInput.read(io, fullPath) { source ->
			LcLoc.from(source, loc)
		}

	fun lex(path: Path): Arr<LexedEntry> =
		doWork(path) {
			FileInput.read(io, path, ::lexToArray)
		}

	fun parse(path: Path): ast.Module =
		doWork(path) {
			FileInput.read(io, path, ::parseModule)
		}

	fun compile(path: Path): Module {
		fun getModule(path: Path) =
			modules[path] ?: TODO("error message: module not found")
		val linearModules = doWork2 { linearizeModuleDependencies(io, path) }
		for (linearModule in linearModules) {
			//TODO: not sue this is right...
			fun getModuleRel(rel: RelPath): Module = getModule(path.resolve(rel))
			val module = doWork(linearModule.fullPath) {
				compileSingleModule(::getModuleRel, linearModule)
			}
			modules.add(path, module)
		}
		return getModule(path)
	}
}

//TODO:RENAME XXX
internal fun compileSingleModule(getModuleRel: (RelPath) -> Module, xxx: LinearizedModule): Module {
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
