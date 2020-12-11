package com.smeup.rpgparser.interpreter

import com.smeup.rpgparser.AbstractTest
import com.smeup.rpgparser.adaptForTestCase
import com.smeup.rpgparser.execution.Configuration
import com.smeup.rpgparser.execution.JarikoCallback
import com.smeup.rpgparser.execution.getProgram
import com.smeup.rpgparser.jvminterop.JavaSystemInterface
import com.smeup.rpgparser.rpginterop.DirRpgProgramFinder
import com.smeup.rpgparser.rpginterop.RpgProgramFinder
import com.smeup.rpgparser.utils.asBigDecimal
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryStorage : IMemorySliceStorage {

    val storage = mutableMapOf<MemorySliceId, Map<String, Value>>()

    /**
     * Open the storage
     * */
    override fun open() {
    }

    /**
     * Load memory associated to memorySliceId
     * */
    override fun load(memorySliceId: MemorySliceId): Map<String, Value> {
        return storage.getOrDefault(memorySliceId, mutableMapOf())
    }

    /**
     * Notify transaction start. Is called before all memory slices storing
     * */
    override fun beginTrans() {
    }

    /**
     * Store map associated to memory slices
     * */
    override fun store(memorySliceId: MemorySliceId, values: Map<String, Value>) {
        storage[memorySliceId] = values
    }

    /**
     * Called if all memory slices storing process is succesfully completed
     * */
    override fun commitTrans() {
    }

    /**
     * Called in case of failure
     * */
    override fun rollbackTrans() {
    }

    /**
     * Close the storage. If do not has been called commitTrans, it could be needed to implement rollback mechanisms
     * */
    override fun close() {
    }
}

open class SymbolTableStoragingTest : AbstractTest() {

    @Test
    fun execPgmAndEvaluateStorage() {
        execPgmAndEvaluateStorage("1")
    }

    fun execPgmAndEvaluateStorage(varValue: String) {
        // Program RT-closing: test to check SymbolTable preserves variables values
        val myProgram = """
     H ACTGRP('MyAct')
     D X               S             50  
     D Msg             S             12
     C                   EVAL      X = '$varValue'
     C                   EVAL      Msg = X
     C     msg           dsply
     C                   SETON                                          RT
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        commandLineProgram.singleCall(emptyList(), configuration)
        // verify if serialization is ok
        // get variables for memorysliceid
        val variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        require(variables != null)
        assertEquals(
            expected = varValue,
            actual = (variables.get("X") as StringValue).value.trim() ?: error("Not found X")
        )
    }

    @Test
    fun initPgmByStorageAndEvaluateResult() {
        initPgmByStorageAndEvaluateResult("A")
    }

    fun initPgmByStorageAndEvaluateResult(varValue: String) {
        val myProgram = """
     H ACTGRP('MyAct')
     D X               S             50
     D Y               S              1
     D Msg             S             12
     C     *ENTRY        PLIST
     C                   PARM                    X
     C                   EVAL      X = Y + '$varValue'
     C                   EVAL      Msg = X
     C     msg           dsply
     C                   SETON                                          RT
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memorySliceId = MemorySliceId("MyAct".toUpperCase(), programName = myProgram)
        val memoryStorage = MemoryStorage()
        memoryStorage.storage[memorySliceId] = mutableMapOf("Y" to StringValue(value = "$varValue", varying = true))
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        // pass PLIST  in order to achieve in output the current X value and to evaluate it at the end
        val result = commandLineProgram.singleCall(listOf("B"), configuration)
        require(result != null)
        assertEquals("$varValue$varValue", result.parmsList[0].trim())
    }

    @Test
    fun execLRPgmAndEvaluateStorage() {
        execLRPgmAndEvaluateStorage("1")
    }

