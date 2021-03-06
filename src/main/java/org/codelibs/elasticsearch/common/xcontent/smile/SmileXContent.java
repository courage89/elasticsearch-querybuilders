/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.common.xcontent.smile;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import org.codelibs.elasticsearch.common.bytes.BytesReference;
import org.codelibs.elasticsearch.common.io.FastStringReader;
import org.codelibs.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.codelibs.elasticsearch.common.xcontent.XContent;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import org.codelibs.elasticsearch.common.xcontent.XContentGenerator;
import org.codelibs.elasticsearch.common.xcontent.XContentParser;
import org.codelibs.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Set;

/**
 * A Smile based content implementation using Jackson.
 */
public class SmileXContent implements XContent {

    public static XContentBuilder contentBuilder() throws IOException {
        return XContentBuilder.builder(smileXContent);
    }

    static final SmileFactory smileFactory;
    public static final SmileXContent smileXContent;

    static {
        smileFactory = new SmileFactory();
        smileFactory.configure(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT, false); // for now, this is an overhead, might make sense for web sockets
        smileFactory.configure(SmileFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW, false); // this trips on many mappings now...
        // Do not automatically close unclosed objects/arrays in com.fasterxml.jackson.dataformat.smile.SmileGenerator#close() method
        smileFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        smileXContent = new SmileXContent();
    }

    private SmileXContent() {
    }

    @Override
    public XContentType type() {
        return XContentType.SMILE;
    }

    @Override
    public byte streamSeparator() {
        return (byte) 0xFF;
    }

    @Override
    public XContentGenerator createGenerator(OutputStream os, Set<String> includes, Set<String> excludes) throws IOException {
        return new SmileXContentGenerator(smileFactory.createGenerator(os, JsonEncoding.UTF8), os, includes, excludes);
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, String content) throws IOException {
        return new SmileXContentParser(xContentRegistry, smileFactory.createParser(new FastStringReader(content)));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, InputStream is) throws IOException {
        return new SmileXContentParser(xContentRegistry, smileFactory.createParser(is));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, byte[] data) throws IOException {
        return new SmileXContentParser(xContentRegistry, smileFactory.createParser(data));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, byte[] data, int offset, int length) throws IOException {
        return new SmileXContentParser(xContentRegistry, smileFactory.createParser(data, offset, length));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, BytesReference bytes) throws IOException {
        return createParser(xContentRegistry, bytes.streamInput());
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, Reader reader) throws IOException {
        return new SmileXContentParser(xContentRegistry, smileFactory.createParser(reader));
    }
}
