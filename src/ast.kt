package ast

import u.*

data class LocalDeclare(val loc: Loc, val name: Sym) : HasSexpr {
	override fun toSexpr() =
		TODO()
}
class TyParam(val loc: Loc, val name: Sym) : HasSexpr {
	override fun toSexpr() =
		TODO()
}

sealed class Ty : HasSexpr {
	abstract val loc: Loc

	data class Access(override val loc: Loc, val name: Sym) : Ty() {
		override fun toString() = name.toString()
		override fun toSexpr() =
			Sexpr.S(name)
	}
	data class Inst(override val loc: Loc, val instantiated: Ty, val tyArgs: Arr<Ty>) : Ty() {
		override fun toString() =
			"[$instantiated $tyArgs]"
		override fun toSexpr() =
			sexprTuple(Arr.cons(instantiated, tyArgs))
	}
}

sealed class Pattern : HasSexpr {
	class Ignore(val loc: Loc) : Pattern() {
		override fun toString() =
			"Ignore"
		override fun toSexpr() =
			sexpr("Ignore")
	}
	class Single(val declare: LocalDeclare) : Pattern() {
		override fun toString() =
			"Single($declare)"
		override fun toSexpr() =
			declare.toSexpr()
	}
	class Destruct(val loc: Loc, val destructed: Arr<Pattern>) : Pattern() {
		override fun toString() =
			"Destruct($destructed)"
		override fun toSexpr() =
			sexpr(destructed)
	};
}

class Parameter(val loc: Loc, val name: Sym, val ty: Ty) : HasSexpr {
	override fun toString() =
		"$name $ty"
	override fun toSexpr() =
		sexprTuple(name, ty)
}

class Signature(val loc: Loc, val returnTy: Ty, val parameters: Arr<Parameter>) : HasSexpr {
	override fun toString() =
		"$returnTy $parameters"
	override fun toSexpr() =
		sexpr("Signature", returnTy, sexpr(parameters))
}

sealed class Expr : HasSexpr {
	abstract val loc: Loc
}

data class At(override val loc: Loc, val kind: Kind, val ty: Ty, val expr: Expr) : Expr() {
	override fun toString() =
		"At($kind, $ty, $expr)"
	override fun toSexpr() =
		sexpr("At", kind, ty, expr)

	enum class Kind : HasSexpr {
		Exact,
		Convert;

		override fun toSexpr() = sexpr(this.name)
	}
}

class ExprTy(val ty: Ty) : Expr() {
	override val loc: Loc
		get() = ty.loc

	override fun toString() =
		"ExprTy($ty)"
	override fun toSexpr() =
		sexpr("ExprTy", ty)
}

class Access(override val loc: Loc, val name: Sym) : Expr() {
	override fun toString() =
		"Access($name)"
	override fun toSexpr() =
		sexpr("Access", name)
}

class Call(override val loc: Loc, val target: Expr, val args: Arr<Expr>) : Expr() {
	override fun toString() =
		"Call($target, $args)"
	override fun toSexpr() = sexpr("Call") {
		s(target)
		for (arg in args)
			s(arg)
	}
}

class Cs(override val loc: Loc, val cased: Expr, val parts: Arr<Part>) : Expr() {
	data class Part(val loc: Loc, val test: Test, val result: Expr) : HasSexpr {
		override fun toString() =
			"Cs.Part($test, $result)"

		override fun toSexpr() =
			sexprTuple(test, result)
	}
	data class Test(val loc: Loc, val testTy: Ty, val pattern: Pattern) : HasSexpr {
		override fun toString() =
			"$testTy, $pattern"
		override fun toSexpr() =
			sexprTuple(testTy, pattern)
	}

	override fun toString() =
		"Cs($cased, $parts)"
	override fun toSexpr() =
		sexpr("Cs", cased, sexpr(parts))
}

class Ts(override val loc: Loc, val parts: Arr<Part>, val elze: Expr) : Expr() {
	class Part(val loc: Loc, val test: Expr, val result: Expr) : HasSexpr {
		override fun toString() =
			"Ts.Part($test, $result)"
		override fun toSexpr() =
			sexprTuple(test, result)
	}

	override fun toString() =
		"Ts($parts, $elze)"
	override fun toSexpr() =
		sexpr("Ts", sexpr(parts), elze)
}

data class GetProperty(override val loc: Loc, val target: Expr, val propertyName: Sym) : Expr() {
	override fun toString() =
		"GetProperty($target, $propertyName)"
	override fun toSexpr() =
		sexpr("GetProperty", target, propertyName)
}

data class Lambda(override val loc: Loc, val signature: Signature, val body: Expr) : Expr() {
	override fun toString() =
		"Lambda($signature, $body)"

	override fun toSexpr() =
		sexpr("Lambda", signature, body)
}

class Let(override val loc: Loc, val assigned: Pattern, val value: Expr, val expr: Expr) : Expr() {
	override fun toString() =
		"Let($assigned = $value, $expr)"
	override fun toSexpr() =
		sexpr("Let", sexprTuple(assigned, value), expr)
}

data class List(override val loc: Loc, val parts: Arr<Expr>) : Expr() {
	override fun toString() =
		"List($parts)"
	override fun toSexpr() = sexpr ("List") {
		s("List")
		for (part in parts)
			s(part)
	}
}

class Literal(override val loc: Loc, val value: LiteralValue) : Expr() {
	override fun toString() =
		value.toString()
	override fun toSexpr() =
		value.toSexpr()
}

