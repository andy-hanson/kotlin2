package compile

import compile.err.*
import compile.parse.parseModule
import u.*
import n.*
import java.util.*
import javax.sound.sampled.Line

/**
Loads modules and produces a linear compilation order.
https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
*/
internal data class LinearizedModule(val logicalPath: Path, val fullPath: Path, val ast: ast.Module)

internal fun linearizeModuleDependencies(io: FileInput, startPath: Path): Arr<LinearizedModule> {
	val f = Floof(io)
	f.floof(Path.empty, zeroLoc, RelPath(0, startPath))
	return f.finish()
}

//TODO:NAME
private class Floof(private val io: FileInput) {
	private val modules = mutableListOf<LinearizedModule>()
	// This also functions as `visited`
	private val map = HashMap<Path, Foo>()

	fun floof(from: Path, fromLoc: Loc, rel: RelPath) {
		val path = from.resolve(rel)
		if (map.containsKey(path))
			raiseWithPath(from, fromLoc, Err.CircularDepdendency(path))
		else {
			val (fullPath, source) = resolve(io, from, fromLoc, rel)
			val ast =
				try {
					parseModule(source)
				} catch (error: CompileError) {
					error.path = fullPath
					throw error
				}
			source.close()

			// Calculate dependencies
			val imports =
				ast.imports.map { import ->
					when (import.path) {
						is ast.ImportPath.Global ->
							TODO()
						is ast.ImportPath.Relative ->
							Bar(import.loc, import.path.path)
					}
				}

			// Mark this node as visited
			map[path] = Foo(imports, ast)
			//TODO: do this in parallel
			for (import in imports) {
				floof(path, import.loc, import.rel)
			}
			// We are linearizing: write out this module after all of its dependencies are written out.
			modules.add(LinearizedModule(path, fullPath, ast))
		}
	}

	fun finish() =
		Arr.from(modules)
}


//TODO:NAME
private class Foo(val imports: Arr<Bar>, val ast: ast.Module)
private class Bar(val loc: Loc, val rel: RelPath)

private val extension = ".nz"
private val mainNz = Sym.ofString("main$extension")

private fun attempt(io: FileInput, path: Path): Resolved? =
	try {
		val source = io.open(path)
		Resolved(path, source)
	} catch (_: FileNotFound) {
		null
	}

private data class Resolved(val fullPath: Path, val input: Input)
/**
Returns the resolved path, with '.nz' included.
Normally we just add a '.nz' extension, but for directories we try a 'main.nz' inside of it.
*/
//TODO: absolutely forbid unnecessary specification of 'main', because we want modules to only have 1 valid path.
private fun resolve(io: FileInput, from: Path, fromLoc: Loc, rel: RelPath): Resolved {
	fun<T> raiseErr(message: Err): T =
		raiseWithPath(from, fromLoc, message)
	fun attempt(path: Path): Resolved? = attempt(io, path)
	val base = from.resolve(rel)
	val mainPath = base.add(mainNz)
	fun attemptMain() = attempt(mainPath)
	return if (rel.isParentsOnly)
		attemptMain() ?: raiseErr(Err.CantFindLocalModuleFromDirectory(rel, mainPath))
	else {
		val (pre, last) = base.directoryAndBasename()
		val regularPath = pre.add(Sym.ofString(last.toString() + extension))
		attempt(regularPath) ?: attemptMain() ?:
			raiseErr(Err.CantFindLocalModuleFromFileOrDirectory(rel, regularPath, mainPath))
	}
}

