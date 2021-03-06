package org.jetbrains.haskell.parser.grammar

import org.jetbrains.haskell.parser.rules.RuleBasedElementType
import org.jetbrains.haskell.psi.FqName
import org.jetbrains.haskell.parser.rules.notEmptyList
import org.jetbrains.haskell.parser.rules.Rule
import org.jetbrains.haskell.parser.rules.lazy
import org.jetbrains.haskell.parser.rules.aList
import org.jetbrains.haskell.parser.inParentheses
import org.jetbrains.haskell.parser.token.*
import org.jetbrains.haskell.psi.FieldUpdate
import org.jetbrains.haskell.psi.CaseClause
import com.intellij.lang.PsiBuilder
import org.jetbrains.haskell.psi.DoStatement
import org.jetbrains.haskell.parser.rules.rule
import org.jetbrains.haskell.psi.LetExpression
import org.jetbrains.haskell.psi.DoExpression
import org.jetbrains.haskell.psi.ReferenceExpression
import org.jetbrains.haskell.parser.grammar.CONSTRUCTOR
import org.jetbrains.haskell.parser.grammar.VALUE_BODY
import org.jetbrains.haskell.psi.ClassDeclaration
import org.jetbrains.haskell.parser.rules.maybe
import org.jetbrains.haskell.psi.InstanceDeclaration
import org.jetbrains.haskell.parser.grammar.MODULE_NAME
import org.jetbrains.haskell.parser.grammar.MODULE_EXPORTS
import org.jetbrains.haskell.parser.grammar.SYMBOL_EXPORT
import org.jetbrains.haskell.parser.grammar.IMPORT_AS_PART
import org.jetbrains.haskell.psi.ValueDeclaration
import org.jetbrains.haskell.psi.Import
import org.jetbrains.haskell.parser.grammar.CONSTRUCTOR_DECLARATION
import org.jetbrains.haskell.parser.grammar.DATA_DECLARATION
import org.jetbrains.haskell.psi.SomeId
import org.jetbrains.haskell.psi.UnparsedToken
import org.jetbrains.haskell.parser.grammar.MODULE_HEADER
import org.jetbrains.haskell.psi.ConstructorName
import org.jetbrains.haskell.psi.ValueName

/**
 * Created by atsky on 5/2/14.
 */
private val FQ_NAME = RuleBasedElementType("FQ name", ::FqName) {
    notEmptyList(TYPE_OR_CONS, DOT)
}


private val VALUE_NAME = RuleBasedElementType("Value name", ::ValueName) {
    simpleId
}

val CONTEXT : Rule = lazy {
    val aClass : Rule = TYPE_OR_CONS + aList(TYPE)
    (inParentheses(notEmptyList(aClass, COMMA)) or aClass) + DOUBLE_ARROW
}

val untilSemicolon : Rule = object : Rule {
    override fun parse(builder: PsiBuilder): Boolean {
        while (builder.getTokenType() != VIRTUAL_SEMICOLON &&
        builder.getTokenType() != VIRTUAL_RIGHT_PAREN &&
        !builder.eof()) {

            (SOME_ID or ANY).parse(builder)
        }
        return true
    }
}

val expressionList = aList(anAtomExpression, null)


val aGuard = lazy {
    VERTICAL_BAR + anExpression + EQUALS + anExpression
}

val aValueBody = rule(VALUE_BODY) {
    val rhs = (EQUALS + anExpression) or notEmptyList(aGuard)
    VALUE_NAME + expressionList + rhs
}

val CLASS_BODY = lazy {
    aList(VALUE_DECLARATION, VIRTUAL_SEMICOLON)
}

val CLASS_DECLARATION = RuleBasedElementType("Class declaration", ::ClassDeclaration) {
    val body = VIRTUAL_LEFT_PAREN + CLASS_BODY + VIRTUAL_RIGHT_PAREN

    CLASS_KW + maybe(CONTEXT) + TYPE_OR_CONS + aList(TYPE, null) + WHERE_KW + body
}

val INSTANCE_BODY = lazy {
    aList(aValueBody, VIRTUAL_SEMICOLON)
}

val INSTANCE_DECLARATION = RuleBasedElementType("Instance declaration", ::InstanceDeclaration) {
    val body = VIRTUAL_LEFT_PAREN + INSTANCE_BODY + VIRTUAL_RIGHT_PAREN

    INSTANCE_KW + maybe(CONTEXT) + TYPE_OR_CONS + aList(TYPE, null) + WHERE_KW + body
}

private val aModuleName = rule(MODULE_NAME) {
    notEmptyList(TYPE_OR_CONS, DOT)
}

private val aModuleExports = rule(MODULE_EXPORTS) {
    LEFT_PAREN + aList(anExport, COMMA) + maybe(COMMA) + maybe(VIRTUAL_SEMICOLON) + RIGHT_PAREN
}


val anExport = lazy {
    val symbolExport = rule(SYMBOL_EXPORT) {
        simpleId or TYPE_OR_CONS or inParentheses(OPERATOR_ID)
    }

    val qcnameExt = maybe(TYPE_KW) + symbolExport

    val qcnames = notEmptyList(qcnameExt, COMMA)

    val exportSubspec = inParentheses(maybe((DOT_DOT) or qcnames))

    (qcnameExt + maybe(exportSubspec)) or (MODULE_KW + aModuleName)
}

val aImportAsPart = rule(IMPORT_AS_PART) {
    AS_KW + FQ_NAME
}

val VALUE_DECLARATION = RuleBasedElementType("Value declaration", ::ValueDeclaration) {
    notEmptyList(VALUE_NAME, COMMA) + DOUBLE_COLON + TYPE
}

val IMPORT = RuleBasedElementType("Import", ::Import) {
    IMPORT_KW + maybe(QUALIFIED_KW) + aModuleName + maybe(aImportAsPart) +
    maybe(HIDING_KW) + maybe(aModuleExports)
}

val IMPORTS_LIST = aList(IMPORT, VIRTUAL_SEMICOLON)

val typedBinding = lazy {
    VALUE_NAME + DOUBLE_COLON + TYPE
}

val extendedConstructor = lazy {
    LEFT_BRACE + aList(typedBinding, COMMA) + RIGHT_BRACE
}

val aConstructorName = RuleBasedElementType("ConstructorName", ::ConstructorName) {
    TYPE_OR_CONS
}


val aConstructor = rule(CONSTRUCTOR_DECLARATION) {
    aConstructorName + (extendedConstructor or aList(TYPE))
}

val aDataDeclaration = rule(DATA_DECLARATION) {
    val derivingSection = DERIVING_KW + ((LEFT_PAREN + notEmptyList(TYPE_OR_CONS, COMMA) + RIGHT_PAREN) or TYPE_OR_CONS)
    val data_or_newtype = DATA_KW or NEWTYPE_KW
    data_or_newtype + SIMPLETYPE + EQUALS + aList(aConstructor, VERTICAL_BAR) + maybe(derivingSection)
}

val SOME_ID = RuleBasedElementType("Some id", ::SomeId) {
    simpleId or TYPE_OR_CONS or OPERATOR_ID
}

val ANY : Rule = RuleBasedElementType("Any", ::UnparsedToken) {
    object : Rule {
        override fun parse(builder: PsiBuilder): Boolean {
            builder.advanceLexer()
            return true
        }
    }
}

val MODULE_HEADER_RULE = rule(MODULE_HEADER) {
    (aList(PRAGMA) + MODULE_KW + FQ_NAME + maybe(aModuleExports) + WHERE_KW + VIRTUAL_LEFT_PAREN)
}