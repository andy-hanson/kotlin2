package testU

import compile.Compiler
import compile.err.CompileError
import compile.lex.Token
import n.*
import u.*

private val testIo = NativeFileInput(Path.of("test-nz"))
private val testCompiler = Compiler(testIo)

object TestU {
	fun printTokens(path: Path) {
		val tokens = testCompiler.lex(path)
		for ((token, loc) in tokens)
			println("$token @ $loc")
	}

	fun lex(path: Path): Arr<Token> =
		testCompiler.lex(path).map { it.token }

	fun parse(path: Path): ast.Module =
		testCompiler.parse(path)

	fun fnNamed(module: Module, name: String): Fn.Declared {
		val member =
			try {
				module.getExport(nilLoc, Sym.ofString(name))
			} catch (_: CompileError) {
				throw Exception("No function named $name")
			}
		if (member !is ModuleMember.TypedV)
			throw Exception("$name is not a fn")
		val f = member.v
		if (f !is Fn.Declared)
			throw Exception("$name is not a fn")
		return f
	}
}
