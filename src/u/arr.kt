package u

import java.util.Arrays

/**
Immutable array.
Do *not* call the constructor!
 */
class Arr<out T> constructor(private val data: Array<out T>) : Iterable<T> {
	companion object {
		inline fun<reified T> from(c: Collection<T>) = Arr(c.toTypedArray())
		inline fun<reified T> of(vararg elements: T) = Arr(elements)

		inline fun<reified T> empty(): Arr<T> = Arr(arrayOf())

		inline fun<reified T> init(size: Int, noinline initialize: (Int) -> T) =
			Arr(Array<T>(size, initialize))

		inline fun<reified T, reified U> fromMapped(c: List<T>, crossinline map: (T) -> U): Arr<U> =
			init(c.size) { i ->
				map(c[i])
			}

		inline fun<reified T, reified U> fromMappedArray(a: Array<out T>, crossinline map: (T) -> U): Arr<U> =
			fromMapped(a.asList(), map)

		inline fun<reified T> tail(l: MutableList<T>): Arr<T> =
			init(l.size - 1) { i ->
				l[i + 1]
			}

		inline fun<reified T> nil(): Arr<T> = empty()

		inline fun<reified T> concat(a: Arr<T>, b: Arr<T>) =
			Arr.init(a.size + b.size) { i ->
				if (i < a.size)
					a[i]
				else
					b[i - a.size]
			}

		inline fun<reified T> slice(arr: Arr<T>, start: Int, end: Int): Arr<T> {
			require(start >= 0)
			require(start <= end)
			require(end <= arr.size)
			return Arr.init(end - start) { i ->
				arr[start + i]
			}
		}

		inline fun<reified T> cons(first: T, arr: Arr<T>): Arr<T> =
			Arr.init(arr.size + 1) { i ->
				if (i == 0)
					first
				else
					arr[i - 1]
			}

		inline fun<reified T> rcons(arr: Arr<T>, last: T): Arr<T> =
			Arr.init(arr.size + 1) { i ->
				if (i < arr.size)
					arr[i]
				else
					last
			}

		inline fun<reified T> rtail(arr: Arr<T>): Arr<T> =
			Arr.init(arr.size - 1) { i ->
				arr[i]
			}
	}

	operator fun get(idx: Int): T =
		data[idx]

	override fun toString() =
		"[${joinToString(" ")}]"

	override fun equals(other: Any?): Bool =
		other is Arr<*> && other.size == size && Arrays.equals(data, other.data)

	override fun hashCode(): Int =
		Arrays.hashCode(data)

	val size: Int
		get() = data.size

	val isEmpty: Bool
		get() = size == 0

	override fun iterator(): Iterator<T> =
		data.iterator()

	val first: T
		get() = this[0]

	val last: T
		get() = this[size - 1]

	inline fun<reified U> map(crossinline f: (T) -> U): Arr<U> =
		Arr.init<U>(size) { i ->
			f(this[i])
		}
}

//TODO: ArrBuild.kt
inline fun<reified T> build(f: ArrayBuilder<T>.() -> Unit): Arr<T> {
	val l = mutableListOf<T>()
	ArrayBuilder(l).f()
	return Arr.from(l)
}

class ArrayBuilder<T>(val l: MutableList<T>) {
	fun add(t: T) {
		l.add(t)
	}

	fun addMany(many: Iterable<T>) {
		for (element in many)
			add(element)
	}
}


inline fun<reified Element, Result> buildAndReturn(f: (Action<Element>) -> Result): Pair<Arr<Element>, Result> {
	val l = mutableListOf<Element>()
	val res = f { l.add(it) }
	return Pair(Arr.from(l), res)
}

inline fun<reified T> buildLoop(f: Thunk<Pair<T, Bool>>): Arr<T> =
	build<T> {
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

inline fun<reified T> ArrayBuilder<T>.buildUntilNullWorker(f: Thunk<T?>): Unit {
	while (true) {
		val x = f()
		when (x) {
			null -> return
			else -> add(x)
		}
	}
}

inline fun<reified T> buildUntilNull(f: Thunk<T?>): Arr<T> =
	build {
		buildUntilNullWorker(f)
	}

inline fun<reified T> buildUntilNullWithFirst(first: T, f: Thunk<T?>): Arr<T> =
	build {
		add(first)
		buildUntilNullWorker(f)
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
