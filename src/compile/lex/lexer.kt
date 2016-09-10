package compile.lex

import u.*
import compile.err.*
import ast.LiteralValue

fun safeRead(source: Input): Char =
	try {
		source.readChar()
	} catch (_: EndOfFile) {
		'\u0000'
	}

class Lexer(private val source: Input) {
	private var peek: Char = safeRead(source)
	private var pos: Pos = startPos
	private var indent: Int = 0
	// Number of Token.Dedent we have to output before continuing to read
	private var dedenting: Int = 0

	init {
		skipNewlines()
	}

	fun curPos(): Pos =
		pos

	fun locFrom(start: Pos): Loc =
		makeLoc(start, pos)

	private fun incrPos(): Unit {
		pos = pos.incr()
	}

	private fun safeRead(): Char =
		safeRead(source)

	private fun readChar(): Char =
		peek.apply {
			peek = safeRead()
			incrPos()
		}

	private fun skip(): Unit {
		readChar()
	}

	private inline fun skipWhile(pred: Pred<Char>): Unit {
		if (pred(peek)) {
			peek = try {
				var ch: Char
				do {
					ch = source.readChar()
					incrPos()
				} while (pred(ch));
				ch
			} catch (_: EndOfFile) {
				'\u0000'
			}
		}
	}

	private inline fun bufferWhile(addChar: Action<Char>, pred: Pred<Char>): Unit {
		if (pred(peek)) {
			addChar(peek);
			// Returns the first char that's not skipped.
			peek =
				try {
					var ch: Char
					while (true) {
						ch = source.readChar()
						incrPos()
						if (!pred(ch))
							break
						addChar(ch)
					}
					ch
				}
				catch (_: EndOfFile) {
					'\u0000'
				}
		}
	}

	private fun skipNewlines(): Unit =
		skipWhile { it == '\n' }

	data class QuotePart(val text: String, val isEndOfQuote: Bool)
	fun nextQuotePart(): QuotePart {
		val (text, isEnd) = buildStringFromCharsAndReturn { addChar ->
			var isEnd: Bool
			outer@ while (true) {
				val ch = readChar()
				when (ch) {
					'"' -> {
						isEnd = true
						break@outer
					}
					'{' -> {
						isEnd = false
						break@outer
					}
					'\n' ->
						TODO("Compile error: unterminated quote")
					'\\' ->
						addChar(escape(readChar()))
					else ->
						addChar(ch)
				}
			}
			isEnd
		}
		return QuotePart(text, isEnd)
	}

	private fun takeNumber(negate: Bool, fst: Char): Token {
		val (str, isFloat) = buildStringFromCharsAndReturn { addChar ->
			addChar(fst)
			bufferWhile(addChar, ::isDigit)
			returning (peek == '.') { isFloat ->
				if (isFloat) {
					skip()
					addChar('.')
					must(isDigit(peek), singleCharLoc(pos), Err.TooMuchIndent)
					bufferWhile(addChar, ::isDigit)
				}
			}
		}
		val value =
			if (isFloat) {
				val f = str.toDouble()
				ast.LiteralValue.Float(if (negate) -f else f)
			}
			else {
				val i = str.toLong()
				ast.LiteralValue.Int(if (negate) -i else i)
			}
		return Token.Literal(value)
	}

	private inline fun takeSymbol(first: Char, pred: Pred<Char>, makeToken: (Sym) -> Token): Token {
		val text = buildStringFromChars { addChar ->
			addChar(first)
			bufferWhile(addChar, pred)
		}
		val name = Sym.ofString(text)
		return Token.opKeyword(name) ?: makeToken(name)
	}

	private fun takeOperator(ch: Char): Token =
		takeSymbol(ch, ::isOperatorChar) { Token.Operator(it) }

	private inline fun countWhile(pred: Pred<Char>): Int {
		var count = 0
		while (pred(peek)) {
			skip()
			count++
		}
		return count
	}

	private fun lexIndent(): Int {
		val start = pos
		return returning(countWhile { it == '\t' }) {
			must(peek != ' ', locFrom(start), Err.LeadingSpace)
		}
	}

	private fun handleNewline(indentOnly: Bool): Token {
		skipNewlines()
		val oldIndent = indent
		indent = lexIndent()
		return when {
			indent > oldIndent -> {
				must(indent == oldIndent + 1, singleCharLoc(pos), Err.TooMuchIndent)
				Token.Indent
			}
			indent == oldIndent ->
				if (indentOnly)
					nextToken()
				else
					Token.Newline
			else -> {
				dedenting = oldIndent - indent - 1
				Token.Dedent
			}
		}
	}

	fun nextToken(): Token {
		if (dedenting != 0) {
			dedenting = dedenting - 1
			return Token.Dedent
		} else
			return takeNext()
	}

	private fun takeNext(): Token {
		val ch = readChar()
		return when (ch) {
			'\u0000' -> {
				// Remember to dedent before finishing
				if (indent != 0) {
					indent--
					Token.Dedent
				} else
					Token.EOF
			}

			' ' -> {
				must(peek != '\n', singleCharLoc(pos), Err.TrailingSpace)
				takeNext()
			}

			'\n' ->
				handleNewline(false)

			'|' -> {
				skipWhile { it != '\n' }
				handleNewline(true)
			}

			'\\' -> Token.Backslash
			':' -> Token.Colon
			'(' -> Token.Lparen
			')' -> Token.Rparen
			'[' -> Token.Lbracket
			']' -> Token.Rbracket
			'{' -> Token.LCurly
			'}' -> Token.RCurly
			'_' -> Token.Underscore

			'-' -> {
				val next = readChar()
				if (isDigit(next))
					takeNumber(true, next)
				else
					takeOperator(ch)
			}

			'.' ->
				when (peek) {
					'.' -> {
						skip()
						Token.DotDot
					}
					else ->
						Token.Dot
				}

			'"' -> {
				val (str, isDone) = nextQuotePart()
				if (isDone)
					Token.Literal(LiteralValue.Str(str))
				else
					Token.QuoteStart(str)
			}

			in '0' .. '9' ->
				takeNumber(false, ch)
			in 'a' .. 'z' ->
				takeSymbol(ch, ::isNameChar) { Token.Name(it) }
			in 'A' .. 'Z' ->
				takeSymbol(ch, ::isNameChar) { Token.TyName(it) }
			'@', '+', '*', '/', '^', '?', '<', '>', '=' ->
				takeOperator(ch)

			else ->
				raise(singleCharLoc(pos), Err.UnrecognizedCharacter(ch))
		}
	}

	data class PosNext(val pos: Pos, val next: Token)
	fun posNext(): PosNext {
		val p = pos
		return PosNext(p, nextToken())
	}
}

fun escape(escaped: Char) =
	when (escaped) {
		'"', '{' -> escaped
		'n' -> '\n'
		't' -> '\t'
		else -> TODO("Compile error: bad escape")
	}

