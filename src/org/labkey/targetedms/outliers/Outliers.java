/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.targetedms.outliers;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryService;
import org.labkey.api.security.User;
import org.labkey.targetedms.TargetedMSSchema;
import org.labkey.targetedms.model.QCMetricConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Outliers
{

    protected static String METRIC_NAME = "$METRICNAME_";
    protected static String METRIC_LABEL_ONE = "$METRICLABEL1_";
    protected static String METRIC_LABEL_TWO = "$METRICLABEL2_";

    protected static Map<String, Object> _namedParameters;

    /**
     * LabKey Sql executor
     * @param container: container
     * @param user: user
     * @param sql: labkey sql to execute
     * @param columnNames: set of column names
     * @param sort: Sort object
     * @return retuns map collection
     */
    public static TableSelector executeQuery(Container container, User user, String sql, Set<String> columnNames, Sort sort, @Nullable Map<String, Object> namedParameters)
    {
        QuerySchema query = DefaultSchema.get(user, container).getSchema(TargetedMSSchema.SCHEMA_NAME);
        assert query != null;
        TableSelector tableSelector = QueryService.get().selector(query, sql, columnNames, null, sort);
        if (null != namedParameters)
        {
            tableSelector.setNamedParameters(namedParameters);
        }
        return tableSelector;
    }

    protected static String getExclusionWhereSql(int metricId)
    {
        return " WHERE SampleFileId.ReplicateId NOT IN "
                + "(SELECT ReplicateId FROM QCMetricExclusion WHERE MetricId IS NULL OR MetricId = " + metricId + ")";
    }

    public static Map<String, Object> getNamedParametersForQCConfigurations()
    {
        return _namedParameters;
    }

    public static void setNamedParametersForQCConfigurations(List<QCMetricConfiguration> configurations)
    {
        Map<String, Object> namedParameters = new HashMap<>();
        configurations.forEach(config -> {
            String nameKey = METRIC_NAME + config.getId() + "$";
            namedParameters.put(nameKey, config.getName());

            if (null != config.getSeries1Label())
            {
                String labelKey = METRIC_LABEL_ONE + config.getId() + "$";
                namedParameters.put(labelKey, config.getSeries1Label());
            }

            if (null != config.getSeries2Label())
            {
                String labelKey = METRIC_LABEL_TWO + config.getId() + "$";
                namedParameters.put(labelKey, config.getSeries2Label());
            }
        });

        _namedParameters = namedParameters;
    }

    protected static String addParametersToSQL(List<QCMetricConfiguration> configurations)
    {
        StringBuilder parameters = new StringBuilder();
        StringBuilder sb = new StringBuilder();

        sb.append("PARAMETERS (");
        configurations.forEach(config ->
        {
            sb.append(METRIC_NAME + config.getId() +"$ VARCHAR, ");
            if (null != config.getSeries1Label())
            {
                sb.append(METRIC_LABEL_ONE + config.getId() + "$ VARCHAR, \n");
            }

            if (null != config.getSeries2Label())
            {
                sb.append(METRIC_LABEL_TWO + config.getId() + "$ VARCHAR, \n");
            }
        });

        //stripping off last comma from the loop above
        int parameterLen  = sb.length();
        parameters.append(sb.toString().substring(0, parameterLen-3));

        parameters.append(")");
        return parameters.toString();

    }
}
