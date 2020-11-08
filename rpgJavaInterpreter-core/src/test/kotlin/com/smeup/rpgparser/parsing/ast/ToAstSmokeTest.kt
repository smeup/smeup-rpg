package com.smeup.rpgparser.parsing.ast

import com.smeup.rpgparser.assertASTCanBeProduced
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToAstSmokeTest {

    @Test
    fun buildAstForJD_001() {
        val cu = assertASTCanBeProduced("JD_001")
        assertEquals(10, cu.dataDefinitions.size)
        assertEquals(4, cu.main.stmts.size)
        assertEquals(7, cu.subroutines.size)
    }

    @Test
    fun buildAstForJD_002() {
        val cu = assertASTCanBeProduced("JD_002")
        assertEquals(18, cu.dataDefinitions.size)
        assertEquals(4, cu.main.stmts.size)
        assertEquals(10, cu.subroutines.size)
    }

    @Test
    fun buildAstForJD_003() {
        val cu = assertASTCanBeProduced("JD_003")
        assertEquals(16, cu.dataDefinitions.size)
        assertEquals(4, cu.main.stmts.size)
        assertEquals(6, cu.subroutines.size)
    }

    @Test
    fun buildAstForJCODFISS() {
        val cu = assertASTCanBeProduced("JCODFISS")
        assertEquals(0, cu.dataDefinitions.size)
        assertEquals(2, cu.main.stmts.size)
        assertEquals(0, cu.subroutines.size)
    }

    @Test
    fun buildAstForJD_001_dataDefinitions() {
        val root = assertASTCanBeProduced("JD_001")
        assertEquals(10, root.dataDefinitions.size)
        assertEquals("@UNNAMED_DS_16", root.dataDefinitions[0].name)
        assertEquals("U\$FUNZ", root.dataDefinitions[1].name)
        assertEquals("U\$METO", root.dataDefinitions[2].name)
        assertEquals("U\$SVARSK", root.dataDefinitions[3].name)
        assertEquals("U\$IN35", root.dataDefinitions[4].name)
        assertEquals("\$\$URL", root.dataDefinitions[5].name)
        assertEquals("\$X", root.dataDefinitions[6].name)
        assertEquals("U\$SVARSK_INI", root.dataDefinitions[7].name)
        assertEquals("§§FUNZ", root.dataDefinitions[8].name)
        assertEquals("§§METO", root.dataDefinitions[9].name)
    }

    @Test
    fun buildAstForJD_001_subroutines() {
        val root = assertASTCanBeProduced("JD_001")
        assertEquals(7, root.subroutines.size)
        assertEquals("£INIZI", root.subroutines[0].name)
        assertEquals(1, root.subroutines[0].stmts.size)
        assertEquals("IMP0", root.subroutines[1].name)
        assertEquals(1, root.subroutines[1].stmts.size)
        assertEquals("FIN0", root.subroutines[2].name)
        assertEquals(0, root.subroutines[2].stmts.size)
        assertEquals("FINZ", root.subroutines[3].name)
        assertEquals(2, root.subroutines[3].stmts.size)
        assertEquals("FESE", root.subroutines[4].name)
        assertEquals(3, root.subroutines[4].stmts.size)
        assertEquals("REPVAR", root.subroutines[5].name)
        assertEquals(1, root.subroutines[5].stmts.size)
        assertEquals("FCLO", root.subroutines[6].name)
        assertEquals(0, root.subroutines[6].stmts.size)
    }

    @Test
    fun buildAstForHELLO() {
        val cu = assertASTCanBeProduced("HELLO")
        assertEquals(1, cu.dataDefinitions.size)
        assertEquals(3, cu.main.stmts.size)
        assertEquals(0, cu.subroutines.size)
    }

    @Test
    fun buildAstForCALCFIBCAL() {
        val cu = assertASTCanBeProduced("CALCFIBCAL")
        assertEquals(1, cu.dataDefinitions.size)
        assertEquals(3, cu.main.stmts.size)
        assertEquals(0, cu.subroutines.size)
    }

    @Test
    fun buildAstForCALCFIBCA2() {
        val cu = assertASTCanBeProduced("CALCFIBCA2")
        assertEquals(0, cu.dataDefinitions.size)
        assertEquals(3, cu.main.stmts.size)
        assertEquals(0, cu.subroutines.size)
    }

    @Test
    fun buildAstForCALCFIBCA3() {
        val cu = assertASTCanBeProduced("CALCFIBCA3")
        assertEquals(0, cu.dataDefinitions.size)
        assertEquals(3, cu.main.stmts.size)
        assertEquals(0, cu.subroutines.size)
    }

    @Test
    fun buildAstForHELLOTYPE() {
        val cu = assertASTCanBeProduced("HELLOTYPE")
        assertEquals(2, cu.dataDefinitions.size)
        assertEquals(6, cu.main.stmts.size)
        assertEquals(0, cu.subroutines.size)
    }

    @Test
    fun buildAstForHELLOERROR() {
        val cu = assertASTCanBeProduced("HELLOERROR")
        assertEquals(1, cu.dataDefinitions.size)
        assertEquals(5, cu.main.stmts.size)
        assertEquals(0, cu.subroutines.size)
    }

    @Test
    fun buildAstForTIMESTDIFF() {
        val cu = assertASTCanBeProduced("TIMESTDIFF")
        assertEquals(5, cu.dataDefinitions.size)
        assertEquals(6, cu.main.stmts.size)
        assertEquals(1, cu.subroutines.size)
    }

    @Test
    fun buildAstForMUTE10_01() {
        assertASTCanBeProduced("performance/MUTE10_01")
        assertASTCanBeProduced("performance/MUTE10_01A")
        assertASTCanBeProduced("performance/MUTE10_01B")
        assertASTCanBeProduced("performance/MUTE10_01C")
    }

    @Test
    fun buildAstForREADP() {
        assertASTCanBeProduced("db/READP")
    }

    @Test
    fun ast_performance_evaluator() {
        for (i in 1..10) assertASTCanBeProduced("JD_003_full")
    }

    @Test
    fun buildAstForACTGRP_FIX() {
        val cu = assertASTCanBeProduced("ACTGRP_FIX")
        assertEquals(firstActivationGroupDirective(cu).type, NamedActivationGroup("MYACT"))
    }

    @Test
    fun buildAstForACTGRP_NEW() {
        val cu = assertASTCanBeProduced("ACTGRP_NEW")
        assertEquals(firstActivationGroupDirective(cu).type, NewActivationGroup)
    }

    @Test
    fun buildAstForACTGRP_CALLER() {
        val cu = assertASTCanBeProduced("ACTGRP_CLR")
        assertEquals(firstActivationGroupDirective(cu).type, CallerActivationGroup)
    }

    private fun firstActivationGroupDirective(cu: CompilationUnit): ActivationGroupDirective {
        assertTrue(cu.directives.size >= 1)
        val directive = cu.directives[0]
        assertTrue(directive is ActivationGroupDirective)
        return directive
    }
}
