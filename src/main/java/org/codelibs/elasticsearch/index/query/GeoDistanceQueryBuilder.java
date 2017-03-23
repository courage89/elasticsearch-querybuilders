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

package org.codelibs.elasticsearch.index.query;

import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.codelibs.elasticsearch.common.ParseField;
import org.codelibs.elasticsearch.common.Strings;
import org.codelibs.elasticsearch.common.geo.GeoDistance;
import org.codelibs.elasticsearch.common.geo.GeoPoint;
import org.codelibs.elasticsearch.common.geo.GeoUtils;
import org.codelibs.elasticsearch.common.io.stream.StreamInput;
import org.codelibs.elasticsearch.common.io.stream.StreamOutput;
import org.codelibs.elasticsearch.common.unit.DistanceUnit;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Filter results of a query to include only those within a specific distance to some
 * geo point.
 */
public class GeoDistanceQueryBuilder extends AbstractQueryBuilder<GeoDistanceQueryBuilder> {
    public static final String NAME = "geo_distance";

    /** Default for latitude normalization (as of this writing true).*/
    public static final boolean DEFAULT_NORMALIZE_LAT = true;
    /** Default for longitude normalization (as of this writing true). */
    public static final boolean DEFAULT_NORMALIZE_LON = true;
    /** Default for distance unit computation. */
    public static final DistanceUnit DEFAULT_DISTANCE_UNIT = DistanceUnit.DEFAULT;
    /** Default for geo distance computation. */
    public static final GeoDistance DEFAULT_GEO_DISTANCE = GeoDistance.DEFAULT;
    /** Default for optimising query through pre computed bounding box query. */
    @Deprecated
    public static final String DEFAULT_OPTIMIZE_BBOX = "memory";

    /**
     * The default value for ignore_unmapped.
     */
    public static final boolean DEFAULT_IGNORE_UNMAPPED = false;

    private static final ParseField VALIDATION_METHOD_FIELD = new ParseField("validation_method");
    private static final ParseField IGNORE_MALFORMED_FIELD = new ParseField("ignore_malformed").withAllDeprecated("validation_method");
    private static final ParseField COERCE_FIELD = new ParseField("coerce", "normalize").withAllDeprecated("validation_method");
    @Deprecated
    private static final ParseField OPTIMIZE_BBOX_FIELD = new ParseField("optimize_bbox")
            .withAllDeprecated("no replacement: `optimize_bbox` is no longer supported due to recent improvements");
    private static final ParseField DISTANCE_TYPE_FIELD = new ParseField("distance_type");
    private static final ParseField UNIT_FIELD = new ParseField("unit");
    private static final ParseField DISTANCE_FIELD = new ParseField("distance");
    private static final ParseField IGNORE_UNMAPPED_FIELD = new ParseField("ignore_unmapped");

    private final String fieldName;
    /** Distance from center to cover. */
    private double distance;
    /** Point to use as center. */
    private GeoPoint center = new GeoPoint(Double.NaN, Double.NaN);
    /** Algorithm to use for distance computation. */
    private GeoDistance geoDistance = DEFAULT_GEO_DISTANCE;
    /** Whether or not to use a bbox for pre-filtering. TODO change to enum? */
    private String optimizeBbox = null;
    /** How strict should geo coordinate validation be? */
    private GeoValidationMethod validationMethod = GeoValidationMethod.DEFAULT;

    private boolean ignoreUnmapped = DEFAULT_IGNORE_UNMAPPED;

    /**
     * Construct new GeoDistanceQueryBuilder.
     * @param fieldName name of indexed geo field to operate distance computation on.
     * */
    public GeoDistanceQueryBuilder(String fieldName) {
        if (Strings.isEmpty(fieldName)) {
            throw new IllegalArgumentException("fieldName must not be null or empty");
        }
        this.fieldName = fieldName;
    }

    /**
     * Read from a stream.
     */
    public GeoDistanceQueryBuilder(StreamInput in) throws IOException {
        super(in);
        fieldName = in.readString();
        distance = in.readDouble();
        validationMethod = GeoValidationMethod.readFromStream(in);
        center = in.readGeoPoint();
        optimizeBbox = in.readOptionalString();
        geoDistance = GeoDistance.readFromStream(in);
        ignoreUnmapped = in.readBoolean();
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeString(fieldName);
        out.writeDouble(distance);
        validationMethod.writeTo(out);
        out.writeGeoPoint(center);
        out.writeOptionalString(optimizeBbox);
        geoDistance.writeTo(out);
        out.writeBoolean(ignoreUnmapped);
    }

    /** Name of the field this query is operating on. */
    public String fieldName() {
        return this.fieldName;
    }

    /** Sets the center point for the query.
     * @param point the center of the query
     **/
    public GeoDistanceQueryBuilder point(GeoPoint point) {
        if (point == null) {
            throw new IllegalArgumentException("center point must not be null");
        }
        this.center = point;
        return this;
    }

    /**
     * Sets the center point of the query.
     * @param lat latitude of center
     * @param lon longitude of center
     * */
    public GeoDistanceQueryBuilder point(double lat, double lon) {
        this.center = new GeoPoint(lat, lon);
        return this;
    }

