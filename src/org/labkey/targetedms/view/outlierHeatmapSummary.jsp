<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%
    JspView<JSONObject> me = (JspView<JSONObject>) HttpView.currentView();
    JSONObject bean = me.getModelBean();
%>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
        dependencies.add("internal/jQuery");
    }
%>

<style>
  .date-picker-container {
    display: none;
    margin-top: 10px;
  }

  .legend-container {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 20px;
  }
  .legend {
    flex-grow: 1;
    height: 30px;
    background: linear-gradient(to right, white, red);
    position: relative;
  }
  .legend-label {
    width: 150px;
    text-align: center;

  }
  .arrow-left::before, .arrow-right::after {
    content: '';
    display: inline-block;
    width: 0;
    height: 0;
    border-style: solid;
  }
  .arrow-left::before {
    border-width: 15px 20px 15px 0;
    border-color: transparent black transparent transparent;
  }
  .arrow-right::after {
    border-width: 15px 0 15px 20px;
    border-color: transparent black transparent black;
  }

  .heatmap-table {
    border-collapse: collapse;
    width: 100%;
  }

  .heatmap-table th, .heatmap-table td {
    border: 1px solid black;
    padding: 8px;
    text-align: center;
  }
</style>

<label for="date-range">Date Range:</label>
<select id="date-range">
    <option value="0">All dates</option>
    <option value="7">Last 7 days</option>
    <option value="15">Last 15 days</option>
    <option value="30">Last 30 days</option>
    <option value="90">Last 90 days</option>
    <option value="180">Last 180 days</option>
    <option value="365">Last 365 days</option>
    <option value="-1">Custom range</option>
</select>

<div id="date-picker-container" class="date-picker-container">
    <label for="start-date">Start Date:</label>
    <% addHandler("start-date", "change", "customDateRange()"); %>
    <input type="date" id="start-date">
    <label for="end-date">End Date:</label>
    <% addHandler("end-date", "change", "customDateRange()"); %>
    <input type="date" id="end-date">
</div>

<div class="legend-container">
    <div class="legend-label arrow-left">&nbsp; &nbsp;No Outliers</div>
    <div class="legend"></div>
    <div class="legend-label arrow-right">Most Outliers &nbsp; &nbsp;</div>
    <div>Total Replicates : </div>
    <div id="total-replicates"></div>
</div>

<div id="table-container">

