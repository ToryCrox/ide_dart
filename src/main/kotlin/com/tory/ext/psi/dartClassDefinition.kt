package com.tory.ext.psi

import com.tory.DartFileNotWellFormattedException
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.psi.DartClassDefinition
import com.jetbrains.lang.dart.psi.DartComponentName
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.jetbrains.lang.dart.psi.DartReferenceExpression

fun PsiElement.findParentClassDefinition(): DartClassDefinition? =
    findFirstParentOfType()

fun DartClassDefinition.extractClassName(): String =
    allChildren()
        .filterIsInstance<DartComponentName>()
        .firstOrNull()
        ?.text
        ?: throw com.tory.DartFileNotWellFormattedException("Dart class definition does not have a class name")

fun DartClassDefinition.listMethods(): Sequence<DartMethodDeclaration> = findChildrenByType()

// Only this can find the equals method with the ==
// This also looks for operators
fun DartClassDefinition.findMethodsByName(name: String): Sequence<DartMethodDeclaration> =
    listMethods().filter { it.name == name }

/**
 * 获取所有注解类型
 */
val DartClassDefinition.annotationDartTypeList: List<String>
    get() = metadataList.mapNotNull { it.children.filterIsInstance<DartReferenceExpression>().firstOrNull()?.text }