package com.tory.action.utils

import com.tory.declaration.DeclarationExtractor
import com.tory.declaration.VariableDeclaration
import com.tory.dialog.GenerateDialog
import com.intellij.openapi.project.Project
import com.jetbrains.lang.dart.psi.DartClassDefinition

/**
 * Returns null if user decided to cancel the operation
 */
fun selectFieldsWithDialog(
    project: Project,
    dartClass: DartClassDefinition
): List<VariableDeclaration>? {

    val declarations = DeclarationExtractor.extractDeclarationsFromClass(dartClass)

    val dialog = GenerateDialog(
        project,
        declarations
    )
    dialog.show()

    if (!dialog.isOK) {
        return null
    }

    return dialog.getSelectedFields()
}