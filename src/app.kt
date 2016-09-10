import u.Arr
import u.build
import u.makeLoc

fun<T> f(a: Arr<T>, t: T) =
	a[0] == t

fun main(args: Array<String>) {
	println("Hello \u0000 world!")

	val a = build<Int> { add ->
		add(1)
		add(2)
		add(3)
	}
	println(a)
}

val l = makeLoc(12, 34)

/*
sealed class E {
	data class LocalDeclare(val loc: Loc, val name: u.Sym, val ty: Ty) : E()
	sealed class Pattern {
		data class Ignore(val loc: Loc)
		data class Single(val declare: LocalDeclare)
		data class Destruct(val loc: Loc, val patterns: u.Arr<Pattern>)
	}
}
*/
