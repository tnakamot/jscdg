package com.github.tnakamoto.jscdg.lexer;

import static org.junit.Assert.*;

import com.github.tnakamot.jscdg.lexer.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class JSONLexerTest {
    @Test
    public void testEmpty() throws IOException, JSONLexerException {
        List<JSONToken> tokens = JSONText.fromString("").tokens();
        assertEquals(0, tokens.size());
    }

    @Test
    public void testWSOnly() throws IOException, JSONLexerException {
        List<JSONToken> tokens = JSONText.fromString(" \r\n\t").tokens();
        assertEquals(0, tokens.size());
    }

    @Test
    public void testSimpleObject() throws IOException, JSONLexerException {
        // TODO: add number
        JSONText jsText = JSONText.fromString(" { \"key\":\r\n[true,\nfalse,\rnull]\n\r} ");
        List<JSONToken> tokens = jsText.tokens();

        assertEquals(JSONTokenType.BEGIN_OBJECT, tokens.get(0).type());
        assertEquals("{", tokens.get(0).text());
        assertEquals(1, tokens.get(0).location().position());
        assertEquals(1, tokens.get(0).location().line());
        assertEquals(2, tokens.get(0).location().column());
        assertEquals(jsText, tokens.get(0).source());

        assertEquals(JSONTokenType.STRING, tokens.get(1).type());
        assertEquals("\"key\"", tokens.get(1).text());
        assertEquals(3, tokens.get(1).location().position());
        assertEquals(1, tokens.get(1).location().line());
        assertEquals(4, tokens.get(1).location().column());
        assertEquals(jsText, tokens.get(1).source());
        assertTrue(tokens.get(1) instanceof JSONTokenString);
        assertEquals("key", ((JSONTokenString) tokens.get(1)).value());

        assertEquals(JSONTokenType.NAME_SEPARATOR, tokens.get(2).type());
        assertEquals(":", tokens.get(2).text());
        assertEquals(8, tokens.get(2).location().position());
        assertEquals(1, tokens.get(2).location().line());
        assertEquals(9, tokens.get(2).location().column());
        assertEquals(jsText, tokens.get(2).source());

        assertEquals(JSONTokenType.BEGIN_ARRAY, tokens.get(3).type());
        assertEquals("[", tokens.get(3).text());
        assertEquals(11, tokens.get(3).location().position());
        assertEquals(2, tokens.get(3).location().line());
        assertEquals(1, tokens.get(3).location().column());
        assertEquals(jsText, tokens.get(3).source());

        assertEquals(JSONTokenType.BOOLEAN, tokens.get(4).type());
        assertEquals("true", tokens.get(4).text());
        assertEquals(12, tokens.get(4).location().position());
        assertEquals(2, tokens.get(4).location().line());
        assertEquals(2, tokens.get(4).location().column());
        assertEquals(jsText, tokens.get(4).source());
        assertTrue(tokens.get(4) instanceof JSONTokenBoolean);
        assertEquals(true, ((JSONTokenBoolean) tokens.get(4)).value());

        assertEquals(JSONTokenType.VALUE_SEPARATOR, tokens.get(5).type());
        assertEquals(",", tokens.get(5).text());
        assertEquals(16, tokens.get(5).location().position());
        assertEquals(2, tokens.get(5).location().line());
        assertEquals(6, tokens.get(5).location().column());
        assertEquals(jsText, tokens.get(5).source());

        assertEquals(JSONTokenType.BOOLEAN, tokens.get(6).type());
        assertEquals("false", tokens.get(6).text());
        assertEquals(18, tokens.get(6).location().position());
        assertEquals(3, tokens.get(6).location().line());
        assertEquals(1, tokens.get(6).location().column());
        assertEquals(jsText, tokens.get(6).source());
        assertTrue(tokens.get(6) instanceof JSONTokenBoolean);
        assertEquals(false, ((JSONTokenBoolean) tokens.get(6)).value());

        assertEquals(JSONTokenType.VALUE_SEPARATOR, tokens.get(7).type());
        assertEquals(",", tokens.get(7).text());
        assertEquals(23, tokens.get(7).location().position());
        assertEquals(3, tokens.get(7).location().line());
        assertEquals(6, tokens.get(7).location().column());
        assertEquals(jsText, tokens.get(7).source());

        assertEquals(JSONTokenType.NULL, tokens.get(8).type());
        assertEquals("null", tokens.get(8).text());
        assertEquals(25, tokens.get(8).location().position());
        assertEquals(4, tokens.get(8).location().line());
        assertEquals(1, tokens.get(8).location().column());
        assertEquals(jsText, tokens.get(8).source());
        assertTrue(tokens.get(8) instanceof JSONTokenNull);

        assertEquals(JSONTokenType.END_ARRAY, tokens.get(9).type());
        assertEquals("]", tokens.get(9).text());
        assertEquals(29, tokens.get(9).location().position());
        assertEquals(4, tokens.get(9).location().line());
        assertEquals(5, tokens.get(9).location().column());
        assertEquals(jsText, tokens.get(9).source());

        assertEquals(JSONTokenType.END_OBJECT, tokens.get(10).type());
        assertEquals("}", tokens.get(10).text());
        assertEquals(32, tokens.get(10).location().position());
        assertEquals(6, tokens.get(10).location().line());
        assertEquals(1, tokens.get(10).location().column());
        assertEquals(jsText, tokens.get(10).source());
    }

    @Test
    public void testEscapedString() {
        // TODO: test escape string
    }
}