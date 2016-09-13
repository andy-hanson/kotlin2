package n

import u.*

/** Polymorph */
data class Py(val origin: Origin, val ty: GenFt) {
	sealed class Origin {
		data class Builtin(val name: Sym) : Origin()
	}

	override fun equals(other: Any?) =
		this === other

	override fun hashCode() =
		origin.hashCode()
}

/** Py implementation */
data class Il(
	/** The py being implemented. */
	val py: Py,
	/**
	These are substituted into `py.ty` to get the concrete type.
	The concrete type itself is stored with [fn].
	*/
	val tys: Arr<Ty>,
	/** The function backing this implementation. */
	val fn: Fn
)


