package compile.err

import u.*
import compile.lex.Token
import n.*

sealed class Err {
	// Module loader
	class CircularDepdendency(val path: Path) : Err() {
		override fun toString() =
			"There is a circular dependency involving $path"
	}

	class CantFindLocalModuleFromFileOrDirectory(
		val importPath: RelPath,
		val filePath: Path,
		val dirPath: Path)  : Err() {
		override fun toString() =
			"Can't find any module $importPath. Tried $filePath and $dirPath."
	}

	class CantFindLocalModuleFromDirectory(val path: RelPath, val dirPath: Path) : Err()  {
		override fun toString() =
			"Can't find any module %relPath. Tried $dirPath."
	}

	// Lexer
	object LeadingSpace : Err() {
		override fun toString() =
			"Line may not begin with a space."
	}

	object NumberMustHaveDigitsAfterDecimalPoint : Err() {
		override fun toString() =
			"A number *must* have digits after the decimal point; e.g. `1.0` and not `1.`."
	}

	object TooMuchIndent : Err() {
		override fun toString() =
			"This line is indented multiple times compared to the previous line. Use only one indent."
	}

	object TrailingSpace : Err() {
		override fun toString() =
			"This line has a trailing space character."
	}

	class UnrecognizedCharacter(val ch: Char) : Err() {
		override fun toString() =
			"Unrecognized character: `$ch`"
	}

	// Parser
	object BlockCantEndInDeclare : Err() {
		override fun toString() =
			"Last line of block can't be a variable declaration."
	}

	object CsMustBeInLineContext : Err() {
		override fun toString() =
			"`cs` and `ts` can't appear in sub-expressions because they need an indented blocks."
	}

	object EmptyExpression : Err() {
		override fun toString() =
			"This expression is empty."
	}

	object EqualsInExpression : Err() {
		override fun toString() =
			"`=` can not appear inside an expression. Did you mean `==`?"
	}

	object PrecedingEquals : Err() {
		override fun toString() =
			"`=` must be preceded by a name."
	}

	class Unexpected(val t: Token) : Err() {
		override fun toString() =
			"Unexpected token: $t"
	}

	// Scope
	class CantBind(val name: Sym) : Err() {
		override fun toString() =
			"There is no $name in scope."
	}

	class ModuleHasNoMember(val module: Module, val name: Sym) : Err() {
		override fun toString() =
			"Module ${module.fullPath} has no member $name."
	}

	class NameAlreadyBound(val name: Sym) : Err() {
		override fun toString() =
			"$name has already been declared in this scope."
	}

	class NoIlInScope(val py: Py, val wanted: Arr<Ty>, val availableIls: Arr<Arr<Ty>>) : Err() {
		override fun toString() =
			"There is no implementation in scope of $py for types $wanted.\nAvailable ils are: $availableIls."
	}

	// Type check
	class CanOnlyCsVt(val ty: Ty) : Err() {
		override fun toString() =
			"Expected a vt. Got: $ty"
	}

	class CsPartType(val possibleTys: Arr<Ty>, val handledTy: Ty) : Err() {
		override fun toString() =
			"$handledTy is not a possible value at this point. Possible types are: $possibleTys"
	}

	class CantConvertRtMissingProperty(val convertTo: Rt, val convertFrom: Rt, val propertyName: Sym) : Err() {
		override fun toString() =
			"Can't convert to $convertTo from $convertFrom: missing property $propertyName"
	}

	class CantConvertVt(val convertTo: Vt, val convertFrom: Vt) : Err() {
		override fun toString() = "Can't convert to $convertTo from $convertFrom."
	}

	class CasesUnhandled(val unhandled: Arr<Ty>) : Err() {
		override fun toString() =
			"The following cases are not handled: $unhandled"
	}

	class CombineTypes(val a: Ty, val b: Ty) : Err() {
		override fun toString() =
			"Can't combine types $a and $b."
	}

	class GenInstParameters(val nExpected: Int, val nActual: Int) : Err() {
		override fun toString() =
			"Expected $nExpected generic parameters, got $nActual."
	}

	class NotAFunction(val actual: Ty) : Err() {
		override fun toString() =
			"Expected a fn, got: $actual"
	}

	class NotARc(val actual: Ty) : Err() {
		override fun toString() =
			"Expected a rc, got: $actual"
	}

	class NoSuchProperty(val rt: Rt, val name: Sym) : Err() {
		override fun toString() =
			"Type $rt has no property $name"
	}

	class NotExpectedType(val expected: Ty, val actual: Ty) : Err() {
		override fun toString() =
			"Expected: $expected\nGot: $actual"
	}

	class NotExpectedTypeAndNoConversion(val expected: Ty, val actual: Ty) : Err() {
		override fun toString() =
			"Expected to convert to: $expected\nGot: $actual"
	}

	class NumArgs(val nParameters: Int, val nArguments: Int) : Err() {
		override fun toString() =
			"This function needs $nParameters parameters, but is given $nArguments."
	}
}


class CompileError(val loc: Loc, val kind: Err) : Exception() {
	constructor(loc: Loc, kind: Err, path: Path) : this(loc, kind) {
		this.path = path
	}

	var path: Path by Late()

	fun output(translateLoc: (Path, Loc) -> LcLoc): String {
		val lcLoc = translateLoc(path, loc)
		return "Error at $path $lcLoc: $kind"
	}
}