    fun execLRPgmAndEvaluateStorage(varValue: String) {
        // Program LR-closing: test to check SymbolTable does not preserve variables values
        val myProgram = """
     H ACTGRP('MyAct')
     D X               S             50  
     D Msg             S             12
     C                   EVAL      X = X + '$varValue'
     C                   EVAL      Msg = X
     C     msg           dsply
     C                   SETON                                          LR
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        commandLineProgram.singleCall(emptyList(), configuration)
        val variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        assertEquals(
            expected = null,
            actual = variables,
            "Expected no variables due to 'SETON LR' instruction."
        )
    }

    @Test
    fun execRTPgmTwiceAndPreserveValues() {
        execRTPgmTwiceAndPreserveValues("ABC")
    }

    fun execRTPgmTwiceAndPreserveValues(varValue: String) {
        // Program RT-closing: test to check SymbolTable preserve variables values
        val myProgram = """
     H ACTGRP('MyAct')
     D X               S             50  
     D Msg             S             12
     C                   EVAL      X = %TRIM(X) + '$varValue'
     C                   EVAL      Msg = X
     C     msg           dsply
     C                   SETON                                          RT
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        // First call, instatiate and initialize X variable to value 1
        commandLineProgram.singleCall(emptyList(), configuration)
        var variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        require(variables != null)
        assertEquals(
            expected = varValue.trim(),
            actual = (variables.get("X") as StringValue).value.trim() ?: error("Not found X")
        )

        // Second program execution, X variable must exist with value 1, than it will increase to value 2.
        commandLineProgram.singleCall(emptyList(), configuration)
        variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        require(variables != null)
        assertEquals(
            expected = varValue.trim() + varValue.trim(),
            actual = (variables.get("X") as StringValue).value.trim() ?: error("Not found X")
        )
    }

    @Test
    fun initPreExistingVariablesPgmByStorageAndEvaluateResult() {
        initPreExistingVariablesPgmByStorageAndEvaluateResult("B")
    }

