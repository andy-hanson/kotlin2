package u

typealias Loc = Long
typealias Pos = Int

fun makeLoc(a: Pos, b: Pos): Loc {
	return (a.toLong() shl 32) or b.toLong()
}

fun singleCharLoc(a: Pos): Loc =
	TODO()

fun Loc.start(): Pos =
	(this ushr 32).toInt()

fun Loc.end(): Pos =
	this.toInt()

fun Pos.incr(): Pos =
	this + 1

val startPos = 0
