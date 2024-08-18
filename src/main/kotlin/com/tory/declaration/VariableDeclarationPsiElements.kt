package com.tory.declaration

import com.tory.DartFileNotWellFormattedException
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.lang.dart.psi.*
import com.tory.ext.psi.findFirstChildByType
import com.tory.templater.VariableTemplateParam

// DartVarAccessDeclaration can not be null
// DartType can be null
interface VariableDeclarationPsiElements {
    val modifiers: List<LeafPsiElement>

    // DartType is null if initialized and has a final or const modifier
    val dartType: DartType?
    val name: DartComponentName
    val initializer: DartVarInit?
}

class VariableDeclarationPsiElementsImpl(
    override val modifiers: List<LeafPsiElement>,
    override val dartType: DartType?,
    override val name: DartComponentName,
    override val initializer: DartVarInit?
) : VariableDeclarationPsiElements

interface PublicNamedVariable {
    // The name of the variable that can be used for constructor parameter
    // (unique among all parameter names and has no starting underscore sign)
    val publicVariableName: String
}

class PublicNamedVariableImpl(
    override val publicVariableName: String
) : PublicNamedVariable

// Class constructor will only be used from the 2 given types
class VariableDeclaration(
    psiElements: VariableDeclarationPsiElements,
    publicNamedVariable: PublicNamedVariable
) : VariableDeclarationPsiElements by psiElements,
    PublicNamedVariable by publicNamedVariable

fun VariableDeclarationPsiElements.hasModifier(modifier: DeclarationModifier): Boolean {
    return modifiers.find { it.text == modifier.text } !== null
}

val VariableDeclarationPsiElements.fullTypeName: String?
    get() = dartType?.text?.substringBeforeLast("?")

val VariableDeclarationPsiElements.hasInitializer: Boolean
    get() = initializer !== null

val VariableDeclarationPsiElements.variableName: String
    get() = name.name ?: throw DartFileNotWellFormattedException("Encountered a variable which does not have a name.")

/**
 * 获取成员变量的dartClass 类型
 * DartEnumDefinitionImpl  枚举
 * DartClassDefinitionImpl 普通类
 */
val VariableDeclarationPsiElements.dartClass: DartClass?
    get() {
        val target = dartType?.referenceExpression?.resolve()?.parent
        return if (target is DartClass) target else null
    }

/**
 * 是否是枚举
 */
val VariableDeclarationPsiElements.isEnum: Boolean
    get() = dartClass?.isEnum == true

val VariableDeclarationPsiElements.enumVariableList: List<String>
    get() = dartClass?.enumConstantDeclarationList?.map { it.componentName?.name ?: "" } ?: emptyList()

/// 注解相关
val VariableDeclarationPsiElements.metadataList: List<DartMetadata>
    get() {
        val declaration = name.parent
        return if (declaration is DartVarAccessDeclaration) declaration.metadataList else emptyList()
    }

/// 注解类型
val DartMetadata.dartTypeName: String?
    get() = children.filterIsInstance<DartReferenceExpression>().firstOrNull()?.text

val DartMetadata.variableList: List<String>
    get() {
        val argumentList = children.filterIsInstance<DartArguments>().firstOrNull()?.argumentList ?: return emptyList()
        return argumentList.expressionList.map { it.javaClass.toString() } + argumentList.namedArgumentList.map { it.javaClass.toString() }
    }

fun isVariableNamePrivate(variableName: String): Boolean =
    variableName.startsWith("_")

val VariableDeclarationPsiElements.isPrivate: Boolean
    get() = isVariableNamePrivate(variableName)

val VariableDeclarationPsiElements.isPublic: Boolean
    get() = !isPrivate

val VariableDeclarationPsiElements.isStatic: Boolean
    get() = hasModifier(DeclarationModifier.Static)

val VariableDeclarationPsiElements.isMember: Boolean
    get() = !isStatic

val VariableDeclarationPsiElements.isFinal: Boolean
    get() = hasModifier(DeclarationModifier.Final)

val VariableDeclarationPsiElements.isNullable: Boolean
    get() = dartType?.text?.endsWith("?") ?: false

val VariableDeclarationPsiElements.canBeAssignedFromConstructor: Boolean
    get() {
        val isStatic = hasModifier(DeclarationModifier.Static)
        // Static or static const makes no difference
        if (isStatic) {
            return false
        }

        val isFinal = hasModifier(DeclarationModifier.Final)
        if (isFinal && hasInitializer) {
            return false
        }

        return true
    }

fun Iterable<VariableDeclarationPsiElements>.allMembersFinal(): Boolean {
    return this.asSequence()
        .filter { it.isMember }
        .all { it.isFinal }
}

/**
 * 转换成需要使用的 VariableTemplateParam 类
 */
fun VariableDeclaration.toVariableTemplateParam(): VariableTemplateParam {
    return VariableTemplateParam(
        variableName = variableName,
        type = fullTypeName
            ?: throw RuntimeException("No type is available - this variable should not be assignable from constructor"),
        publicVariableName = publicVariableName,
        isNullable = isNullable,
        isEnum = isEnum,
        enumVariableList = enumVariableList
    )
}
