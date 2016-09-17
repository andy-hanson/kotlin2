package u

import java.util.HashMap

//TODO: move to its own module
object Hm {
	fun<K, V> createWithSize(size: Int) =
		HashMap<K, V>(size)

	fun<K, V> buildWithSize(size: Int, builder: Action<(K, V) -> V?>): HashMap<K, V> =
		returning (createWithSize(size)) { map ->
			//TODO:revokable
			val tryAdd = { k: K, v: V -> map.tryAdd(k, v) } //TODO:map::tryAdd
			builder(tryAdd)
			//TODO:revoke
		}

	fun<A, K, V> buildFromArr(arr: Arr<A>, getPair: (Int, A) -> Pair<K, V>): HashMap<K, V> =
		buildWithSize(arr.size) { add ->
			for ((index, element) in arr.withIndex()) {
				val (k, v) = getPair(index, element)
				val alreadyPresent = add(k, v)
				if (alreadyPresent != null)
					throw Exception("Key already in map: $k")
			}
		}

	fun<K, V> buildFromKeywsWithIndex(keys: Arr<K>, getValue: (Int, K) -> V): HashMap<K, V> =
		buildFromArr(keys) { i, key ->
			key to getValue(i, key)
		}

	fun<K, V> buildFromValues(values: Arr<V>, getKey: (V) -> K): HashMap<K, V> =
		buildFromArr(values) { key, value ->
			getKey(value) to value
		}
}

fun<K, V> HashMap<K, V>.addOr(key: K, value: V, f: Action<V>) {
	val old = tryAdd(key, value)
	if (old != null)
		f(old)
}

fun<K, V> HashMap<K, V>.tryAdd(key: K, value: V): V? =
	returning(this[key]) { alreadyPresent ->
		if (alreadyPresent == null)
			this[key] = value
	}

fun<K, V> HashMap<K, V>.surelyRemove(key: K) {
	require(containsKey(key))
	remove(key)
}


/** Immutable hash map. */
class Lookup<K, V> private constructor(private val data: HashMap<K, V>) : Iterable<Pair<K, V>> {
	companion object {
		fun<K, V> empty(): Lookup<K, V> =
			Lookup(HashMap())

		fun<K, V> buildWithSize(size: Int, builder: Action<(K, V) -> V?>) =
			Lookup(Hm.buildWithSize(size, builder))

		fun<K, V> build(builder: Action<(K, V) -> V?>) =
			buildWithSize(0, builder)

		fun<K1, V1, K2, V2> build2(builder: ((K1, V1) -> V1?, (K2, V2) -> V2?) -> Unit): Pair<Lookup<K1, V1>, Lookup<K2, V2>> {
			var b: Lookup<K2, V2>? = null
			val a = build<K1, V1> { tryAdd1 ->
				b = build { tryAdd2 ->
					builder(tryAdd1, tryAdd2)
				}
			}
			return Pair(a, b!!)
		}

		fun<K, V> fromValues(values: Arr<V>, getKey: (V) -> K) =
			Lookup(Hm.buildFromValues(values, getKey))
	}

	operator fun get(key: K): V? =
		data[key]

	fun has(key: K): Bool =
		data.containsKey(key)

	fun keys(): Arr<K> =
		TODO()

	override fun iterator(): Iterator<Pair<K, V>> =
		object : Iterator<Pair<K, V>> {
			val iter = data.iterator()
			override fun hasNext() =
				iter.hasNext()
			override fun next(): Pair<K, V> {
				val (k, v) = iter.next()
				return Pair(k, v)
			}
		}
}
