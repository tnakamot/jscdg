/*
 *  Copyright (C) 2020 Takashi Nakamoto <nyakamoto@gmail.com>.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.tnakamot.jscdg.lexer;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;

/**
 * An implementation of lexical analyzer of JSON text.
 *
 * The instance of this class is not thread-safe.
 */
public class JSONLexer {
    private final JSONText source;
    private final PushbackReader reader;
    private int location;

    protected JSONLexer(JSONText source) {
        if (source == null) {
            throw new NullPointerException("source cannot be null");
        }

        this.source   = source;
        this.reader   = new PushbackReader(new StringReader(source.get()));
        this.location = 0;
    }

    private int read() throws IOException {
        location += 1;
        return reader.read();
    }

    private char readChar() throws IOException, JSONLexerException {
        int ich = read();
        if (ich == -1)
            throw new JSONLexerException(source, location, "reached EOF unexpectedly");

        return (char)ich;
    }

    private void pushBack(int ch) throws IOException {
        location -= 1;
        reader.unread(ch);
    }

    /**
     * Read and return the next JSON token.
     *
     * @return the next token, or null if reached EOF
     * @throws IOException if I/O error happens
     * @throws JSONLexerException if there is a syntax error in JSON text
     * @see <a href="https://tools.ietf.org/html/rfc8259#section-2">RFC 8259 - 2. JSON Grammer</a>
     */
    public JSONToken next() throws IOException, JSONLexerException {
        if (skipWhiteSpaces()) {
            return null;
        }

        int startLocation = location;
        char ch = readChar();

        switch (ch) {
            case '[':
                return new JSONToken(JSONTokenType.BEGIN_ARRAY, "[", startLocation, source);
            case ']':
                return new JSONToken(JSONTokenType.END_ARRAY, "]", startLocation, source);
            case '{':
                return new JSONToken(JSONTokenType.BEGIN_OBJECT, "{", startLocation, source);
            case '}':
                return new JSONToken(JSONTokenType.END_OBJECT, "}", startLocation, source);
            case ':':
                return new JSONToken(JSONTokenType.NAME_SEPARATOR, ":", startLocation, source);
            case ',':
                return new JSONToken(JSONTokenType.VALUE_SEPARATOR, ",", startLocation, source);
            case 't':
                pushBack(ch);
                expect(JSONTokenBoolean.JSON_TRUE);
                return new JSONTokenBoolean(JSONTokenBoolean.JSON_TRUE, startLocation, source);
            case 'f':
                pushBack(ch);
                expect(JSONTokenBoolean.JSON_FALSE);
                return new JSONTokenBoolean(JSONTokenBoolean.JSON_FALSE, startLocation, source);
            case 'n':
                pushBack(ch);
                expect(JSONTokenNull.JSON_NULL);
                return new JSONTokenNull(startLocation, source);
            case '"':
                pushBack(ch);
                return readString();
            // TODO: support number
            default:
                throw new JSONLexerException(source, startLocation, "unexpected character '" + ch +"'");
        }
    }

    private void expect(String expected)
            throws IOException, JSONLexerException
    {
        int expectedLen = expected.length();
        int originalLocation = location;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < expectedLen; i++ ) {
            char expectedCh = expected.charAt(i);
            char ch = readChar();
            sb.append(ch);

            if (ch != expectedCh) {
                throw new JSONLexerException(source, originalLocation,
                        "unknown token starting with '" + sb.toString() + "'");
            }
        }
    }

    /**
     * Read one JSON string token.
     *
     * @return
     * @throws IOException
     * @throws JSONLexerException
     * @see <a href="https://tools.ietf.org/html/rfc8259#section-7">RFC 8259 - 7. Strings</a>
     */
    private JSONTokenString readString()
            throws IOException, JSONLexerException {
        int originalLocation = location;
        StringBuilder tokenText = new StringBuilder();
        StringBuilder strValue  = new StringBuilder();
        boolean escaped = false;

        char ch = readChar();
        if (ch != '"')
            throw new JSONLexerException(source, originalLocation, "string token must start with '\"'");
        tokenText.append(ch);

        while (true) {
            ch = readChar();
            tokenText.append(ch);

            if (ch < 0x20) {
                StringBuilder errmsg = new StringBuilder();
                errmsg.append("control character ");
                errmsg.append(String.format("U+%04x", (int)ch));
                errmsg.append(" is not allowed in a JSON string token");
                throw new JSONLexerException(source, location - 1, errmsg.toString());
            }

            if (escaped) {
                switch (ch) {
                    case '"':
                    case '\\':
                    case '/':
                        strValue.append(ch);
                        break;
                    case 'b':
                        strValue.append('\b');
                        break;
                    case 'f':
                        strValue.append('\f');
                        break;
                    case 'n':
                        strValue.append('\n');
                        break;
                    case 'r':
                        strValue.append('\r');
                        break;
                    case 't':
                        strValue.append('\t');
                        break;
                    case 'u':
                        int unicode = 0;
                        for (int i = 0; i < 4; i++) {
                            char v = readChar();
                            tokenText.append(v);

                            unicode = unicode * 16;
                            if ('0' <= v && v <= '9') {
                                unicode = unicode + (v - '0');
                            } else if ('a' <= v && v <= 'f') {
                                unicode = unicode + (v - 'a') + 10;
                            } else if ('A' <= v && v <= 'F') {
                                unicode = unicode + (v - 'A') + 10;
                            } else {
                                throw new JSONLexerException(source, location - 1,
                                        "an Unicode escape sequence must consist of four characters of [0-9A-Fa-f]");
                            }
                        }

                        strValue.append((char) unicode);
                        break;
                    default:
                        throw new JSONLexerException(source, location - 1,
                                "unexpected character for an escape sequence");
                }

                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                break;
            } else {
                strValue.append(ch);
            }
        }

        return new JSONTokenString(tokenText.toString(), strValue.toString(), originalLocation, source);
    }

    /**
     * Skip insignificant white spaces. The white space characters are defined
     * in RFC 8259.
     *
     * @throws IOException if an I/O error occurs
     * @return true if the end of the text has been reached
     *
     * @see <a href="https://tools.ietf.org/html/rfc8259#section-2">RFC 8259 - 2. JSON Grammer</a>
     */
    private boolean skipWhiteSpaces() throws IOException {
        while (true) {
            int ch = read();

            switch (ch) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    continue;
                case -1:
                    return true;
                default:
                    pushBack(ch);
                    return false;
            }
        }
    }
}