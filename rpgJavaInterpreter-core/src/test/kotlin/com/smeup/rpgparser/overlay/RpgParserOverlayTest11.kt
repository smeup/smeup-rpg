package com.smeup.rpgparser.overlay

import com.smeup.rpgparser.AbstractTest
import com.smeup.rpgparser.assertCanBeParsed
import com.smeup.rpgparser.executeAnnotations
import com.smeup.rpgparser.interpreter.InternalInterpreter
import com.smeup.rpgparser.interpreter.NumberType
import com.smeup.rpgparser.interpreter.SystemInterface
import com.smeup.rpgparser.jvminterop.JavaSystemInterface
import com.smeup.rpgparser.parsing.parsetreetoast.resolveAndValidate
import com.smeup.rpgparser.rpginterop.DirRpgProgramFinder
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

open class RpgParserOverlayTest11 : AbstractTest() {

    private fun createSystemInterface(): SystemInterface {
        return JavaSystemInterface().apply {
            rpgSystem.addProgramFinder(DirRpgProgramFinder(File("src/test/resources/overlay")))
        }
    }

    @Test
    fun parseMUTE11_11C_syntax() {
        assertCanBeParsed("overlay/MUTE11_11C", withMuteSupport = true)
    }

    @Test
    fun parseMUTE11_11C_ast() {
        assertASTCanBeProduced("overlay/MUTE11_11C", considerPosition = true, withMuteSupport = true)
    }

    @Test
    fun parseMUTE11_11C_runtime() {
        val cu = assertASTCanBeProduced("overlay/MUTE11_11C", considerPosition = true, withMuteSupport = true)
        cu.resolveAndValidate()

        val interpreter = InternalInterpreter(createSystemInterface())
        interpreter.execute(cu, mapOf())
        val annotations = interpreter.systemInterface.getExecutedAnnotation().toSortedMap()
        var failed: Int = executeAnnotations(annotations)
        if (failed > 0) {
            throw AssertionError("$failed/${annotations.size} failed annotation(s) ")
        }
    }

    @Test
    fun parseMUTE11_15_syntax() {
        assertCanBeParsed("overlay/MUTE11_15", withMuteSupport = true)
    }

    @Test
    fun parseMUTE11_15_ast() {
        val cu = assertASTCanBeProduced("overlay/MUTE11_15", considerPosition = true, withMuteSupport = true)
        cu.resolveAndValidate()

        val FUND1 = cu.getDataDefinition("£FUND1")
        val FUNQT = FUND1.getFieldByName("£FUNQT")
        assertEquals(Pair(442, 457), FUNQT.offsets)
        assertEquals(NumberType(entireDigits = 10, decimalDigits = 5, rpgType = "S"), FUNQT.type)
        assertEquals(15, FUNQT.size)
    }

    @Test
    fun parseMUTE11_15_runtime() {
        val cu = assertASTCanBeProduced("overlay/MUTE11_15", considerPosition = true, withMuteSupport = true)
        cu.resolveAndValidate()

        val interpreter = InternalInterpreter(createSystemInterface())
        interpreter.execute(cu, mapOf())
        val annotations = interpreter.systemInterface.getExecutedAnnotation().toSortedMap()
        var failed: Int = executeAnnotations(annotations)
        if (failed > 0) {
            throw AssertionError("$failed/${annotations.size} failed annotation(s) ")
        }
    }

    @Test
    fun parseMUTE11_16_syntax() {
        assertCanBeParsed("overlay/MUTE11O16", withMuteSupport = true)
    }

    @Test
    fun parseMUTE11_16_ast() {
        assertASTCanBeProduced("overlay/MUTE11O16", considerPosition = true, withMuteSupport = true)
    }

    @Test
    fun parseMUTE11_16_runtime() {
        val cu = assertASTCanBeProduced("overlay/MUTE11O16", considerPosition = true, withMuteSupport = true)
        cu.resolveAndValidate()

        val interpreter = InternalInterpreter(createSystemInterface())
        interpreter.execute(cu, mapOf())
        val annotations = interpreter.systemInterface.getExecutedAnnotation().toSortedMap()
        var failed: Int = executeAnnotations(annotations)
        if (failed > 0) {
            throw AssertionError("$failed/${annotations.size} failed annotation(s) ")
        }
    }
}
