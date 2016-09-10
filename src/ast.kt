package ast

import u.*

data class LocalDeclare(val loc: Loc, val name: Sym)
data class TyParam(val loc: Loc, val name: Sym)

sealed class Ty {
	abstract val loc: Loc

	data class Access(override val loc: Loc, val name: Sym) : Ty()
	data class Inst(override val loc: Loc, val instantiated: Ty, val ty_args: Arr<Ty>) : Ty()
}

sealed class Pattern {
	data class Ignore(val loc: Loc) : Pattern()
	data class Single(val declare: LocalDeclare) : Pattern()
	data class Destruct(val loc: Loc, val destructed: Arr<Pattern>) : Pattern()
}

enum class AtKind {
	Exact,
	Convert
}

data class Parameter(val loc: Loc, val name: Sym, val ty: Ty)
data class Signature(val loc: Loc, val return_ty: Ty, val parameters: Arr<Parameter>)


sealed class Expr {
	abstract val loc: Loc
}

data class At(override val loc: Loc, val kind: AtKind, val ty: Ty, val expr: Expr) : Expr()
data class ExprTy(val ty: Ty) : Expr() {
	override val loc: Loc
		get() = ty.loc

}
data class Access(override val loc: Loc, val name: Sym) : Expr()
data class Call(override val loc: Loc, val target: Expr, val args: Arr<Expr>) : Expr()

data class Cs(override val loc: Loc, val value: Expr, val parts: Arr<CsPart>) : Expr()
//move
data class CsPart(val loc: Loc, val test: CsTest, val result: Expr)
data class CsTest(val loc: Loc, val test_ty: Ty, val pattern: Pattern)

data class Ts(override val loc: Loc, val parts: Arr<TsPart>, val elze: Expr) : Expr()
data class TsPart(val loc: Loc, val test: Expr, val result: Expr)

//TODO: kill, just use a getter functino
data class GetProperty(override val loc: Loc, val target: Expr, val propertyName: Sym) : Expr()
data class Lambda(override val loc: Loc, val signature: Signature, val body: Expr) : Expr()
data class Let(override val loc: Loc, val assigned: Pattern, val value: Expr, val expr: Expr) : Expr()
data class List(override val loc: Loc, val parts: Arr<Expr>) : Expr()

data class Literal(override val loc: Loc, val value: LiteralValue) : Expr()
sealed class LiteralValue {
	data class Int(val value: Long) : LiteralValue()
	data class Float(val value: Double) : LiteralValue()
	data class Str(val value: String) : LiteralValue()
}

data class Seq(override val loc: Loc, val first: Expr, val then: Expr) : Expr()
data class Partial(override val loc: Loc, val target: Expr, val args: Arr<Expr>) : Expr()

// For string with no interpolations, Literal is used instead
//TODO: above is silly, kill LiteralValue.Str and always use a quote
data class Quote(override val loc: Loc, val head: String, val parts: Arr<QuotePart>) : Expr()
data class QuotePart(val interpolated: Expr, val text: String)

data class Check(override val loc: Loc, val checked: Expr) : Expr()

data class GenInst(override val loc: Loc, val instantiatedName: Sym, val args: Arr<Ty>) : Expr()




sealed class FnHead {
	data class Plain(val name: Sym) : FnHead()
	data class Generic(val name: Sym, val params: Arr<TyParam>) : FnHead()
}

sealed class Decl {
	abstract val loc: Loc
}

sealed class DeclVal : Decl()

data class Fn(override val loc: Loc, val head: FnHead, val signature: Signature, val body: Expr) : DeclVal()

sealed class DeclTy : Decl()

data class Rt(override val loc: Loc, val name: Sym, val properties: Arr<Property>) : DeclTy()
data class Property(override val loc: Loc, val name: Sym, val ty: Ty) : DeclTy()
data class GenRt(override val loc: Loc, val name: Sym, val params: Arr<TyParam>, val properties: Arr<Property>) : DeclTy()
data class Vt(override val loc: Loc, val name: Sym, val variants: Arr<Ty>) : DeclTy()
data class GenVt(override val loc: Loc, val name: Sym, val params: Arr<TyParam>, val variants: Arr<Ty>) : DeclTy()
data class Ft(override val loc: Loc, val name: Sym, val signature: Signature) : DeclTy()
data class GenFt(override val loc: Loc, val name: Sym, val params: Arr<TyParam>, val signature: Signature) : DeclTy()

data class Module(val imports: Arr<Imports>, val decls: Arr<Decl>)
data class Imports(val loc: Loc, val path: ImportPath, val imported: Arr<LocalDeclare>)
sealed class ImportPath {
	data class Global(val path: Path) : ImportPath()
	data class Relative(val path: RelPath) : ImportPath()
}
