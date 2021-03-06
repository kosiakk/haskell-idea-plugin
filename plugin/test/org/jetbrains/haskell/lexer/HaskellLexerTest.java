package org.jetbrains.haskell.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.testFramework.LexerTestCase;
import com.intellij.testFramework.ParsingTestCase;
import org.jetbrains.cabal.parser.CaballParserDefinition;
import org.jetbrains.haskell.parser.lexer.HaskellFullLexer;
import org.junit.Test;


public class HaskellLexerTest extends LexerTestCase {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }


    public HaskellLexerTest() {

    }

    @Override
    protected Lexer createLexer() {
        return new HaskellFullLexer();
    }

    @Override
    protected String getDirPath() {
        return "data/haskellLexerTest";
    }

    @Test
    public void testNestedComments() throws Exception {
        doTest(" {- {- -} -} ",
               "WHITE_SPACE (' ')\n" +
               "Haskell Token:COMMENT ('{- {- -} -}')\n" +
               "WHITE_SPACE (' ')");
    }

    @Test
    public void testDigits() throws Exception {
        doTest("0x10FFFF",
               "Haskell Token:number ('0x10FFFF')");
    }

    @Test
    public void testStrings() throws Exception {
        doTest("\"\\\\\" ",
                "Haskell Token:string ('\"\\\\\"')\n" +
                        "WHITE_SPACE (' ')");
        doTest("'\\x2919'",
               "Haskell Token:character (''\\x2919'')");
        doTest("\"\\ \n\t \\\"",
                "Haskell Token:string ('\"\\ \\n\t \\\"')");
    }

    @Test
    public void testQuotation() throws Exception {
        doTest("'name",
                "Haskell Token:' (''')\n" +
                "Haskell Token:id ('name')");
        doTest("''name",
                "Haskell Token:'' ('''')\n" +
                "Haskell Token:id ('name')");
    }

    @Test
    public void testOperators() throws Exception {
        doTest("\u222F",
                "Haskell Token:opertor ('\u222F')");
    }

    @Test
    public void testIndentsComments() throws Exception {
        doTest("module Main where\n" +
               "\n" +
               "main\n",
               "Haskell Token:module ('module')\n" +
               "WHITE_SPACE (' ')\n" +
               "Haskell Token:type_cons ('Main')\n" +
               "WHITE_SPACE (' ')\n" +
               "Haskell Token:where ('where')\n" +
               "NEW_LINE_INDENT ('\\n')\n" +
               "NEW_LINE_INDENT ('\\n')\n" +
               "Haskell Token:VIRTUAL_LEFT_PAREN ('')\n" +
               "Haskell Token:id ('main')\n" +
               "NEW_LINE_INDENT ('\\n')\n" +
               "Haskell Token:VIRTUAL_RIGHT_PAREN ('')");
    }


}