sealed class LiteralValue : HasSexpr {
	class Int(val value: Long) : LiteralValue() {
		override fun toString() =
			value.toString()
		override fun toSexpr() =
			Sexpr.N(value)
	}
	class Float(val value: Double) : LiteralValue() {
		override fun toString() =
			value.toString()
		override fun toSexpr() =
			Sexpr.F(value)
	}
	class Str(val value: String) : LiteralValue() {
		override fun toString() =
			"\"${value.toString()}\""
		override fun toSexpr() =
			Sexpr.Str(value)
	}
}

class Seq(override val loc: Loc, val first: Expr, val then: Expr) : Expr() {
	override fun toString() =
		"Seq($first, $then)"
	override fun toSexpr() =
		sexpr("Seq", first, then)
}

data class Partial(override val loc: Loc, val target: Expr, val args: Arr<Expr>) : Expr() {
	override fun toString() =
		"Partial($target, $args)"
	override fun toSexpr() = sexpr("Partial") {
		s(target)
		for (arg in args)
			s(arg)
	}
}

// For string with no interpolations, Literal is used instead
//TODO: above is silly, kill LiteralValue.Str and always use a quote
data class Quote(override val loc: Loc, val head: String, val parts: Arr<Part>) : Expr() {
	class Part(val interpolated: Expr, val text: String) : HasSexpr {
		override fun toString() =
			"$interpolated, \"$text\""
		override fun toSexpr() =
			sexprTuple(interpolated, sexpr(text))
	}

	override fun toString() =
		"Quote($head, $parts)"
	override fun toSexpr() = sexpr("Quote") {
		s(head)
		for (part in parts)
			s(part)
	}
}


data class Check(override val loc: Loc, val checked: Expr) : Expr() {
	override fun toString() =
		"Check($checked)"
	override fun toSexpr() =
		sexpr("Check", checked)
}

data class GenInst(override val loc: Loc, val instantiatedName: Sym, val args: Arr<Ty>) : Expr() {
	override fun toString() =
		"GenInst($instantiatedName, $args)"
	override fun toSexpr() = sexpr("GenInst") {
		s(instantiatedName)
		for (arg in args)
			s(arg)
	}
}

sealed class Decl : HasSexpr {
	abstract val loc: Loc
	abstract val name: Sym
}

sealed class DeclVal : Decl()

class Fn(override val loc: Loc, val head: Head, val signature: Signature, val body: Expr) : DeclVal() {
	sealed class Head : HasSexpr {
		abstract val name: Sym

		class Plain(override val name: Sym) : Head() {
			override fun toString() =
				name.toString()

			override fun toSexpr() =
				Sexpr.S(name)
		}

		data class Generic(override val name: Sym, val params: Arr<TyParam>) : Head() {
			override fun toString() =
				"[$name ${params.joinToString(" ")}]"

			override fun toSexpr() =
				sexprTuple(Arr.cons(name, params))
		}
	}

	override val name: Sym
		get() = head.name

	override fun toString() =
		"fn $head $signature $body"

	override fun toSexpr() =
		sexpr("fn") {
			s(sexprTuple(build {
				add(head)
				add(signature.returnTy)
				for (param in signature.parameters) {
					add(param.name)
					add(param.ty)
				}
			}))
			s(body)
		}
}

sealed class DeclTy : Decl()

class Rt(override val loc: Loc, override val name: Sym, val properties: Arr<Property>) : DeclTy() {
	override fun toString() =
		TODO()
	override fun toSexpr() =
		TODO()

	class Property(val loc: Loc, val name: Sym, val ty: Ty) : HasSexpr {
		override fun toSexpr() =
			TODO()
	}
}

class GenRt(override val loc: Loc, override val name: Sym, val params: Arr<TyParam>, val properties: Arr<Rt.Property>) : DeclTy() {
	override fun toString() =
		TODO()
	override fun toSexpr() =
		TODO()
}

class Vt(override val loc: Loc, override val name: Sym, val variants: Arr<Ty>) : DeclTy() {
	override fun toString() =
		TODO()
	override fun toSexpr() =
		TODO()
}

class GenVt(override val loc: Loc, override val name: Sym, val params: Arr<TyParam>, val variants: Arr<Ty>) : DeclTy() {
	override fun toString() =
		TODO()
	override fun toSexpr() =
		TODO()
}

class Ft(override val loc: Loc, override val name: Sym, val signature: Signature) : DeclTy() {
	override fun toString() =
		TODO()
	override fun toSexpr() =
		TODO()
}

class GenFt(override val loc: Loc, override val name: Sym, val params: Arr<TyParam>, val signature: Signature) : DeclTy() {
	override fun toString() =
		TODO()
	override fun toSexpr() =
		TODO()
}

class Module(val imports: Arr<Imports>, val decls: Arr<Decl>) : HasSexpr {
	override fun toString(): String =
		"Module($imports, $decls)"

	override fun toSexpr() = sexpr("Module", sexpr("Imports", imports), sexpr("Decls", decls))
}

class Imports(val loc: Loc, val path: ImportPath, val imported: Arr<LocalDeclare>) : HasSexpr {
	override fun toSexpr() =
		TODO()
}

sealed class ImportPath : HasSexpr {
	class Global(val path: Path) : ImportPath() {
		override fun toSexpr() =
			TODO()
	}

	class Relative(val path: RelPath) : ImportPath() {
		override fun toSexpr() =
			TODO()
	}
}
