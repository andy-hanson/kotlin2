package u

class Path(val parts: Arr<Sym>) {
	companion object {
		val empty = Path(Arr.empty())
		fun resolveWithRoot(root: Path, path: Path): Path =
			root.resolve(RelPath(0, path))

		fun of(vararg elements: String): Path =
			Path(Arr.fromMappedArray<String, Sym>(elements) { Sym.ofString(it) }) //TODO: Sym::ofString
	}

	override fun toString() =
		parts.joinToString("/")

	fun resolve(rel: RelPath): Path {
		val (nParents, relToParent) = rel
		val nPartsToKeep = parts.size - nParents
		if (nPartsToKeep < 0)
			throw Exception("Can't resolve: $rel\nRelative to: $this")
		val parent = Arr.slice(parts, 0, nPartsToKeep)
		return Path(Arr.concat(parent, relToParent.parts))
	}

	fun add(next: Sym): Path =
		Path(Arr.rcons(parts, next))

	fun parent(): Path =
		Path(Arr.rtail(parts))

	val last: Sym
		get() = parts.last

	fun addExtension(extension: String): Path =
		parent().add(last.mod { it + extension })

	val isEmpty: Bool
		get() = parts.isEmpty

	override fun equals(other: Any?) =
		other is Path && parts.equals(other.parts)

	override fun hashCode() =
		parts.hashCode()
}

// # of parents, then a path relative to the ancestor
data class RelPath(val nParents: Int, val relToParent: Path) {
	override fun toString(): String {
		val start =
			when (nParents) {
				0 -> "/"
				1 -> "./"
				else -> "../".repeat(nParents - 1)
			}
		return start + relToParent.toString()
	}

	val isParentsOnly: Bool
		get() = relToParent.isEmpty
}
