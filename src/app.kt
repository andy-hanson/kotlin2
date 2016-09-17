import testU.*
import u.*

fun main(args: Array<String>) {
	val path = Path.of("main.nz")
	val s = TestU.lex(path)
	println(s)
	val ast = TestU.parse(path)
	val ss = ast.toSexpr()
	println(ss)
}