    fun initPreExistingVariablesPgmByStorageAndEvaluateResult(varValue: String) {
        // Program RT-closing: test to check per existing variable will be re-assigned
        val myProgram = """
     H ACTGRP('MyAct')
     D Z               S             50
     D Msg             S             12
     C                   EVAL      Z = '$varValue'
     C                   EVAL      Msg = Z
     C     msg           dsply
     C                   SETON                                          RT
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memorySliceId = MemorySliceId("MyAct".toUpperCase(), programName = myProgram)
        val memoryStorage = MemoryStorage()
        memoryStorage.storage[memorySliceId] = mutableMapOf("Z" to StringValue(value = "$varValue", varying = true))
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        commandLineProgram.singleCall(emptyList(), configuration)
        val variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        require(variables != null)
        assertEquals(
            expected = varValue,
            actual = (variables.get("Z") as StringValue).value.trim() ?: error("Not found Z")
        )
    }

    @Test
    fun sameVariablesButDifferentACTGRP() {
        sameVariablesButDifferentACTGRP("xyz")
    }

    fun sameVariablesButDifferentACTGRP(varValue: String) {
        // Program A called, program B called, both with 'SETON RT', both have same var Z but with different two values.
        // Memory must have separated scopes cause two programs have two different ACTGRP (MyActA and MyActB).
        val myProgramA = """
     H ACTGRP('MyActA')
     D Z               S             50
     D Msg             S             12
     C                   EVAL      Z = '$varValue'+'A'
     C                   EVAL      Msg = Z
     C     msg           dsply
     C                   SETON                                          RT
     """
        val myProgramB = """
     H ACTGRP('MyActB')
     D Z               S             50
     D Msg             S             12
     C                   EVAL      Z = '$varValue'+'B'
     C                   EVAL      Msg = Z
     C     msg           dsply
     C                   SETON                                          RT
     """

        val commandLineProgramA = getProgram(nameOrSource = myProgramA)
        val commandLineProgramB = getProgram(nameOrSource = myProgramB)

        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)

        // First call to program A, instatiate and initialize Z variable to value ending with 'A' character.
        commandLineProgramA.singleCall(emptyList(), configuration)
        var variables = memoryStorage.storage[MemorySliceId("MyActA".toUpperCase(), programName = myProgramA)]
        require(variables != null)
        assertEquals(
            expected = "${varValue}A",
            actual = (variables.get("Z") as StringValue).value.trim() ?: error("Not found Z")
        )

        // First call to program B, instatiate and initialize Z variable to value ending with 'B' character.
        commandLineProgramB.singleCall(emptyList(), configuration)
        variables = memoryStorage.storage[MemorySliceId("MyActB".toUpperCase(), programName = myProgramB)]
        require(variables != null)
        assertEquals(
            expected = "${varValue}B",
            actual = (variables.get("Z") as StringValue).value.trim() ?: error("Not found Z")
        )

        // Z variable on MyActA must have previous value ending with 'A' character.
        variables = memoryStorage.storage[MemorySliceId("MyActA".toUpperCase(), programName = myProgramA)]
        require(variables != null)
        assertEquals(
            expected = "${varValue}A",
            actual = (variables.get("Z") as StringValue).value.trim() ?: error("Not found Z")
        )
    }

    @Test
    fun sameVariablesAndSameACTGRP() {
        sameVariablesAndSameACTGRP("zyx")
    }

    fun sameVariablesAndSameACTGRP(varValue: String) {
        // Program A called, program B called, both with 'SETON RT', both have same var 'Z' but with different values.
        // Memory have separated scopes cause two programs with same ACTGRP (MyActSAME and MyActSAME) have different names.
        val myProgramA = """
     H ACTGRP('MyActSAME')
     D Z               S             50
     D Msg             S             12
     C                   EVAL      Z = '$varValue'+'A'
     C                   EVAL      Msg = %CHAR(Z)
     C     msg           dsply
     C                   SETON                                          RT
     """
        val myProgramB = """
     H ACTGRP('MyActSAME')
     D Z               S             50
     D Msg             S             12
     C                   EVAL      Z = '$varValue'+'B'
     C                   EVAL      Msg = %CHAR(Z)
     C     msg           dsply
     C                   SETON                                          RT
     """

        val commandLineProgramA = getProgram(nameOrSource = myProgramA)
        val commandLineProgramB = getProgram(nameOrSource = myProgramB)

        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)

        // First call to program A, instatiate and initialize Z variable to value ending with 'A' character.
        commandLineProgramA.singleCall(emptyList(), configuration)
        var variables = memoryStorage.storage[MemorySliceId("MyActSAME".toUpperCase(), programName = myProgramA)]
        require(variables != null)
        assertEquals(
            expected = "${varValue}A",
            actual = (variables.get("Z") as StringValue).value.trim() ?: error("Not found Z")
        )

        // First call to program B, instatiate and initialize Z variable to value ending with 'B' character.
        commandLineProgramB.singleCall(emptyList(), configuration)
        variables = memoryStorage.storage[MemorySliceId("MyActSAME".toUpperCase(), programName = myProgramB)]
        require(variables != null)
        assertEquals(
            expected = "${varValue}B",
            actual = (variables.get("Z") as StringValue).value.trim() ?: error("Not found Z")
        )

        // Variable 'Z myProgramA scoped' have not changed his initial values, no interference between two programs
        variables = memoryStorage.storage[MemorySliceId("MyActSAME".toUpperCase(), programName = myProgramA)]
        require(variables != null)
        assertEquals(
            expected = "${varValue}A",
            actual = (variables.get("Z") as StringValue).value.trim() ?: error("Not found Z")
        )
    }

    @Test
    @Ignore
    fun execRTPgmTwiceAndNotPreserveValues() {
        // TODO Handle ACTGRP(*NEW)
        // Program RT-closing: test to check SymbolTable does NOT preserve variables values due to *NEW activation group
//        """
//     H ACTGRP(*NEW)
//     D X               S              1  0
//     D Msg             S             12
//     C                   EVAL      X = X + 1
//     C                   EVAL      Msg = %CHAR(X)
//     C     msg           dsply
//     C                   SETON                                          RT
//     """
    }