</div>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    let startDate;
    let endDate;
    let minDate;
    let maxDate;

    function getMinMaxDate() {
        LABKEY.Query.executeSql({
            schemaName: 'targetedms',
            sql: 'SELECT MIN(AcquiredTime) AS MinAcquiredTime, MAX(AcquiredTime) AS MaxAcquiredTime, count(*) AS runs FROM SampleFile',
            success: function(data) {
                if (data.rows.length === 0 || !data.rows[0]['MinAcquiredTime']) {
                    Ext4.get(plotPanelId).update("No data found. Please upload runs using the Data Pipeline or directly from Skyline.");
                }
                else {
                    startDate = data.rows[0]['MinAcquiredTime'] ? new Date(data.rows[0]['MinAcquiredTime']) : null;
                    minDate = data.rows[0]['MinAcquiredTime'] ? new Date(data.rows[0]['MinAcquiredTime']) : null;
                    endDate = data.rows[0]['MaxAcquiredTime'] ? new Date(data.rows[0]['MaxAcquiredTime']) : null;
                    maxDate = data.rows[0]['MaxAcquiredTime'] ? new Date(data.rows[0]['MaxAcquiredTime']) : null;
                    getData();
                }
            },
            failure: function(response) {
                Ext4.get(plotPanelId).update("<span class='labkey-error'>Error: " + response.exception + "</span>");
            }
        });
    }

    function calculateStartDateByOffset(offset) {
        if (offset > 0) {
            var startDateByOffset = maxDate ? new Date(maxDate) : new Date();
            startDateByOffset.setDate(startDateByOffset.getDate() - offset);
            return startDateByOffset;
        }

        return minDate;
    }

    function calculateEndDateByOffset(offset) {
        if (offset > 0)
            return maxDate ? maxDate : new Date();

        return maxDate;
    }

    function generateTable(data) {
        const tableContainer = document.getElementById('table-container');
        let tableHTML = '<table id="heatmap-table" class="heatmap-table">';
        tableHTML += '<thead><tr><th></th>';

        for (let i = 0; i < data.metrics.length; i++) {
            tableHTML += '<th>' + data.metrics[i] + '</th>';
        }

        tableHTML += '</tr></thead><tbody>';

        let totalOutliersByMetric = {};
        for (let i = 0; i < data.peptideOutliers.length; i++) {
            tableHTML += '<tr><td>' + data.peptideOutliers[i].peptide + '</td>';

            for (let j = 0; j < data.metrics.length; j++) {
                let metric = data.metrics[j];
                if (data.peptideOutliers[i].outlierCountsPerMetric[metric]) {
                    tableHTML += '<td>' + data.peptideOutliers[i].outlierCountsPerMetric[metric] + '</td>';
                    if (!totalOutliersByMetric[metric]) {
                        totalOutliersByMetric[metric] = 0;
                    }

                    totalOutliersByMetric[metric] += data.peptideOutliers[i].outlierCountsPerMetric[metric];

                }
                else {
                    tableHTML += '<td>0</td>';
                }
            }
            tableHTML += '</tr>';
        }

        tableHTML += '<tr><td><b>Total</b></td>';
        for (let i = 0; i < data.metrics.length; i++) {
            if (totalOutliersByMetric[data.metrics[i]]) {
                tableHTML += '<td><b>' + totalOutliersByMetric[data.metrics[i]] + '</b></td>';
            }
            else {
                tableHTML += '<td><b>0</b></td>';
            }
        }
        tableHTML += '</tbody></table>';
        tableContainer.innerHTML = tableHTML;

        applyHeatmapColors();
    }

    function init() {
        if (!startDate || !endDate) {
            getMinMaxDate();
        }
    }

    function getData() {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetPeptideOutliers.api'),
            params: {
                startDate: startDate,
                endDate: endDate
            },
            success: function (response) {
                const parsed = JSON.parse(response.responseText);
                document.getElementById('total-replicates').textContent = parsed.replicatesCount
                generateTable(parsed);
            }
        });
    }

    function applyHeatmapColors() {
        const table = document.getElementById('heatmap-table');
        const values = [];

        // Extract values from table cells, excluding the last row (totals row)
        for (let i = 1; i < table.rows.length - 1; i++) { // start from 1 to skip header
            const row = table.rows[i];
            for (let j = 1; j < row.cells.length; j++) { // start from 1 to skip the first column
                const value = parseFloat(row.cells[j].textContent);
                if (!isNaN(value)) {
                    values.push(value);
                }
            }
        }

        const min = Math.min(...values);
        const max = Math.max(...values);

        // Apply colors based on values, excluding the last row (totals row)
        for (let i = 1; i < table.rows.length - 1; i++) { // start from 1 to skip header
            const row = table.rows[i];
            for (let j = 1; j < row.cells.length; j++) { // start from 1 to skip the first column
                const value = parseFloat(row.cells[j].textContent);
                if (!isNaN(value)) {
                    row.cells[j].style.backgroundColor = getColor(value, min, max);
                }
            }
        }
    }

    function getColor(value, min, max) {
        const percent = (value - min) / (max - min);
        const red = 255;
        const green = Math.floor(255 * (1 - percent));
        const blue = Math.floor(255 * (1 - percent));
        return 'rgb(' + red + ',' + green + ',' + blue + ')';
    }

    function customDateRange() {
        if (document.getElementById("start-date").value) {
            startDate = new Date(document.getElementById("start-date").value);
        }
        if (document.getElementById("end-date").value) {
            endDate = new Date(document.getElementById("end-date").value);
        }
        if (document.getElementById("start-date").value && document.getElementById("end-date").value) {
            getData();
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        const dateRangeSelect = document.getElementById('date-range');
        const datePickerContainer = document.getElementById('date-picker-container');

        dateRangeSelect.addEventListener('change', function () {
            if (dateRangeSelect.value == '-1') {  // Custom range selected
                datePickerContainer.style.display = 'block';
            } else {
                datePickerContainer.style.display = 'none';
                startDate = new Date(calculateStartDateByOffset(parseInt(dateRangeSelect.value)));
                endDate = new Date(calculateEndDateByOffset(parseInt(dateRangeSelect.value)));
                getData();
            }
        });
    });

    Ext4.onReady(init);

</script>