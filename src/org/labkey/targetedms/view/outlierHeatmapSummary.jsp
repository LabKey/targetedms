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
    <input type="date" id="start-date">
    <label for="end-date">End Date:</label>
    <input type="date" id="end-date">
</div>

<div class="legend-container">
    <div class="legend-label arrow-left">No Outliers</div>
    <div class="legend"></div>
    <div class="legend-label arrow-right">Most Outliers</div>
</div>


<%--<table id="heatmap-table" class="heatmap-table">--%>
<%--    <thead>--%>
<%--    <tr>--%>
<%--        <th>Category</th>--%>
<%--        <th>Value</th>--%>
<%--    </tr>--%>
<%--    </thead>--%>
<%--    <tbody>--%>
<%--    <tr><td>Item 1</td><td>50</td></tr>--%>
<%--    <tr><td>Item 2</td><td>40</td></tr>--%>
<%--    <tr><td>Item 3</td><td>30</td></tr>--%>
<%--    <tr><td>Item 4</td><td>20</td></tr>--%>
<%--    <tr><td>Item 5</td><td>10</td></tr>--%>
<%--    </tbody>--%>
<%--</table>--%>

<div id="table-container">


</div>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">

    const heatmapData = [{
        category: 'Category 1',
        value: 50
    }, {
        category: 'Category 2',
        value: 40
    }, {
        category: 'Category 3',
        value: 30
    }, {
        category: 'Category 4',
        value: 20
    }, {
        category: 'Category 5',
        value: 10
    }];

    function generateTable(data) {
        const tableContainer = document.getElementById('table-container');
        let tableHTML = '<table id="heatmap-table" class="heatmap-table">';
        tableHTML += '<thead><tr><th>Category</th><th>Value</th></tr></thead><tbody>';

        data.forEach(row => {
            tableHTML += "<tr><td>" + row.category + "</td><td>" + row.value + "</td></tr>";
        });

        tableHTML += '</tbody></table>';
        tableContainer.innerHTML = tableHTML;

        applyHeatmapColors();
    }

    function getData() {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetPeptideOutliers.api'),
            success: function (response) {
                generateTable(heatmapData);
            }
        });
    }

    function applyHeatmapColors() {
        const table = document.getElementById('heatmap-table');
        const values = [];

        // Extract values from table cells
        for (let row of table.rows) {
            for (let cell of row.cells) {
                const value = parseFloat(cell.textContent);
                if (!isNaN(value)) {
                    values.push(value);
                }
            }
        }

        const min = Math.min(...values);
        const max = Math.max(...values);

        // Apply colors based on values
        for (let row of table.rows) {
            for (let cell of row.cells) {
                const value = parseFloat(cell.textContent);
                if (!isNaN(value)) {
                    cell.style.backgroundColor = getColor(value, min, max);
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

    document.addEventListener('DOMContentLoaded', function () {
        const dateRangeSelect = document.getElementById('date-range');
        const datePickerContainer = document.getElementById('date-picker-container');

        dateRangeSelect.addEventListener('change', function () {
            if (dateRangeSelect.value == '-1') {  // Custom range selected
                datePickerContainer.style.display = 'block';
            } else {
                datePickerContainer.style.display = 'none';
            }
        });
    });

    Ext4.onReady(getData);

</script>