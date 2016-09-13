package u

typealias Loc = Long
typealias Pos = Int

fun makeLoc(a: Pos, b: Pos): Loc {
	return (a.toLong() shl 32) or b.toLong()
}

fun singleCharLoc(a: Pos): Loc =
	makeLoc(a, a + 1)

fun Loc.start(): Pos =
	(this ushr 32).toInt()

fun Loc.end(): Pos =
	this.toInt()

fun Pos.incr(): Pos =
	this + 1

val startPos: Pos = 0
val nilLoc: Loc = -1

class LcPos(val line: Int, val column: Int)
class LcLoc(val start: LcPos, val end: LcPos) {
	companion object {
		fun from(source: Input, loc: Loc): LcLoc {
			val start = walkTo(source, startLc, loc.start())
			val end = walkTo(source, start, loc.end() - loc.start())
			return LcLoc(start, end)
		}
	}
}

private fun walkTo(source: Input, startPos: LcPos, distanceToWalk: Int): LcPos {
	var line = startPos.line
	var column = startPos.column
	repeat(distanceToWalk) {
		when (source.readChar()) {
			'\n' -> {
				line++
				column = 1
			}
			else ->
				column++
		}
	}
	return LcPos(line, column)
}

private val startLc = LcPos(1, 1)
