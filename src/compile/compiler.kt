package compile
import compile.err.*
import compile.lex.*
import compile.parse.parseModule
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
			modules.get(path)
		val linearModules = doWork2 { linearizeModuleDependencies(io, path) }
		TODO()
	}
}
