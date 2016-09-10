package u

inline fun<T> returning(value: T, fn: (T) -> Unit): T {
	fn(value)
	return value
}
