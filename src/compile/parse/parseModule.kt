package compile.parse

import u.*
import ast.*
import compile.lex.*

fun parseModule(source: Input): Module =
	Lexer(source).parseModule()

private fun Lexer.parseModule(): Module {
	val (start, next) = posNext()
	val (imports, newStartNext) =
		when (next) {
			Token.Import -> {
				mustSkip(Token.Indent)
				val imports = parseImports()
				Pair(imports, posNext())
			}
			else ->
				Pair(Arr.empty<Imports>(), Lexer.PosNext(start, next))
		}

	var (declStart, declNext) = newStartNext
	val decls = build<Decl> {
		while (declNext !== Token.EOF) {
			add(parseDecl(declStart, declNext))
			declStart = curPos()
			declNext = nextToken()
		}
	}

	return Module(imports, decls)
}

private fun Lexer.parseImports(): Arr<Imports> =
	buildLoop {
		val (start, next) = posNext()
		val (path, firstImport) = parseImportPathAndFirstImport(start, next)
		val (importedNames, shouldContinue) =
			buildLoop0WithFirst<LocalDeclare, Bool>(firstImport) {
				val (start, next) = posNext()
				when (next) {
					is Token.Name -> Builder.Continue(LocalDeclare(locFrom(start), next.name))
					is Token.Newline -> Builder.Done(true)
					is Token.Dedent -> Builder.Done(false)
					else -> unexpected(start, next)
				}
			}
		val imports = Imports(locFrom(start), path, importedNames)
		Pair(imports, shouldContinue)
	}

//TODO:move
fun<T> count(f: Thunk<T?>): Pair<Int, T> {
	var n = 0
	while (true) {
		val x = f()
		when (x) {
			null -> n++
			else -> return Pair(n, x)
		}
	}
}

private fun Lexer.parseImportPathAndFirstImport(start: Pos, next: Token): Pair<ImportPath, LocalDeclare> {
	val (nLeadingDots, startName) =
		when (next) {
			Token.Dot -> {
				// First '.' is special because it can be repeated to reach parents.
				val (nExtraDots, name) = count {
					val next = nextToken()
					when (next) {
						Token.Dot -> null
						is Token.Name -> next.name
						else -> unexpected(start, next)
					}
				}
				Pair(nExtraDots + 1, name)
			}
			is Token.Name ->
				Pair(0, next.name)
			else ->
				unexpected(start, next)
		}

	val (names, firstImport) =
		buildLoop0WithFirst<Sym, LocalDeclare>(startName) {
			val (start, next) = posNext()
			when (next) {
				Token.Dot -> Builder.Continue(parseName())
				is Token.Name -> Builder.Done(LocalDeclare(locFrom(start), next.name))
				else -> unexpected(start, next)
			}
		}
	val namesPath = Path(names)
	val path =
		when (nLeadingDots) {
			0 -> ImportPath.Global(namesPath)
			else -> ImportPath.Relative(RelPath(nLeadingDots, namesPath))
		}

	return Pair(path, firstImport)
}
