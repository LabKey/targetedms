<%
/*
 * Copyright (c) 2014-2026 LabKey Corporation
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
%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController" %>
<%@ page import="org.labkey.targetedms.parser.Molecule" %>
<%@ page import="org.labkey.targetedms.parser.Peptide" %>
<%@ page import="org.labkey.targetedms.parser.Replicate" %>
<%@ page import="org.labkey.targetedms.parser.ReplicateAnnotation" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("TargetedMS/js/svgChart.js");
        dependencies.add("TargetedMS/css/svgChart.css");
    }
%>
<%
    JspView<TargetedMSController.SummaryChartBean> me = HttpView.currentView();
    TargetedMSController.SummaryChartBean bean = me.getModelBean();
    long peptideGroupId = bean.getPeptideGroupId(); // Used when displaying peak areas for all peptides of a protein

    List<Replicate> replicateList = bean.getReplicateList();
    List<String> replicateAnnotationNameList = bean.getReplicateAnnotationNameList();
    List<ReplicateAnnotation> replicateAnnotationValueList = bean.getReplicateAnnotationValueList();

    ActionURL peakAreaUrl = new ActionURL(TargetedMSController.ShowPeakAreasAction.class, getContainer());
    ActionURL retentionTimesUrl = new ActionURL(TargetedMSController.ShowRetentionTimesChartAction.class, getContainer());

    ActionURL replicateListAction = new ActionURL(TargetedMSController.ShowReplicatesAction.class, getContainer());
    replicateListAction.addParameter("id", bean.getRun().getId());

    // for proteomics summary charts
    List<Peptide> peptideList = bean.getPeptideList();
    long peptideId = bean.getPeptideId(); // Used when displaying peak areas for a single peptide in multiple replicates
                                         // or grouped by replicate annotation.
    long precursorId = bean.getPrecursorId(); // Used when displaying peak areas for a single precursor

    // for small molecule summary charts
    List<Molecule> moleculeList = bean.getMoleculeList();
    long moleculeId = bean.getMoleculeId();
    long moleculePrecursorId = bean.getMoleculePrecursorId();

    boolean asProteomics = (peptideList != null && !peptideList.isEmpty()) || peptideId != 0 || precursorId != 0;

    if (asProteomics)
    {
        peakAreaUrl.addParameter("asProteomics", true);
        retentionTimesUrl.addParameter("asProteomics", true);
    }

    if (peptideGroupId != 0)
    {
        peakAreaUrl.addParameter("peptideGroupId", peptideGroupId);
        retentionTimesUrl.addParameter("peptideGroupId", peptideGroupId);
    }
    else if (peptideId != 0)
    {
        peakAreaUrl.addParameter("peptideId", peptideId);
        retentionTimesUrl.addParameter("peptideId", peptideId);
    }
    else if (moleculeId != 0)
    {
        peakAreaUrl.addParameter("moleculeId", moleculeId);
        retentionTimesUrl.addParameter("moleculeId", moleculeId);
    }

    if (precursorId != 0)
    {
        peakAreaUrl.addParameter("precursorId", precursorId);
        retentionTimesUrl.addParameter("precursorId", precursorId);
    }
    else if (moleculePrecursorId != 0)
    {
        peakAreaUrl.addParameter("moleculePrecursorId", moleculePrecursorId);
        retentionTimesUrl.addParameter("moleculePrecursorId", moleculePrecursorId);
    }
    peakAreaUrl.addParameter("chartWidth", bean.getInitialWidth());
    peakAreaUrl.addParameter("chartHeight", bean.getInitialHeight());
    retentionTimesUrl.addParameter("chartWidth", bean.getInitialWidth());
    retentionTimesUrl.addParameter("chartHeight", bean.getInitialHeight());

    // The ExtJS stores prepended "All"/"None" entries, so the original visibility checks were
    // count-based (e.g. store.count() > 2). Translate those to list-size checks here.
    boolean hasPeptides = peptideList != null && !peptideList.isEmpty();
    boolean hasMolecules = moleculeList != null && !moleculeList.isEmpty();
    boolean showReplicate = replicateList.size() > 1;
    boolean showAnnotName = !replicateAnnotationNameList.isEmpty();
    boolean showAnnotValue = !replicateAnnotationValueList.isEmpty();
    boolean showPeptide = hasPeptides && peptideList.size() > 1;
    boolean showMolecule = hasMolecules && moleculeList.size() > 1;
    boolean showCv = showReplicate || showAnnotName;
%>
<style>
    .summary_form_box {
        padding-bottom: 25px;
    }

    .summary_title_box {
        border: #ccc 1px solid;
        float: left;
        margin-right: 1em;
        margin-bottom: 1em;
    }

    .summary_title_box .title {
        position: relative;
        top : -0.6em;
        margin-left: 20px;
        display: inline;
        background-color: white;
        font-size: 18px;
        padding: 0 5px;
    }
    .valuelabel
    {
        color: #000000;
        font-size: 10px;
        padding-left:105px;
    }
    .summary-form-table {
        border-collapse: collapse;
    }
    .summary-form-table td {
        padding: 3px 4px;
        vertical-align: middle;
    }
    .summary-form-table td.sc-label {
        background-color: #E0E6EA;
        width: 150px;
    }
    .summary-form-table select {
        width: 400px;
    }
    .summary-form-table input[type=number] {
        width: 100px;
    }
</style>
<div>
    <div id="peakAreasFormDiv" class="summary_form_box" style="display: <%=h(bean.isShowControls() ? "block" : "none")%>;">
        <table class="summary-form-table">
            <tr id="sc-row-replicate" style="display: <%=h(showReplicate ? "table-row" : "none")%>;">
                <td class="sc-label"><label for="sc-replicate">Replicate</label></td>
                <td>
                    <select id="sc-replicate">
                        <option value="0">All</option>
                        <% for (Replicate r : replicateList) { %>
                            <option value="<%=r.getId()%>"><%=h(r.getName())%></option>
                        <% } %>
                    </select>
                </td>
            </tr>
            <tr id="sc-row-annotName" style="display: <%=h(showAnnotName ? "table-row" : "none")%>;">
                <td class="sc-label"><label for="sc-annotName">Group By</label></td>
                <td>
                    <select id="sc-annotName">
                        <option value="None">None</option>
                        <% for (String annotName : replicateAnnotationNameList) { %>
                            <option value="<%=h(annotName)%>"><%=h(annotName)%></option>
                        <% } %>
                    </select>
                </td>
            </tr>
            <tr id="sc-row-annotValue" style="display: <%=h(showAnnotValue ? "table-row" : "none")%>;">
                <td class="sc-label"><label for="sc-annotValue">Filter</label></td>
                <td>
                    <select id="sc-annotValue">
                        <option value="None">None</option>
                        <% for (ReplicateAnnotation annotValue : replicateAnnotationValueList) { %>
                            <option value="<%=h(annotValue.getDisplayName())%>"><%=h(annotValue.getDisplayName())%></option>
                        <% } %>
                    </select>
                </td>
            </tr>
            <tr id="sc-row-peptide" style="display: <%=h(showPeptide ? "table-row" : "none")%>;">
                <td class="sc-label"><label for="sc-peptide">Peptide</label></td>
                <td>
                    <select id="sc-peptide">
                        <option value="0">All</option>
                        <% if (peptideList != null) { for (Peptide p : peptideList) { %>
                            <option value="<%=p.getId()%>"><%=h(p.getPeptideModifiedSequence())%></option>
                        <% } } %>
                    </select>
                </td>
            </tr>
            <tr id="sc-row-molecule" style="display: <%=h(showMolecule ? "table-row" : "none")%>;">
                <td class="sc-label"><label for="sc-molecule">Small Molecule</label></td>
                <td>
                    <select id="sc-molecule">
                        <option value="0">All</option>
                        <% if (moleculeList != null) { for (Molecule mol : moleculeList) { %>
                            <option value="<%=mol.getId()%>"><%=h(mol.getCustomIonName())%></option>
                        <% } } %>
                    </select>
                </td>
            </tr>
            <tr id="sc-row-cv" style="display: <%=h(showCv ? "table-row" : "none")%>;">
                <td class="sc-label"><label for="sc-cv">CV Values</label></td>
                <td><input type="checkbox" id="sc-cv"></td>
            </tr>
            <tr>
                <td class="sc-label"><label for="sc-log">Log Scale</label></td>
                <td><input type="checkbox" id="sc-log"></td>
            </tr>
            <tr>
                <td class="sc-label"><label for="sc-width">Width</label></td>
                <td><input type="number" id="sc-width" value="<%= bean.getInitialWidth() %>" min="1" step="1"></td>
            </tr>
            <tr>
                <td class="sc-label"><label for="sc-height">Height</label></td>
                <td><input type="number" id="sc-height" value="<%= bean.getInitialHeight() %>" min="1" step="1"></td>
            </tr>
            <tr>
                <td class="sc-label"><label for="sc-value">Value</label></td>
                <td>
                    <select id="sc-value">
                        <option value="All">All</option>
                        <option value="Retention Time">Retention Time</option>
                        <option value="FWHM">FWHM</option>
                        <option value="FWB">FWB</option>
                    </select>
                </td>
            </tr>
            <tr>
                <td></td>
                <td><span class="valuelabel">Value only effects Retention Times chart.</span></td>
            </tr>
            <tr>
                <td></td>
                <td><button type="button" id="sc-update" class="labkey-button">Update</button></td>
            </tr>
        </table>
    </div>
    <div class="summary_title_box">
        <h3 class="title">Peak Areas</h3>
        <div id="peakAreasGraph"></div>
        <a href="<%= h(replicateListAction)%>"><div style="text-align: center" id="peakAreasGraphLabel"></div></a>
    </div>
    <div class="summary_title_box">
        <h3 class="title">Retention Times</h3>
        <div id="retentionTimesGraph"></div>
        <a href="<%= h(replicateListAction)%>"><div style="text-align: center" id="retentionTimesGraphLabel"></div></a>
    </div>
</div>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    LABKEY.Utils.onReady(function() {

        const byId = function(id) { return document.getElementById(id); };

        const replicateSel = byId('sc-replicate');
        const annotNameSel = byId('sc-annotName');
        const annotValueSel = byId('sc-annotValue');
        const valueSel = byId('sc-value');
        const peptideSel = byId('sc-peptide');
        const moleculeSel = byId('sc-molecule');
        const cvChk = byId('sc-cv');
        const logChk = byId('sc-log');
        const widthInput = byId('sc-width');
        const heightInput = byId('sc-height');
        const updateBtn = byId('sc-update');

        const showReplicate = <%=showReplicate%>;
        const showAnnotName = <%=showAnnotName%>;
        const showCv = <%=showCv%>;
        const hasPeptides = <%=hasPeptides%>;
        const hasMolecules = <%=hasMolecules%>;

        function updateCvCheckbox() {
            if (!showCv) {
                return;
            }
            const allReplicates = replicateSel.value === '0'; // replicate = 'All'
            const noAnnotations = annotNameSel.value === 'None'; // annotation = 'None'

            const replicateEnabled = showReplicate && !replicateSel.disabled;
            const annotEnabled = showAnnotName && !annotNameSel.disabled;

            if ((replicateEnabled && allReplicates) || (annotEnabled && !noAnnotations)) {
                cvChk.disabled = false;
            }
            else {
                cvChk.checked = false;
                cvChk.disabled = true;
            }
        }

        replicateSel.addEventListener('change', function() {
            if (replicateSel.value !== '0') {
                annotNameSel.disabled = true; annotNameSel.value = 'None';
                peptideSel.disabled = true; peptideSel.value = '0';
                moleculeSel.disabled = true; moleculeSel.value = '0';
                annotValueSel.disabled = true; annotValueSel.value = 'None';
            }
            else {
                annotNameSel.disabled = false;
                peptideSel.disabled = false;
                moleculeSel.disabled = false;
                annotValueSel.disabled = false;
            }
            updateCvCheckbox();
        });

        annotNameSel.addEventListener('change', updateCvCheckbox);
        valueSel.addEventListener('change', updateCvCheckbox);
        annotValueSel.addEventListener('change', updateCvCheckbox);

        peptideSel.addEventListener('change', function() {
            if (peptideSel.value !== '0') {
                replicateSel.value = '0';
                replicateSel.disabled = true;
            }
            else {
                replicateSel.disabled = false;
            }
            updateCvCheckbox();
        });

        moleculeSel.addEventListener('change', function() {
            if (moleculeSel.value !== '0') {
                replicateSel.value = '0';
                replicateSel.disabled = true;
            }
            else {
                replicateSel.disabled = false;
            }
            updateCvCheckbox();
        });

        updateBtn.addEventListener('click', function() {

            const params = {
                asProteomics: <%=asProteomics%>,
                peptideGroupId: <%=peptideGroupId%>,
                replicateId: replicateSel.value,
                groupByReplicateAnnotName: annotNameSel.value,
                filterByReplicateAnnotName: annotValueSel.value,
                peptideId: hasPeptides ? peptideSel.value : <%=peptideId%>,
                moleculeId: hasMolecules ? moleculeSel.value : <%=moleculeId%>,
                cvValues: cvChk.checked,
                logValues: logChk.checked,
                chartWidth: widthInput.value,
                chartHeight: heightInput.value
            };

            const peakAreaUrl = LABKEY.ActionURL.buildURL(
                    'targetedms',  // controller
                    'showPeakAreas', // action
                    LABKEY.ActionURL.getContainer(),
                    params
            );

            const retentionTimeParams = Object.assign({}, params, {value: valueSel.value});

            const retentionTimesUrl = LABKEY.ActionURL.buildURL(
                    'targetedms',  // controller
                    'showRetentionTimesChart', // action
                    LABKEY.ActionURL.getContainer(),
                    retentionTimeParams
            );

            // change the src of the image
            const areaElement = byId('peakAreasGraph');
            LABKEY.targetedms.SVGChart.requestAndRenderSVG(peakAreaUrl, areaElement, null, byId('peakAreasGraphLabel'));
            const timeElement = byId('retentionTimesGraph');
            LABKEY.targetedms.SVGChart.requestAndRenderSVG(retentionTimesUrl, timeElement, null, byId('retentionTimesGraphLabel'));

            areaElement.style.width = parseInt(widthInput.value) + 'px';
            areaElement.style.height = parseInt(heightInput.value) + 'px';
            timeElement.style.width = parseInt(widthInput.value) + 'px';
            timeElement.style.height = parseInt(heightInput.value) + 'px';
        });

        // peak areas / retention times graphs
        LABKEY.targetedms.SVGChart.requestAndRenderSVG(<%= q(peakAreaUrl) %>, byId('peakAreasGraph'), null, byId('peakAreasGraphLabel'));
        LABKEY.targetedms.SVGChart.requestAndRenderSVG(<%= q(retentionTimesUrl) %>, byId('retentionTimesGraph'), null, byId('retentionTimesGraphLabel'));

        updateCvCheckbox();
    });
</script>
