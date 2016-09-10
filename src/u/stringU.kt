package u

inline fun<T> buildStringFromCharsAndReturn(f: (Action<Char>) -> T): Pair<String, T> {
	val buffer = StringBuffer()
	//TODO: f(buffer::append)
	val returned = f { buffer.append(it) }
	return Pair(buffer.toString(), returned)
}

inline fun buildStringFromChars(f: Action<Action<Char>>): String {
	val buffer = StringBuffer()
	f { buffer.append(it) }
	return buffer.toString()
}
