package compile.err

import u.*
import compile.lex.Token

sealed class Err {
	// Module loader

	// Lexer
	object LeadingSpace : Err()
	object NumberMustHaveDigitsAfterDecimalPoint : Err()
	object TooMuchIndent : Err()
	object TrailingSpace : Err()
	data class UnrecognizedCharacter(val ch: Char) : Err()

	// Parser
	object BlockCantEndInDeclare : Err()
	object CsMustBeInLineContext : Err()
	object EmptyExpression : Err()
	object EqualsInExpression : Err()
	object PrecedingEquals : Err()
	data class Unexpected(val t: Token) : Err()

	// Bind

	// Type check
}

class CompileError(val path: Path, val loc: Loc, val kind: Err) : Exception() {

}
