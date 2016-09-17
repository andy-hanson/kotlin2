package compile

import compile.err.*
import u.*
import n.*

internal data class TyAndAst(val ast: ast.DeclTy, val ty: TyOrGen)
internal data class FnAndAst(val ast: ast.Fn, val fn: Fn.Declared)
internal fun writeNilDeclarations(module: Module, decls: Arr<ast.Decl>): Pair<Arr<TyAndAst>, Arr<FnAndAst>> =
	build2 { addTy, addFn ->
		module.members = Lookup.buildWithSize(decls.size) { addMember ->
			for (decl in decls) {
				fun build(name: Sym, member: ModuleMember) {
					if (addMember(name, member) != null)
						raise<Unit>(decl.loc, Err.NameAlreadyBound(name))
				}
				when (decl) {
					is ast.DeclVal -> {
						val (name, member) = createNilVal(module, addFn, decl)
						build(name, member)
					}
					is ast.DeclTy -> {
						val ty = nilOfAst(module, decl)
						addTy(TyAndAst(decl, ty))
						build(decl.name, ModuleMember.Ty(ty))
					}
				}
			}
		}
	}

//TODO: don't pass in buildFn
private fun createNilVal(module: Module, buildFn: (FnAndAst) -> Unit, ast: ast.DeclVal): Pair<Sym, ModuleMember> =
	when (ast) {
		is ast.Fn -> {
			val fn =
				when (ast.head) {
					is ast.Fn.Head.Plain -> {
						val name = ast.head.name
						val ty = Ft()
						val fn = Fn.Declared(CodeOrigin(module, ast.loc, name), FtOrGen.F(ty))
						ty.origin = Ft.Origin.FromDeclaredFn(fn)
						fn
					}
					is ast.Fn.Head.Generic -> {
						val (name, genParams) = ast.head
						val ty = GenFt(createGenStuffFromParamAsts(module, genParams))
						val fn = Fn.Declared(CodeOrigin(module, ast.loc, name), FtOrGen.G(ty))
						ty.origin = GenFt.Origin.FromDeclaredFn(fn)
						fn
					}
				}
			buildFn(FnAndAst(ast, fn))
			Pair(ast.name, typedVFromFn(fn))
		}
	}
