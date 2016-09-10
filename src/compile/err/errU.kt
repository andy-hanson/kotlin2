package compile.err

import u.*

fun<T> raise(loc: Loc, kind: Err): T =
	raiseWithPath(emptyPath, loc, kind)

fun<T> raiseWithPath(path: Path, loc: Loc, kind: Err): T =
	throw CompileError(path, loc, kind)

fun must(cond: Bool, loc: Loc, kind: Err): Unit {
	if (!cond)
		raise<Unit>(loc, kind)
}
