package u

import java.util.WeakHashMap

// Maps from a string to itself
private val table = WeakHashMap<String, Sym>()

class Sym private constructor(val str: String) {
	companion object {
		fun ofString(s: String): Sym {
			val entry = table[s]
			if (entry == null) {
				val sym = Sym(s)
				table[s] = sym
				return sym
			} else
				return entry
		}
	}

	override fun toString() =
		str
}

/*
type t = string

(* Use a weak set of strings that have been interned. *)
module Table = Weak.Make(struct
	include String
	let hash = Hashtbl.hash
u.end)

let u.table = Table.create 1024

let of_string(text: string): t =
	try
		Table.find u.table text
	with Not_found ->
		U.returning text @@ Table.add u.table

let string_of: t -> string = Fn.id

let output: t Out.printer = Out.str

(* Use of interning means that symbols may be compared using (==). *)
let equal: t Eq.t = (==)
let hash: t -> int = Hashtbl.hash

module Lookup: Lookup.S with type key = t = Lookup.Make(struct
	type t = string
	let equal = equal
	let hash = hash
	let output = output
u.end)
*/
