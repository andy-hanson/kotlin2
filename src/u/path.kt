package u

typealias Path = Arr<Sym>

// # of parents, then a path relative to the ancestor
data class RelPath(val nParents: Int, val relToParent: Path)

val emptyPath = Arr.empty<Sym>()

