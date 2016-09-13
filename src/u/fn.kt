package u

inline fun<T> returning(value: T, fn: (T) -> Unit): T {
	fn(value)
	return value
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
