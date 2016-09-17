package u

inline fun<T> returning(value: T, fn: (T) -> Unit): T {
	fn(value)
	return value
}

fun<T> never(): T {
	throw Error("This should never happen")
}

//TODO:MOVE
inline fun<T> loop(fn: Thunk<T?>): T {
	while (true) {
		val res = fn()
		if (res != null)
			return res
	}
}

class Loop2<T, U, Res> private constructor(val a: T?, val b: U?, val res: Res?) {
	companion object {
		fun<T, U, Res> done(res: Res): Loop2<T, U, Res> =
			Loop2(null, null, res)

		fun<T, U, Res> cont(a: T, b: U): Loop2<T, U, Res> =
			Loop2(a, b, null)
	}
}

inline fun<T, U, Res> loop2(a: T, b: U, fn: (T, U) -> Loop2<T, U, Res>): Res {
	var aa = a
	var bb = b
	while (true) {
		val x = fn(aa, bb)
		val res = x.res
		if (res != null)
			return res
		aa = x.a!!
		bb = x.b!!
	}
}

class Loop3<T, U, V, Res> private constructor(val a: T?, val b: U?, val c: V?, val res: Res?) {
	companion object {
		fun<T, U, V, Res> done(res: Res): Loop3<T, U, V, Res> =
			Loop3(null, null, null, res)

		fun<T, U, V, Res> cont(a: T, b: U, c: V): Loop3<T, U, V, Res> =
			Loop3(a, b, c, null)
	}
}

inline fun<T, U, V, Res> loop3(a: T, b: U, c: V, fn: (T, U, V) -> Loop3<T, U, V, Res>): Res {
	var aa = a
	var bb = b
	var cc = c
	while (true) {
		val x = fn(aa, bb, cc)
		val res = x.res
		if (res != null)
			return res
		aa = x.a!!
		bb = x.b!!
		cc = x.c!!
	}
}
