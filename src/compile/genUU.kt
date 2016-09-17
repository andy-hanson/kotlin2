package compile

import u.*
import n.*

//TODO: this file, man, this file!

internal fun<T> createGenStuffFromParamAsts(module: Module, params: Arr<ast.TyParam>): GenStuff<T> =
	GenStuff(params.map {
		GenVar.Declared(CodeOrigin(module, it.loc, it.name))
	})
