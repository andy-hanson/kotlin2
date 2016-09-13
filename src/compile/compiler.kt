package compile
import compile.err.*
import compile.lex.*
import compile.parse.parseModule
import n.*
import u.*

class Compiler(private val io: FileInput) {
	private var modules = hashMapOf<Path, Module>()

	private fun<T> doWork(fullPath: Path, f: Thunk<T>): T {
		try {
			return f()
		} catch (error: CompileError) {
			error.path = fullPath
			val message = error.output(this::translateLoc)
			System.err.println(message)
			throw error
		}
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
		TODO()
	}
}

private fun compile(getModuleRel: (RelPath) -> Module, path: Path, fullPath: Path, moduleAst: ast.Module): Module {
	TODO()
}





