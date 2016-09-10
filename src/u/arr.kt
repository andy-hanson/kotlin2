package u

/**
Immutable array.
Do *not* call the constructor!
 */
class Arr<out T> constructor(private val data: Array<out T>) {
	companion object {
		inline fun<reified T> from(c: Collection<T>) = Arr(c.toTypedArray())
		fun<T> of(vararg elements: T) = Arr(elements)

		inline fun<reified T> empty(): Arr<T> = Arr(arrayOf())
	}

	operator fun get(idx: Int) =
		data[idx]

	override fun toString() =
		//TODO:BETTER
		data.toString()

	val size: Int
		get() = data.size

	val isEmpty: Bool
		get() = size == 0
}

//TODO: ArrBuild.kt
inline fun<reified T> build(f: Action<Action<T>>): Arr<T> {
	val l = mutableListOf<T>()
	f { l.add(it) } //TODO: l::add ?
	return Arr.from(l)
}

inline fun<reified Element, Result> buildAndReturn(f: (Action<Element>) -> Result): Pair<Arr<Element>, Result> {
	val l = mutableListOf<Element>()
	val res = f { l.add(it) }
	return Pair(Arr.from(l), res)
}

inline fun<reified T> buildLoop(f: Thunk<Pair<T, Bool>>): Arr<T> =
	build<T> { add ->
		while (true) {
			val (element, shouldContinue) = f()
			add(element)
			if (!shouldContinue)
				break
		}
	}

sealed class Builder<Element, Result> {
	class Continue<Element, Result>(val element: Element) : Builder<Element, Result>()
	class Done<Element, Result>(val result: Result) : Builder<Element, Result>()
}

inline fun<reified Element, Result> buildLoop0WithFirst(first: Element, f: Thunk<Builder<Element, Result>>): Pair<Arr<Element>, Result> =
	buildAndReturn { add ->
		add(first)
		var res: Result
		loop@ while (true) {
			val x = f()
			when (x) {
				is Builder.Continue -> {
					add(x.element)
				}
				is Builder.Done -> {
					res = x.result
					break@loop
				}
			}
		}
		res
	}

inline fun<reified T> buildUntilNullWorker(f: Thunk<T?>, add: Action<T>): Unit {
	while (true) {
		val x = f()
		when (x) {
			null -> return
			else -> add(x)
		}
	}
}

inline fun<reified T> buildUntilNull(f: Thunk<T?>): Arr<T> =
	build { add ->
		buildUntilNullWorker(f, add)
	}

//TODO:MOVE
inline fun<T> loopUntilResult(f: Thunk<T?>): T {
	var res: T
	loop@while (true) {
		val resOrNull = f()
		when (resOrNull) {
			null -> {}
			else -> {
				res = resOrNull
				break@loop
			}
		}
	}
	return res
}