    /** Returns the center point of the distance query. */
    public GeoPoint point() {
        return this.center;
    }

    /** Sets the distance from the center using the default distance unit.*/
    public GeoDistanceQueryBuilder distance(String distance) {
        return distance(distance, DistanceUnit.DEFAULT);
    }

    /** Sets the distance from the center for this query. */
    public GeoDistanceQueryBuilder distance(String distance, DistanceUnit unit) {
        if (Strings.isEmpty(distance)) {
            throw new IllegalArgumentException("distance must not be null or empty");
        }
        if (unit == null) {
            throw new IllegalArgumentException("distance unit must not be null");
        }
        double newDistance = DistanceUnit.parse(distance, unit, DistanceUnit.DEFAULT);
        if (newDistance <= 0.0) {
            throw new IllegalArgumentException("distance must be greater than zero");
        }
        this.distance = newDistance;
        return this;
    }

    /** Sets the distance from the center for this query. */
    public GeoDistanceQueryBuilder distance(double distance, DistanceUnit unit) {
        return distance(Double.toString(distance), unit);
    }

    /** Returns the distance configured as radius. */
    public double distance() {
        return distance;
    }

    /** Sets the center point for this query. */
    public GeoDistanceQueryBuilder geohash(String geohash) {
        if (Strings.isEmpty(geohash)) {
            throw new IllegalArgumentException("geohash must not be null or empty");
        }
        this.center.resetFromGeoHash(geohash);
        return this;
    }

    /** Which type of geo distance calculation method to use. */
    public GeoDistanceQueryBuilder geoDistance(GeoDistance geoDistance) {
        if (geoDistance == null) {
            throw new IllegalArgumentException("geoDistance must not be null");
        }
        this.geoDistance = geoDistance;
        return this;
    }

    /** Returns geo distance calculation type to use. */
    public GeoDistance geoDistance() {
        return this.geoDistance;
    }

    /**
     * Set this to memory or indexed if before running the distance
     * calculation you want to limit the candidates to hits in the
     * enclosing bounding box.
     * @deprecated
     **/
    @Deprecated
    public GeoDistanceQueryBuilder optimizeBbox(String optimizeBbox) {
        this.optimizeBbox = optimizeBbox;
        return this;
    }

    /**
     * Returns whether or not to run a BoundingBox query prior to
     * distance query for optimization purposes.
     * @deprecated
     **/
    @Deprecated
    public String optimizeBbox() {
        return this.optimizeBbox;
    }

    /** Set validation method for geo coordinates. */
    public void setValidationMethod(GeoValidationMethod method) {
        this.validationMethod = method;
    }

    /** Returns validation method for geo coordinates. */
    public GeoValidationMethod getValidationMethod() {
        return this.validationMethod;
    }

    /**
     * Sets whether the query builder should ignore unmapped fields (and run a
     * {@link MatchNoDocsQuery} in place of this query) or throw an exception if
     * the field is unmapped.
     */
    public GeoDistanceQueryBuilder ignoreUnmapped(boolean ignoreUnmapped) {
        this.ignoreUnmapped = ignoreUnmapped;
        return this;
    }

    /**
     * Gets whether the query builder will ignore unmapped fields (and run a
     * {@link MatchNoDocsQuery} in place of this query) or throw an exception if
     * the field is unmapped.
     */
    public boolean ignoreUnmapped() {
        return ignoreUnmapped;
    }

    @Override
    protected Query doToQuery(QueryShardContext shardContext) throws IOException {
        throw new UnsupportedOperationException("querybuilders does not support this operation.");
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.startArray(fieldName).value(center.lon()).value(center.lat()).endArray();
        builder.field(DISTANCE_FIELD.getPreferredName(), distance);
        builder.field(DISTANCE_TYPE_FIELD.getPreferredName(), geoDistance.name().toLowerCase(Locale.ROOT));
        if (Strings.isEmpty(optimizeBbox) == false) {
            builder.field(OPTIMIZE_BBOX_FIELD.getPreferredName(), optimizeBbox);
        }
        builder.field(VALIDATION_METHOD_FIELD.getPreferredName(), validationMethod);
        builder.field(IGNORE_UNMAPPED_FIELD.getPreferredName(), ignoreUnmapped);
        printBoostAndQueryName(builder);
        builder.endObject();
    }

    public static Optional<GeoDistanceQueryBuilder> fromXContent(QueryParseContext parseContext) throws IOException {
        throw new UnsupportedOperationException("querybuilders does not support this operation.");
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(center, geoDistance, optimizeBbox, distance, validationMethod, ignoreUnmapped);
    }

    @Override
    protected boolean doEquals(GeoDistanceQueryBuilder other) {
        return Objects.equals(fieldName, other.fieldName) &&
                (distance == other.distance) &&
                Objects.equals(validationMethod, other.validationMethod) &&
                Objects.equals(center, other.center) &&
                Objects.equals(optimizeBbox, other.optimizeBbox) &&
                Objects.equals(geoDistance, other.geoDistance) &&
                Objects.equals(ignoreUnmapped, other.ignoreUnmapped);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }
}
