import u.Arr
import u.build
import u.makeLoc

import testU.*
import u.*


class Point(val x: Int, val y: Int) : HasSexpr {
	override fun toSexpr() = sexpr("Point") {
		s(1)
		s(2)
	}
}

class Rect(val p1: Point, val p2: Point): HasSexpr {
	override fun toSexpr() = sexpr("Rect") {
		s(p1)
		s(p2)
	}
}

/*
sealed class J {


	class L(val content: Arr<J>) : J() {
		constructor(vararg content: J) : this(Arr.of(*content))
	}
}
*/


fun main(args: Array<String>) {
	/*
	val p = Point(1, 2)
	val r = Rect(p, p)
	val s = sexpr(Arr.of(r, r, r))
	val st = s.toString()
	println(st)
	*/

	val path = Path.of("main.nz")
	val s = TestU.lex(path)
	println(s)
	val ast = TestU.parse(path)
	val ss = ast.toSexpr()
	println(ss)
}
