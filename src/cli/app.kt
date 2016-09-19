package cli

import testU.*
import u.*
import n.*

import org.objectweb.asm.*
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureWriter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.lang.reflect.*

//import kotlin.reflect.*
//import kotlin.reflect.jvm.*

//TODO: clean up this file
//Then TODO: builtins
//A builtin fn will need to carry around the java.lang.Class and java.lang.reflect.Method.
//Then we write its code using `callStatic`


@Target(AnnotationTarget.FUNCTION)
annotation class Pub(val name: String)

@Pub("foo-foo") fun foo(p: Pub, a: Annotation) {
	if (a is Pub)
		println("???")
}

fun main(args: Array<String>) {
	//println(::foo.annotations)
	//val x = ::foo.reflect()

	//val x = asmTest()
	//printClass(x)
	//runMain(x)

	assert(1 == 2)
	println("Hello")
}

fun findPubs(klass: Class<*>) {
	/*
	for (member in klass.members) {
		getPub(member) opMap { pub ->
			when (member) {
				is KFunction<*> -> {
					val m = member.javaMethod
					m.
				}
				else -> TODO()
			}
		}
	}
	*/

	for (method in klass.methods) {
		handleMethod(method)
	}

	/*for (member in klass.members) {
		when (member) {
			is KFunction<*> ->
				handleMethod(member.javaMethod!!)
			else -> TODO()
		}
	}*/
}

fun handleMethod(m: Method) {
	checkModifiers(m.modifiers)
	m.getDeclaringClass()
}

fun checkModifiers(modifiers: Int) {
	assert(Modifier.isFinal(modifiers))
	val no = listOf(Modifier.ABSTRACT, Modifier.INTERFACE, Modifier.NATIVE, Modifier.PRIVATE, Modifier.PROTECTED, Modifier.SYNCHRONIZED, Modifier.TRANSIENT, Modifier.VOLATILE)
	for (flag in no)
		forbid(modifiers hasFlag flag)
	assert(Modifier.isPublic(modifiers))
	assert(Modifier.isStatic(modifiers)) // TODO: allow instance methods
}

fun forbid(condition: Bool) {
	assert(!condition)
}

infix fun Int.hasFlag(flag: Int): Bool =
	(this and flag) != 0

//fun getPub(f: KCallable<*>): Pub? =
//	f.annotations.findInstanceOf<Annotation, Pub>()

inline fun<T, reified U : T> Iterable<T>.findInstanceOf(): U? =
	find { it is U } as? U



fun runMain(bytes: ByteArray) {
	val loader = DynamicClassLoader()
	val klass = loader.define("hello.HelloWorld", bytes)
	val method = klass.getMethod("foo", MyClass::class.java)
	val result = method.invoke(null, MyClass(1))
	println(result)
}

class DynamicClassLoader : ClassLoader {
	constructor() : super()
	constructor(parent: ClassLoader) : super(parent)

	fun define(className: String, bytecode: ByteArray): Class<out Any> =
		super.defineClass(className, bytecode, 0, bytecode.size)
}


fun printClass(bytes: ByteArray) {
	val reader = ClassReader(bytes)
	val visitor = TraceClassVisitor(PrintWriter(System.out))
	reader.accept(visitor, ClassReader.SKIP_DEBUG)
}





fun asmTest(): ByteArray {
	val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
	cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, "hello/HelloWorld", null, "java/lang/Object", null)
	cw.visitSource("HelloWorld.nz", null)

	writeFunc(cw)

	cw.visitEnd()
	return cw.toByteArray()
}

fun writeFunc(cw: ClassWriter) {
	val mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "foo", signatureString(), null, null)

	mv.visitVarInsn(Opcodes.ALOAD, 0)

	mv.callStatic(MyClass::class, MyClass::class.java.getMethod("incr", MyClass::class.java))

	mv.visitInsn(Opcodes.ARETURN)
	mv.visitMaxs(0, 0)
	mv.visitEnd()
}

fun MethodVisitor.callStatic(klass: kotlin.reflect.KClass<*>, called: Method) {
	val j = called

	val a = j.getDeclaringClass()
	val b = j.declaringClass
	assert(a === b)

	val k = j.declaringClass.javaClass
	assert(k === klass.java)
	println(a === b)
	println(k === klass.java)
	val desc = Type.getMethodDescriptor(j)
	visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(k), called.name, desc, /*isInterface*/ false)
}

fun signatureString(): String {
	val w = SignatureWriter()

	w.visitParameterType()
	sigType(w)

	w.visitReturnType()
	sigType(w)


	println(w.toString())
	return w.toString()
}

fun sigType(w: SignatureWriter) {
	w.visitClass(MyClass::class.java) // This also works for interfaces.
}

fun SignatureWriter.visitClass(klass: Class<*>) {
	//visitBaseType('Z')
	visitClassType(Type.getInternalName(klass))
	//visitClassType(klass.java.canonicalName)
	visitEnd()
}

data class MyClass(val i: Int) {
	companion object {
		@JvmStatic
		fun incr(a: MyClass): MyClass =
			MyClass(a.i + 1)
	}
}


fun test() {
	/*
	val path = Path.of("main.nz")
	val s = TestU.lex(path)
	println(s)
	val ast = TestU.parse(path)
	val ss = ast.toSexpr()
	println(ss)
	*/

	val module = TestU.compile(Path("main"))
	println(module.toSexpr())
}
