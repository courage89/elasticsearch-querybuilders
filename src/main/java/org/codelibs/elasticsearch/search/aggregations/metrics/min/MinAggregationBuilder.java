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

package org.codelibs.elasticsearch.search.aggregations.metrics.min;

import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.xcontent.ObjectParser;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import org.codelibs.elasticsearch.index.query.QueryParseContext;
import org.codelibs.elasticsearch.search.aggregations.AggregatorFactories.Builder;
import org.codelibs.elasticsearch.search.aggregations.AggregationBuilder;
import org.codelibs.elasticsearch.search.aggregations.AggregatorFactory;
import org.codelibs.elasticsearch.search.aggregations.InternalAggregation.Type;
import org.codelibs.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.codelibs.elasticsearch.search.aggregations.support.ValueType;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSource;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSource.Numeric;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceParserHelper;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceType;
import org.codelibs.elasticsearch.search.internal.SearchContext;

import java.io.IOException;

public class MinAggregationBuilder extends ValuesSourceAggregationBuilder.LeafOnly<ValuesSource.Numeric, MinAggregationBuilder> {
    public static final String NAME = "min";
    private static final Type TYPE = new Type(NAME);

    private static final ObjectParser<MinAggregationBuilder, QueryParseContext> PARSER;
    static {
        PARSER = new ObjectParser<>(AvgAggregationBuilder.NAME);
        ValuesSourceParserHelper.declareNumericFields(PARSER, true, true, false);
    }

    public static AggregationBuilder parse(String aggregationName, QueryParseContext context) throws IOException {
        return PARSER.parse(context.parser(), new MinAggregationBuilder(aggregationName), context);
    }

    public MinAggregationBuilder(String name) {
        super(name, TYPE, ValuesSourceType.NUMERIC, ValueType.NUMERIC);
    }

    /**
     * Read from a stream.
     */
    public MinAggregationBuilder(StreamInput in) throws IOException {
        super(in, TYPE, ValuesSourceType.NUMERIC, ValueType.NUMERIC);
    }

    @Override
    protected void innerWriteTo(StreamOutput out) {
        // Do nothing, no extra state to write to stream
    }

    @Override
    protected MinAggregatorFactory innerBuild(SearchContext context, ValuesSourceConfig<Numeric> config,
            AggregatorFactory<?> parent, Builder subFactoriesBuilder) throws IOException {
        return new MinAggregatorFactory(name, type, config, context, parent, subFactoriesBuilder, metaData);
    }

    @Override
    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        return builder;
    }

    @Override
    protected int innerHashCode() {
        return 0;
    }

    @Override
    protected boolean innerEquals(Object obj) {
        return true;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }
}