    @Test
    @Ignore
    fun overflowIntegerVariable() {
        // Variable 'NUM' is defined as '1' integer digit and '0' decimal digit.
        // Due to its definition, 'NUM' variable can store values from -9 to 9.
        // This test shows how numeric variable declaration doesn't work properly
        // cause the 'NUM' variable can store 9999 value.
        val myProgram = """
     H ACTGRP('MyAct')
     D NUM             S              1  0
     D NEGNUM          S              1  0
     C                   EVAL      NUM = *HIVAL
     C                   EVAL      NEGNUM = -9999
     C     NUM           DSPLY
     C     NEGNUM        DSPLY
     C                   SETON                                          RT
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        commandLineProgram.singleCall(emptyList(), configuration)
        val variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        require(variables != null)
        assertEquals(
            expected = IntValue(9),
            actual = variables["NUM"] ?: error("Not found NUM")
        )
        assertEquals(
                expected = IntValue(-9),
                actual = variables["NEGNUM"] ?: error("Not found NEGNUM")
        )
    }

    @Test
    @Ignore
    fun overflowDecimalVariable() {
        // Variable 'NUM' is defined as '2' integer digit and '1' decimal digit.
        // Due to its definition, 'NUM' variable can store values from -99.9 to 99.9 .
        // This test shows how numeric variable declaration doesn't work properly cause,
        // cause the 'NUM' variable can store 9999 value.
        val myProgram = """
     H ACTGRP('MyAct')
     D NUM             S              3  1
     D NEGNUM          S              3  1
     C                   EVAL      NUM = 999,9
     C                   EVAL      NEGNUM = -999,9
     C     NUM           DSPLY
     C     NEGNUM        DSPLY
     C                   SETON                                          RT
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        commandLineProgram.singleCall(emptyList(), configuration)
        val variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        require(variables != null)

        assertEquals(
                expected = "99.9".asBigDecimal(),
                actual = (variables["NUM"] as DecimalValue).value ?: error("Not found NUM")
        )
        assertEquals(
                expected = "-99.9".asBigDecimal(),
                actual = (variables["NEGNUM"] as DecimalValue).value ?: error("Not found NUM")
        )
    }

    @Test
    fun overflowStringVariable() {
        // Variable 'NUM' is defined as '1' integer digit and '0' decimal digit.
        // Due to its definition, 'NUM' variable can store values from -9 to 9.
        // This test shows how numeric variable declaration doesn't work properly cause,
        // cause the 'NUM' variable can store 9999 value.
        val myProgram = """
     H ACTGRP('MyAct')
     D STR             S              2  
     D MSG             S             12
     C                   EVAL      STR = '  '
     C                   EVAL      STR = %TRIM(STR) + 'A'
     C                   EVAL      STR = %TRIM(STR) + 'B'
     C                   EVAL      STR = %TRIM(STR) + 'C'
     C                   EVAL      STR = %TRIM(STR) + 'D'
     C     STR           DSPLY
     C                   SETON                                          RT
     """
        val commandLineProgram = getProgram(nameOrSource = myProgram)
        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        commandLineProgram.singleCall(emptyList(), configuration)
        val variables = memoryStorage.storage[MemorySliceId("MyAct".toUpperCase(), programName = myProgram)]
        require(variables != null)
        assertEquals(
                expected = "AB",
                actual = (variables["STR"] as StringValue).value ?: error("Not found STR")
        )
    }

