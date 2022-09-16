/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.targetedms.clustergrammer;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.labkey.api.util.StringUtilsLabKey;
import org.springframework.validation.BindException;

import java.util.Map;
import java.util.stream.Collectors;

import static org.labkey.api.action.SpringActionController.ERROR_MSG;

/**
 * Created by iansigmon on 4/7/16.
 */
public class ClustergrammerClient
{
    // TSV upload-based endpoint
    private static final String CLUSTERGRAMMER_ENDPOINT = "https://maayanlab.cloud/clustergrammer/upload_network/";

    public String generateHeatMap(ClustergrammerHeatMap matrix, BindException errors) throws Exception
    {
        // Configure CloseableHttpClient to not follow redirects
        try (CloseableHttpClient httpclient = HttpClients.custom().disableRedirectHandling().build())
        {
            HttpPost post = new HttpPost(CLUSTERGRAMMER_ENDPOINT);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.LEGACY);
            builder.addBinaryBody("file", serializeMatrix(matrix.getMatrix()).getBytes(StringUtilsLabKey.DEFAULT_CHARSET), ContentType.DEFAULT_BINARY, matrix.getTitle() + ".txt");

            post.setEntity(builder.build());

            try (CloseableHttpResponse response = httpclient.execute(post))
            {
                if (response.getCode() == HttpStatus.SC_MOVED_TEMPORARILY)
                {
                    Header header = response.getFirstHeader("Location");
                    if (header != null)
                    {
                        return header.getValue();
                    }
                }

                EntityUtils.consume(response.getEntity());
                errors.reject(ERROR_MSG, "Request to Clustergrammer failed:\n " + response.getCode() +": " + response.getReasonPhrase());
            }
        }

        return null;
    }

    /**
     * Convert to a "Simple Matrix Format" TSV for Clustergrammer
     * https://clustergrammer.readthedocs.io/matrix_format_io.html
     */
    private String serializeMatrix(Map<String, Map<String, Double>> matrix)
    {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (String proteinName : matrix.keySet())
        {
            Map<String, Double> values = matrix.get(proteinName);
            if (first)
            {
                sb.append("\t");
                sb.append(StringUtils.join(values.keySet(), "\t"));
                first = false;
            }
            sb.append("\n");
            sb.append(proteinName.replace("\t", " ").trim());
            sb.append("\t");
            sb.append(StringUtils.join(values.values().stream().map(x -> x != null ? String.format("%E", x) : "").collect(Collectors.toList()), "\t"));
        }

        return sb.toString();
    }
}
