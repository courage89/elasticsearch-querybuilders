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

package org.codelibs.elasticsearch.search.aggregations.bucket.terms;

import org.codelibs.elasticsearch.common.ParseField;
import org.codelibs.elasticsearch.search.DocValueFormat;
import org.codelibs.elasticsearch.search.aggregations.Aggregator;
import org.codelibs.elasticsearch.search.aggregations.Aggregator.SubAggCollectionMode;
import org.codelibs.elasticsearch.search.aggregations.AggregatorFactories;
import org.codelibs.elasticsearch.search.aggregations.AggregatorFactory;
import org.codelibs.elasticsearch.search.aggregations.InternalAggregation;
import org.codelibs.elasticsearch.search.aggregations.InternalAggregation.Type;
import org.codelibs.elasticsearch.search.aggregations.NonCollectingAggregator;
import org.codelibs.elasticsearch.search.aggregations.bucket.terms.support.IncludeExclude;
import org.codelibs.elasticsearch.search.aggregations.pipeline.PipelineAggregator;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSource;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import org.codelibs.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.codelibs.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TermsAggregatorFactory extends ValuesSourceAggregatorFactory<ValuesSource, TermsAggregatorFactory> {

    private final Terms.Order order;
    private final TermsAggregator.BucketCountThresholds bucketCountThresholds;
    public TermsAggregatorFactory(String name, Type type, ValuesSourceConfig<ValuesSource> config, Terms.Order order,
            IncludeExclude includeExclude, String executionHint, SubAggCollectionMode collectMode,
            TermsAggregator.BucketCountThresholds bucketCountThresholds, boolean showTermDocCountError, SearchContext context,
            AggregatorFactory<?> parent, AggregatorFactories.Builder subFactoriesBuilder, Map<String, Object> metaData) throws IOException {
        super(name, type, config, context, parent, subFactoriesBuilder, metaData);
        this.order = order;
        this.bucketCountThresholds = bucketCountThresholds;
    }

    @Override
    protected Aggregator createUnmapped(Aggregator parent, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData)
            throws IOException {
        final InternalAggregation aggregation = new UnmappedTerms(name, order, bucketCountThresholds.getRequiredSize(),
                bucketCountThresholds.getMinDocCount(), pipelineAggregators, metaData);
        return new NonCollectingAggregator(name, context, parent, factories, pipelineAggregators, metaData) {
            {
                // even in the case of an unmapped aggregator, validate the
                // order
                InternalOrder.validate(order, this);
            }

            @Override
            public InternalAggregation buildEmptyAggregation() {
                return aggregation;
            }
        };
    }

    @Override
    protected Aggregator doCreateInternal(ValuesSource valuesSource, Aggregator parent, boolean collectsFromSingleBucket,
            List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) throws IOException {
        throw new UnsupportedOperationException("querybuilders does not support this operation.");
    }

    // return the SubAggCollectionMode that this aggregation should use based on the expected size
    // and the cardinality of the field
    static SubAggCollectionMode subAggCollectionMode(int expectedSize, long maxOrd) {
        if (expectedSize == Integer.MAX_VALUE) {
            // return all buckets
            return SubAggCollectionMode.DEPTH_FIRST;
        }
        if (maxOrd == -1 || maxOrd > expectedSize) {
            // use breadth_first if the cardinality is bigger than the expected size or unknown (-1)
            return SubAggCollectionMode.BREADTH_FIRST;
        }
        return SubAggCollectionMode.DEPTH_FIRST;
    }

    public enum ExecutionMode {

        MAP(new ParseField("map")) {

            @Override
            Aggregator create(String name, AggregatorFactories factories, ValuesSource valuesSource, Terms.Order order,
                    DocValueFormat format, TermsAggregator.BucketCountThresholds bucketCountThresholds, IncludeExclude includeExclude,
                    SearchContext context, Aggregator parent, SubAggCollectionMode subAggCollectMode,
                    boolean showTermDocCountError, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData)
                            throws IOException {
                final IncludeExclude.StringFilter filter = includeExclude == null ? null : includeExclude.convertToStringFilter(format);
                return new StringTermsAggregator(name, factories, valuesSource, order, format, bucketCountThresholds, filter,
                        context, parent, subAggCollectMode, showTermDocCountError, pipelineAggregators, metaData);
            }

            @Override
            boolean needsGlobalOrdinals() {
                return false;
            }

        },
        GLOBAL_ORDINALS(new ParseField("global_ordinals")) {

            @Override
            Aggregator create(String name, AggregatorFactories factories, ValuesSource valuesSource, Terms.Order order,
                    DocValueFormat format, TermsAggregator.BucketCountThresholds bucketCountThresholds, IncludeExclude includeExclude,
                    SearchContext context, Aggregator parent, SubAggCollectionMode subAggCollectMode,
                    boolean showTermDocCountError, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData)
                            throws IOException {
                final IncludeExclude.OrdinalsFilter filter = includeExclude == null ? null : includeExclude.convertToOrdinalsFilter(format);
                return new GlobalOrdinalsStringTermsAggregator(name, factories, (ValuesSource.Bytes.WithOrdinals) valuesSource, order,
                        format, bucketCountThresholds, filter, context, parent, subAggCollectMode, showTermDocCountError,
                        pipelineAggregators, metaData);
            }

            @Override
            boolean needsGlobalOrdinals() {
                return true;
            }

        },
        GLOBAL_ORDINALS_HASH(new ParseField("global_ordinals_hash")) {

            @Override
            Aggregator create(String name, AggregatorFactories factories, ValuesSource valuesSource, Terms.Order order,
                    DocValueFormat format, TermsAggregator.BucketCountThresholds bucketCountThresholds, IncludeExclude includeExclude,
                    SearchContext context, Aggregator parent, SubAggCollectionMode subAggCollectMode,
                    boolean showTermDocCountError, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData)
                            throws IOException {
                final IncludeExclude.OrdinalsFilter filter = includeExclude == null ? null : includeExclude.convertToOrdinalsFilter(format);
                return new GlobalOrdinalsStringTermsAggregator.WithHash(name, factories, (ValuesSource.Bytes.WithOrdinals) valuesSource,
                        order, format, bucketCountThresholds, filter, context, parent, subAggCollectMode, showTermDocCountError,
                        pipelineAggregators, metaData);
            }

            @Override
            boolean needsGlobalOrdinals() {
                return true;
            }
        },
        GLOBAL_ORDINALS_LOW_CARDINALITY(new ParseField("global_ordinals_low_cardinality")) {

            @Override
            Aggregator create(String name, AggregatorFactories factories, ValuesSource valuesSource, Terms.Order order,
                    DocValueFormat format, TermsAggregator.BucketCountThresholds bucketCountThresholds, IncludeExclude includeExclude,
                    SearchContext context, Aggregator parent, SubAggCollectionMode subAggCollectMode,
                    boolean showTermDocCountError, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData)
                            throws IOException {
                if (includeExclude != null || factories.countAggregators() > 0
                // we need the FieldData impl to be able to extract the
                // segment to global ord mapping
                        || valuesSource.getClass() != ValuesSource.Bytes.FieldData.class) {
                    return GLOBAL_ORDINALS.create(name, factories, valuesSource, order, format, bucketCountThresholds, includeExclude,
                            context, parent, subAggCollectMode, showTermDocCountError, pipelineAggregators, metaData);
                }
                return new GlobalOrdinalsStringTermsAggregator.LowCardinality(name, factories,
                        (ValuesSource.Bytes.WithOrdinals) valuesSource, order, format, bucketCountThresholds, context, parent,
                        subAggCollectMode, showTermDocCountError, pipelineAggregators, metaData);
            }

            @Override
            boolean needsGlobalOrdinals() {
                return true;
            }
        };

        public static ExecutionMode fromString(String value) {
            for (ExecutionMode mode : values()) {
                if (mode.parseField.match(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown `execution_hint`: [" + value + "], expected any of " + values());
        }

        private final ParseField parseField;

        ExecutionMode(ParseField parseField) {
            this.parseField = parseField;
        }

        abstract Aggregator create(String name, AggregatorFactories factories, ValuesSource valuesSource, Terms.Order order,
                DocValueFormat format, TermsAggregator.BucketCountThresholds bucketCountThresholds, IncludeExclude includeExclude,
                SearchContext context, Aggregator parent, SubAggCollectionMode subAggCollectMode,
                boolean showTermDocCountError, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData)
                        throws IOException;

        abstract boolean needsGlobalOrdinals();

        @Override
        public String toString() {
            return parseField.getPreferredName();
        }
    }

}
