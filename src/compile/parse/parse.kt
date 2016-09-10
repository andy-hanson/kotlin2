package compile.parse

import ast.Module
import compile.lex.Lexer
import u.*

fun parse(source: Input): Module =
	Lexer(source).parseModule()