    @Test
    fun multiThreadTest() {
        // Run all SymbolTableStoragingTest.kt tests in multithread mode

        val testNames = listOf(
            "execPgmAndEvaluateStorage",
            "initPgmByStorageAndEvaluateResult",
            "execLRPgmAndEvaluateStorage",
            "execRTPgmTwiceAndPreserveValues",
            "initPreExistingVariablesPgmByStorageAndEvaluateResult",
            "sameVariablesButDifferentACTGRP",
            "sameVariablesAndSameACTGRP"
        )

        val fixedThreadPool = 10
        val repeatTests = 100
        val timeOutExecutorTermination = 60L

        val executor = Executors.newFixedThreadPool(fixedThreadPool)
        val callables = mutableListOf<Callable<Throwable?>>()
        repeat(repeatTests) {
            for (testName in testNames) {
                val simbolTableStoragingTest = SymbolTableStoragingTest()
                callables.add(WorkerThread(simbolTableStoragingTest, testName))
            }
        }
        val futures = executor.invokeAll(callables)
        executor.shutdown()
        println("Waiting 60s. for all thread to finish...")
        executor.awaitTermination(timeOutExecutorTermination, TimeUnit.SECONDS)
        println("...done")
        if (futures.any { it.get() != null }) {
            throw (futures.first { it.get() != null }.get())!!
        }
    }

