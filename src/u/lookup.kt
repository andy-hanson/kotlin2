package u

import java.util.HashMap

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

fun<K, V> HashMap<K, V>.tryAdd(key: K, value: V): V? =
	returning(this[key]) { alreadyPresent ->
		if (alreadyPresent == null)
			this[key] = value
	}


/** Immutable hash map. */
class Lookup<K, V> private constructor(private val data: HashMap<K, V>) {
	companion object {
		fun<K, V> fromValues(values: Arr<V>, getKey: (V) -> K) =
			Lookup(Hm.buildFromValues(values, getKey))
	}

	operator fun get(key: K): V? =
		data[key]

	fun has(key: K): Bool =
		data.containsKey(key)
}
