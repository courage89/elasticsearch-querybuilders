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

package org.codelibs.elasticsearch.rest.action.admin.cluster;

import org.codelibs.elasticsearch.ExceptionsHelper;
import org.codelibs.elasticsearch.action.admin.cluster.allocation.ClusterAllocationExplainRequest;
import org.codelibs.elasticsearch.action.admin.cluster.allocation.ClusterAllocationExplainResponse;
import org.codelibs.elasticsearch.client.node.NodeClient;
import org.codelibs.elasticsearch.common.bytes.BytesArray;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.common.xcontent.ToXContent;
import org.codelibs.elasticsearch.common.xcontent.XContentBuilder;
import org.codelibs.elasticsearch.common.xcontent.XContentParser;
import org.codelibs.elasticsearch.rest.BaseRestHandler;
import org.codelibs.elasticsearch.rest.BytesRestResponse;
import org.codelibs.elasticsearch.rest.RestController;
import org.codelibs.elasticsearch.rest.RestRequest;
import org.codelibs.elasticsearch.rest.RestResponse;
import org.codelibs.elasticsearch.rest.RestStatus;
import org.codelibs.elasticsearch.rest.action.RestBuilderListener;

import java.io.IOException;

/**
 * Class handling cluster allocation explanation at the REST level
 */
public class RestClusterAllocationExplainAction extends BaseRestHandler {

    @Inject
    public RestClusterAllocationExplainAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.GET, "/_cluster/allocation/explain", this);
        controller.registerHandler(RestRequest.Method.POST, "/_cluster/allocation/explain", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        ClusterAllocationExplainRequest req;
        if (request.hasContentOrSourceParam() == false) {
            // Empty request signals "explain the first unassigned shard you find"
            req = new ClusterAllocationExplainRequest();
        } else {
            try (XContentParser parser = request.contentOrSourceParamParser()) {
                req = ClusterAllocationExplainRequest.parse(parser);
            } catch (IOException e) {
                logger.debug("failed to parse allocation explain request", e);
                return channel -> channel.sendResponse(
                        new BytesRestResponse(ExceptionsHelper.status(e), BytesRestResponse.TEXT_CONTENT_TYPE, BytesArray.EMPTY));
            }
        }

        try {
            req.includeYesDecisions(request.paramAsBoolean("include_yes_decisions", false));
            req.includeDiskInfo(request.paramAsBoolean("include_disk_info", false));
            final boolean humanReadable = request.paramAsBoolean("human", false);
            return channel ->
                    client.admin().cluster().allocationExplain(req, new RestBuilderListener<ClusterAllocationExplainResponse>(channel) {
                @Override
                public RestResponse buildResponse(ClusterAllocationExplainResponse response, XContentBuilder builder) throws Exception {
                    builder.humanReadable(humanReadable);
                    response.getExplanation().toXContent(builder, ToXContent.EMPTY_PARAMS);
                    return new BytesRestResponse(RestStatus.OK, builder);
                }
            });
        } catch (Exception e) {
            logger.error("failed to explain allocation", e);
            return channel ->
                    channel.sendResponse(
                            new BytesRestResponse(ExceptionsHelper.status(e), BytesRestResponse.TEXT_CONTENT_TYPE, BytesArray.EMPTY));
        }
    }
}