    @Test
    fun testExecPgmLoadedFromSource() {
        val executor = Executors.newFixedThreadPool(10)
        var failed: Throwable? = null
        mutableListOf<Runnable>()
        repeat(100) {
            executor.execute {
                runCatching {
                    var varValue = Thread.currentThread().name
                    println("Run interpreter: $varValue")
                    val systemInterface: SystemInterface = JavaSystemInterface()
                    (systemInterface as JavaSystemInterface).addJavaInteropPackage("com.smeup.api")
                    val rpgDir = File("src/test/resources/")
                    val programFinders: List<RpgProgramFinder> = listOf(DirRpgProgramFinder(rpgDir))
                    val jariko = getProgram("XTHREAD.rpgle", systemInterface, programFinders)
                    jariko.singleCall(listOf("FUNZ", "METO", "$varValue"), Configuration().adaptForTestCase(this))
                    println(it)
                }.onFailure {
                    println(it)
                    failed = it
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
        assert(failed == null)
    }

    @Test
    fun playWithStaticACTGRPs() {
        //
        // WHAT THIS TEST DOES:
        // Some cascading calls between programs from main program using the 'CALL' statement.
        // Some programs with same activation group, but all with 'predefined' activation group (none with '*CALLER')
        // acting as a 'static' activation group.
        // Values of 'STRVAR' variable will change during calls due to 'ACTGRP' scope.
        //
        // FLOW:
        // Programs ACTGRP_01 call 'ACTGRP_02' three times (both with *DFTACTGRP activation group)
        // 'ACTGRP_02' append "02." to 'STRVAR' value that is shared between two programs with same ACTGRP and 'RT' ending mode.
        // Final STRVAR must be "02.02.02."
        //
        // Programs ACTGRP_03 call 'ACTGRP_04' three times (both with 'CUSTOM' activation group)
        // 'ACTGRP_04' append "04." to 'STRVAR' value that is shared between two programs with same ACTGRP and 'RT' ending mode.
        // Final STRVAR must be "04.04.04."
        //
        // Programs ACTGRP_05 call 'ACTGRP_06' three times (both with 'CUSTOM' activation group)
        // 'ACTGRP_06' append "06." to 'STRVAR' value that is NOT shared between two programs with same ACTGRP but 'LR' ending mode.
        // Final STRVAR not exists after ACTGRP_06 ended.
        //

        val systemInterface: SystemInterface = JavaSystemInterface()
        (systemInterface as JavaSystemInterface).addJavaInteropPackage("com.smeup.api")
        val rpgDir = File("src/test/resources/")
        val programFinders: List<RpgProgramFinder> = listOf(DirRpgProgramFinder(rpgDir))

        // ACTGRP_01 call ACTGRP_02
        var jariko = getProgram("ACTGRP_01", systemInterface, programFinders)
        var memoryStorage = MemoryStorage()
        var configuration = Configuration(memorySliceStorage = memoryStorage)
        configuration.adaptForTestCase(this)
        jariko.singleCall(emptyList(), configuration)
        val actgrp_02_vars = memoryStorage.storage[MemorySliceId(configuration.defaultActivationGroupName, programName = "ACTGRP_02")]
        require(actgrp_02_vars != null)
        assertEquals(
                expected = "02.02.02.",
                actual = (actgrp_02_vars.get("STRVAR") as StringValue).value.trim() ?: error("Not found STRVAR")
        )

        // ACTGRP_03 call ACTGRP_04
        jariko = getProgram("ACTGRP_03", systemInterface, programFinders)
        memoryStorage = MemoryStorage()
        configuration = Configuration(memorySliceStorage = memoryStorage)
        configuration.adaptForTestCase(this)
        jariko.singleCall(emptyList(), configuration)
        val actgrp_04_vars = memoryStorage.storage[MemorySliceId("CUSTOM", programName = "ACTGRP_04")]
        require(actgrp_04_vars != null)
        assertEquals(
                expected = "04.04.04.",
                actual = (actgrp_04_vars.get("STRVAR") as StringValue).value.trim() ?: error("Not found STRVAR")
        )

        // ACTGRP_05 call ACTGRP_06
        jariko = getProgram("ACTGRP_05", systemInterface, programFinders)
        memoryStorage = MemoryStorage()
        configuration = Configuration(memorySliceStorage = memoryStorage)
        configuration.adaptForTestCase(this)
        jariko.singleCall(emptyList(), configuration)
        val actgrp_06_vars = memoryStorage.storage[MemorySliceId("CUSTOM", programName = "ACTGRP_06")]
        assertEquals(
                expected = null,
                actual = actgrp_06_vars
        )
    }

    @Test
    @Ignore
    fun playWithDynamicACTGRPs() {
        //
        // WHAT THIS TEST DOES:
        // Some cascading calls between programs from main program using the 'CALL' statement.
        // Only the last called program 'ACTGRP_10' has *CALLER activation group, so it can be considered
        // as  'dynamic', depending on 'caller' activation group.
        // Values of 'STRVAR' variable will change during calls due to 'ACTGRP' scope.
        //
        // FLOW:
        // Program ACTGRP_07 (*DFTACTGRP as activation group) call 'ACTGRP_08' (CUSTOM8 actgrp)
        //  - Program ACTGRP_08 call four times 'ACTGRP_10' (*CALLER as activaton group)
        // Program ACTGRP_07 (*DFTACTGRP as activation group) call 'ACTGRP_09' (CUSTOM9 actgrp)
        //  - Program ACTGRP_09 call two times 'ACTGRP_10' (*CALLER as activaton group)
        // Final STRVAR must be "10.10.10.10." in scope of ACTGRP_08.
        // Final STRVAR must be "10.10." in scope of ACTGRP_09.

        val systemInterface: SystemInterface = JavaSystemInterface()
        (systemInterface as JavaSystemInterface).addJavaInteropPackage("com.smeup.api")
        val rpgDir = File("src/test/resources/")
        val programFinders: List<RpgProgramFinder> = listOf(DirRpgProgramFinder(rpgDir))

        // ACTGRP_07 call ACTGRP_08 and ACTGRP_09
        val jariko = getProgram("ACTGRP_07", systemInterface, programFinders)
        val memoryStorage = MemoryStorage()
        val configuration = Configuration(memorySliceStorage = memoryStorage)
        configuration.adaptForTestCase(this)
        jariko.singleCall(emptyList(), configuration)
        val actgrp_10_8_vars = memoryStorage.storage[MemorySliceId("CUSTOM8", programName = "ACTGRP_10")]
        val actgrp_10_9_vars = memoryStorage.storage[MemorySliceId("CUSTOM9", programName = "ACTGRP_10")]
        require(actgrp_10_8_vars != null)
        require(actgrp_10_9_vars != null)
        assertEquals(
                expected = "10.10.10.10.",
                actual = (actgrp_10_8_vars.get("STRVAR") as StringValue).value.trim() ?: error("Not found STRVAR")
        )
        assertEquals(
                expected = "10.10.",
                actual = (actgrp_10_9_vars.get("STRVAR") as StringValue).value.trim() ?: error("Not found STRVAR")
        )
    }

    @Test
    fun playWithStaticACTGRPs_NoMemorystorage() {
        //
        // WHAT THIS TEST DOES:
        // Test run exactly as a part of 'playWithStaticACTGRPs' test, but the 'STRVAR' values check
        // is performed using the 'CALLBACK' technique instead of 'MEMORYSTORAGE'.
        // Some programs with same activation group, but all with 'predefined' activation group (none with '*CALLER')
        // acting as a 'static' activation group.
        // Values of 'STRVAR' variable will change during calls due to 'ACTGRP' scope.
        //
        // FLOW:
        // Programs ACTGRP_01 call 'ACTGRP_02' three times (both with *DFTACTGRP activation group)
        // 'ACTGRP_02' append "02." to 'STRVAR' value that is shared between two programs with same ACTGRP and 'RT' ending mode.
        // Final STRVAR must be "02.02.02."
        //

        // ACTGRP_01 call ACTGRP_02
        val programName = "ACTGRP_01"
        val programPath = File("src${File.separator}test${File.separator}resources${File.separator}").toString() + File.separator + programName
        val programArgs = emptyList<String>()
        var output = mutableListOf<String>()
        val jarikoCallback = JarikoCallback(
                exitInRT = { true },
                onExitPgm = { _: String, symbolTable: ISymbolTable, _: Throwable? ->
                    if (!symbolTable.isEmpty()) {
                        var strvar = symbolTable["STRVAR"]
                        // Main caller may not have 'STRVAR'
                        if (null != strvar) {
                            output.add(strvar.asString().value)
                        }
                    }
                }
        )

        // execWithCallback
        val configuration = Configuration(
                jarikoCallback = jarikoCallback
        )
        val systemInterface = JavaSystemInterface()
        val commandLineProgram = getProgram(programPath, systemInterface)
        commandLineProgram.singleCall(programArgs, configuration = configuration)

        assertTrue { output.isNotEmpty() }
        assertEquals(
                expected = "02.02.02.",
                actual = output[2].trim()
        )
    }
}

class WorkerThread(private val symbolTableStoragingTest: SymbolTableStoragingTest, private val testName: String) :
    Callable<Throwable?> {

    override fun call(): Throwable? {
        kotlin.runCatching {
            println(Thread.currentThread().name + " Start test $testName " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            var varValue = Thread.currentThread().name
            when (testName) {
                "execPgmAndEvaluateStorage" -> symbolTableStoragingTest.execPgmAndEvaluateStorage(varValue)
                "initPgmByStorageAndEvaluateResult" -> symbolTableStoragingTest.initPgmByStorageAndEvaluateResult(varValue)
                "execLRPgmAndEvaluateStorage" -> symbolTableStoragingTest.execLRPgmAndEvaluateStorage(varValue)
                "execRTPgmTwiceAndPreserveValues" -> symbolTableStoragingTest.execRTPgmTwiceAndPreserveValues(varValue)
                "initPreExistingVariablesPgmByStorageAndEvaluateResult" -> symbolTableStoragingTest.initPreExistingVariablesPgmByStorageAndEvaluateResult(varValue)
                "sameVariablesButDifferentACTGRP" -> symbolTableStoragingTest.sameVariablesButDifferentACTGRP(varValue)
                "sameVariablesAndSameACTGRP" -> symbolTableStoragingTest.sameVariablesAndSameACTGRP(varValue)
                else -> {
                    error("Test $testName not exists")
                }
            }
            println(Thread.currentThread().name + " End test $testName " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        }.onFailure {
            println(it)
            return it
        }.onSuccess {
            return null
        }
        return Throwable("Why am i here?")
    }
}