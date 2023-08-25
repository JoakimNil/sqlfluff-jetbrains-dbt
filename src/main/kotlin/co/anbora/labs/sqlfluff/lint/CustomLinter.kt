package co.anbora.labs.sqlfluff.lint

import co.anbora.labs.sqlfluff.ide.runner.SqlFluffLintRunner
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import java.nio.file.Files
import kotlin.io.path.pathString

object CustomLinter: Linter() {

    override fun buildCommandLineArgs(
        python: String,
        lint: String,
        lintOptions: String,
        file: PsiFile,
        document: Document
    ): SqlFluffLintRunner.Param {
        val nioFile = file.virtualFile.toNioPath()
        // Create a temp file in the same folder as the file being parsed
        val tmpFilePath = Files.createTempFile(nioFile.parent, "__sqlfluff_tmp_", ".sql")
        val tmpFile = tmpFilePath.toFile()
        tmpFile.deleteOnExit()  // Mark the file for deletion for when the virtual machine terminates
        Files.write(tmpFilePath, document.text.toByteArray()) // hack trick because virtual file has the changes and real file no
        return SqlFluffLintRunner.Param(
            execPath = python,
            extraArgs = listOf(lint, LINT_COMMAND, tmpFilePath.pathString, *lintOptions.split(" ").toTypedArray()),
            tmpFile = tmpFile
        )
    }
}
