package co.anbora.labs.sqlfluff.ide.runner

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object SqlFluffLintRunner {

    data class Param(val execPath: String = "", val extraArgs: List<String> = listOf(), val tmpFile: File? = null)

    private val log: Logger = Logger.getInstance(SqlFluffLintRunner::class.java)

    private val TIME_OUT = TimeUnit.SECONDS.toMillis(120L).toInt()
    private const val SUCCESS_NO_ISSUES_FOUND = 0
    private const val SUCCESS_ISSUES_FOUND = 1
    private const val ERROR_OCCURRED = 2

    private val successCode = setOf(SUCCESS_NO_ISSUES_FOUND, SUCCESS_ISSUES_FOUND)

    fun runLint(projectPath: String?, params: Param): Result {
        val result = Result()
        try {
            val out: ProcessOutput = lint(projectPath, params)
            log.info("SqlFluffLintRunner:24 - Got sqlfluff output: $out")
            result.errorOutput = out.stderr
            try {
                if (isOkExecution(out)) {
                    result.output = out.stdoutLines
                    result.isOk = true
                }
            } catch (e: Exception) {
                val output = out.stdout.replace("User Error: ", "")
                result.errorOutput = output
            }
        } catch (e: Exception) {
            result.errorOutput = e.toString()
        }
        return result
    }

    private fun isOkExecution(out: ProcessOutput): Boolean {
        val okResult = out.exitCode in successCode
        if (!okResult) {
            throw IllegalArgumentException()
        }
        return okResult
    }

    class Result {
        var isOk = false
        var output: List<String> = listOf()
        var errorOutput: String? = null

        fun hasErrors(): Boolean = !isOk
    }

    @Throws(ExecutionException::class)
    fun lint(projectPath: String?, params: Param): ProcessOutput {
        val commandLine = GeneralCommandLine()
        commandLine
            .withCharset(StandardCharsets.UTF_8)
            .withWorkDirectory(projectPath)
        commandLine.exePath = params.execPath

        params.extraArgs.forEach {
            commandLine.addParameter(it)
        }

        val output = CommandLineRunner.execute(commandLine, TIME_OUT)

        // we sometimes create tmp files in project dir, and if it exists,
        // make sure to delete it as soon as linting is completed.
        if (params.tmpFile != null) {
            try {
                params.tmpFile.delete()
            // Ignore IOException: if the file is already deleted, it doesn't matter
            } catch (_: IOException) {}
        }
        return output
    }

}
