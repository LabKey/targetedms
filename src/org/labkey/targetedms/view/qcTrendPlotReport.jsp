<%
/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

/**
* User: cnathe
* Date: Sept 19, 2011
*/

%>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("Ext4ClientApi");
        dependencies.add("vis/vis");
        dependencies.add("internal/tippy");
        dependencies.add("targetedms/css/SVGExportIcon.css");
        dependencies.add("targetedms/css/qcTrendPlotReport.css");
        dependencies.add("targetedms/js/QCPlotHelperBase.js");
        dependencies.add("targetedms/js/QCPlotLegendHelper.js");
        dependencies.add("targetedms/js/misc.js");
        dependencies.add("targetedms/js/LeveyJenningsPlotHelper.js");
        dependencies.add("targetedms/js/TrailingMeanPlotHelper.js");
        dependencies.add("targetedms/js/TrailingCVPlotHelper.js");
        dependencies.add("targetedms/js/CUSUMPlotHelper.js");
        dependencies.add("targetedms/js/MovingRangePlotHelper.js");
        dependencies.add("targetedms/js/QCPlotHelperWrapper.js");
        dependencies.add("targetedms/js/BaseQCPlotPanel.js");
        dependencies.add("targetedms/js/QCTrendPlotPanel.js");
        dependencies.add("targetedms/js/QCPlotHoverPanel.js");
        dependencies.add("targetedms/js/PlotTypeCheckCombo.js");
        dependencies.add("targetedms/js/QCMetricConfigLoader.js");
    }
%>
<%
    int uid = getRequestScopedUID();
    String reportPanelId = "reportHeaderPanel-" + uid;
    String plotPanelId = "tiledPlotPanel-" + uid;
    String plotPaginationPanelId = "plotPaginationPanel-" + uid;
%>
<!-- Help ExtJS plot controls reliably grab the width they need -->
<div style="height: 1px; width: 1250px;"></div>
<div id="<%=h(reportPanelId)%>"></div>
<div id="<%=h(plotPaginationPanelId)%>" class="plotPaginationHeaderPanel"></div>
<div id="<%=h(plotPanelId)%>" class="tiledPlotPanel"></div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
        function init() {
            var reportPanelId = <%=q(reportPanelId)%>;
            var plotPanelId = <%=q(plotPanelId)%>;
            var plotPaginationPanelId = <%=q(plotPaginationPanelId)%>;

            LABKEY.Query.executeSql({
                schemaName: 'targetedms',
                sql: 'SELECT MIN(AcquiredTime) AS MinAcquiredTime, MAX(AcquiredTime) AS MaxAcquiredTime, count(*) AS runs FROM SampleFile',
                success: function(data) {
                    if (data.rows.length === 0 || !data.rows[0]['MinAcquiredTime']) {
                        Ext4.get(plotPanelId).update("<span>No data found. Please upload runs using the Data Pipeline or directly from Skyline.</span>");
                    }
                    else {
                        initializeReportPanels(data, reportPanelId, plotPanelId, plotPaginationPanelId);
                    }
                },
                failure: function(response) {
                    Ext4.get(plotPanelId).update("<span class='labkey-error'>Error: " + LABKEY.Utils.encodeHtml(response.exception) + "</span>");
                }
            });
        }

        function initializeReportPanels(data, reportPanelId, plotPanelId, plotPaginationPanelId)
        {
            // initialize the panel that displays the Levey-Jennings plot for trend plotting
            Ext4.create('LABKEY.targetedms.QCTrendPlotPanel', {
                renderTo: reportPanelId,
                cls: 'qc-trend-plot-panel',
                plotDivId: plotPanelId,
                plotPaginationDivId: plotPaginationPanelId,
                minAcquiredTime: data.rows[0]['MinAcquiredTime'] ? new Date(data.rows[0]['MinAcquiredTime']) : null,
                maxAcquiredTime: data.rows[0]['MaxAcquiredTime'] ? new Date(data.rows[0]['MaxAcquiredTime']) : null,
                runs: data.rows[0]['runs']
            });
        }

        let plotTypeTooltipInstance = null;

        function createPlotTypeTooltip(tgt, plotType) {
            destroyPlotTypeTooltip();

            const title = plotType.trim() + ' Plot Type';
            const content = getPlotTypeHelpTooltip(plotType.trim());

            plotTypeTooltipInstance = tippy(tgt, {
                content: '<div style="padding: 15px;">' +
                         '<div style="font-size: 18px; font-weight: bold; margin-bottom: 10px; color: #000;">' +
                         LABKEY.Utils.encodeHtml(title) + '</div><div style="font-size: 14px; line-height: 1.5; color: #000;">' +
                         content + '</div></div>',
                allowHTML: true,
                placement: 'top',
                maxWidth: 350,
                showOnCreate: true,
                trigger: 'manual',
                hideOnClick: false,
                offset: [-250, 10],
                onShow(instance) {
                    const tippyBox = instance.popper.querySelector('.tippy-box');
                    if (tippyBox) {
                        tippyBox.style.border = '5px solid #5d5c5c';
                        tippyBox.style.backgroundColor = 'white';
                        tippyBox.style.borderRadius = '4px';
                    }

                    const arrow = instance.popper.querySelector('.tippy-arrow');
                    if (arrow) {
                        arrow.style.color = '#5d5c5c';
                        arrow.style.width = '20px';
                        arrow.style.height = '20px';
                        const arrowBorder = arrow.querySelector('svg');
                        if (arrowBorder) {
                            arrowBorder.style.fill = '#5d5c5c';
                        }
                    }
                }
            });
        }

        function destroyPlotTypeTooltip() {
            if (plotTypeTooltipInstance) {
                plotTypeTooltipInstance.destroy();
                plotTypeTooltipInstance = null;
            }
        }

        function  getPlotTypeHelpTooltip(plotTypeName) {
            if (plotTypeName === 'Metric Value')
                return LABKEY.targetedms.LeveyJenningsPlotHelper.tooltips['Metric Value'];
            else if (plotTypeName === 'Moving Range')
                return LABKEY.targetedms.MovingRangePlotHelper.tooltips['Moving Range'];
            else if (plotTypeName === 'CUSUMm')
                return LABKEY.targetedms.CUSUMPlotHelper.tooltips['CUSUMm'];
            else if (plotTypeName === 'CUSUMv')
                return LABKEY.targetedms.CUSUMPlotHelper.tooltips['CUSUMv'];
            else if (plotTypeName === 'Trailing Mean')
                return LABKEY.targetedms.TrailingMeanPlotHelper.tooltips['Trailing Mean'];
            else if (plotTypeName === 'Trailing CV')
                return LABKEY.targetedms.TrailingCVPlotHelper.tooltips['Trailing CV'];
            return '';
        }

        Ext4.onReady(init);
</script>
