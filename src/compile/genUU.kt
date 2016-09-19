package compile

import u.*
import n.*

//TODO: this file, man, this file!

//TODO:RENAME
internal fun createGenStuffFromParamAsts(module: Module, params: Arr<ast.TyParam>): Arr<GenVar> =
	params.map { GenVar.Declared(CodeOrigin(module, it.loc, it.name)) }
