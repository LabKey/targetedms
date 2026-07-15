<%
    /*
     * Copyright (c) 2026 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.targetedms.TargetedMSController.InstrumentUtilizationBean" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("targetedms/yearCalendar");
    }
%>
<%
    JspView<InstrumentUtilizationBean> me = HttpView.currentView();
    InstrumentUtilizationBean bean = me.getModelBean();
%>

<style nonce="<%=getScriptNonce()%>">
    /*
     * The LabKey page content column (.content-left) is a flexbox item. By default flex items have
     * min-width:auto, so they refuse to shrink below their widest child - a wide data region (e.g. the
     * Samples grid) therefore stretches the whole column and forces a page-level horizontal scrollbar.
     * Allowing the column to shrink to the viewport lets the per-grid overflow-x containers actually
     * scroll their own (wide) content instead of widening the page.
     */
    .content-left {
        min-width: 0;
    }
    /* Each data region scrolls horizontally within its own panel rather than widening the page */
    .content-left form[id^="lk-region-"] {
        overflow-x: auto;
    }
    /* Days with acquired samples drill into the Samples tab, so signal that they're clickable */
    #instrumentUtilizationCalendar .heatmap-shaded {
        cursor: pointer;
    }
</style>

<ul class="nav nav-tabs" id="utilizationTabs" role="tablist">
    <li class="active"><a href="#utilizationTabCalendar" data-utilization-tab="calendar">Calendar</a></li>
    <li><a href="#utilizationTabMonth" data-utilization-tab="month">Summary by Month</a></li>
    <li><a href="#utilizationTabDay" data-utilization-tab="day">Summary by Day</a></li>
    <li><a href="#utilizationTabSamples" data-utilization-tab="samples"><%=h(bean.getSampleFileTitle())%></a></li>
</ul>

<div class="tab-content" style="padding-top: 15px;">
    <div class="tab-pane active" id="utilizationTabCalendar">
        <div style="text-align: center; width: 100%">
            <label for="utilizationMonthNumberSelect">Display:</label>
            <select id="utilizationMonthNumberSelect">
                <option value="1">1 month</option>
                <option value="4">4 months</option>
                <option value="12">12 months</option>
            </select>
        </div>

        <div id="instrumentUtilizationCalendarWrapper" style="min-height: 300px; max-width: 100%; overflow-x: auto;">
            <div id="instrumentUtilizationCalendar">
                Loading...
            </div>
        </div>

        <div class="heatmap-footer-container">
            <div class="heatmap-legend-container">
                <div class="heatmap-legend-label" style="text-align: right">No data</div>
                <div class="heatmap-legend">
                    <div class="heatmap-legend-element"></div>
                    <div class="heatmap-legend-element heatmap-shade0"></div>
                    <div class="heatmap-legend-element heatmap-shade3"></div>
                    <div class="heatmap-legend-element heatmap-shade6"></div>
                    <div class="heatmap-legend-element heatmap-shade9"></div>
                    <div class="heatmap-legend-element heatmap-shade13"></div>
                </div>
                <div class="heatmap-legend-label" id="heatmapFileLegendMax">Files acquired</div>
            </div>
        </div>
    </div>

    <div class="tab-pane" id="utilizationTabMonth">
        <div id="utilizationByMonthGrid" style="max-width: 100%; overflow-x: auto;">
            <% me.include(bean.getByMonthView(), out); %>
        </div>
    </div>

    <div class="tab-pane" id="utilizationTabDay">
        <div id="utilizationByDayGrid" style="max-width: 100%; overflow-x: auto;">
            <% me.include(bean.getByDayView(), out); %>
        </div>
    </div>

    <div class="tab-pane" id="utilizationTabSamples">
        <div id="utilizationSamplesGrid" style="max-width: 100%; overflow-x: auto;">
            <% me.include(bean.getSampleFileView(), out); %>
        </div>
    </div>
</div>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
(function() {
    // Simple client-side tab switching. Panes are pre-rendered; we toggle Bootstrap's `active` class
    // (which drives .tab-pane visibility) so the calendar (default/active tab) initializes while
    // visible and the grids simply reveal on demand.
    const tabLinks = document.querySelectorAll('#utilizationTabs a[data-utilization-tab]');
    const panes = {
        calendar: document.getElementById('utilizationTabCalendar'),
        month: document.getElementById('utilizationTabMonth'),
        day: document.getElementById('utilizationTabDay'),
        samples: document.getElementById('utilizationTabSamples')
    };

    function activate(which) {
        for (const link of tabLinks) {
            const li = link.parentNode;
            li.classList.toggle('active', link.getAttribute('data-utilization-tab') === which);
        }
        for (const key in panes) {
            if (panes[key]) {
                panes[key].classList.toggle('active', key === which);
            }
        }
    }

    for (const link of tabLinks) {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            activate(link.getAttribute('data-utilization-tab'));
        });
    }

    // Shared with the calendar script: activate the Samples tab. The summary-grid count cells and the
    // calendar days are ordinary links to this page with utilizationTab=samples plus an AcquiredTime
    // filter, so following one reloads the page filtered; this reveals the Samples tab once we're back.
    window.showInstrumentSamplesTab = function() {
        activate('samples');
    };
})();
</script>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
(function() {
    let calendar = null;
    let maxFileCount = 0;
    let newestDataDate = new Date();

    const instrumentName = LABKEY.ActionURL.getParameter('name');

    // Following a drill-in link (a summary-grid count cell or a calendar day) reloads this page with
    // utilizationTab=samples and an AcquiredTime filter. Let the calendar render first (it needs to be
    // visible to size itself), then reveal the pre-filtered Samples tab the link was targeting.
    function honorRequestedTab() {
        if (LABKEY.ActionURL.getParameter('utilizationTab') === 'samples' && window.showInstrumentSamplesTab) {
            window.showInstrumentSamplesTab();
        }
    }

    let dateOnly = function (d) {
        let dateTime = new Date(d);
        return new Date(dateTime.getFullYear(), dateTime.getMonth(), dateTime.getDate());
    };

    let formatDate = function (d) {
        let pad = function (n) { return (n < 10 ? '0' : '') + n; };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
    };

    function addEvent(data, date) {
        data.push({
            startDate: new Date(date.getTime()),
            endDate: new Date(date.getTime()),
            fileCount: 0,
            runCount: 0,
            id: data.length
        });
    }

    function loadData(callback) {
        LABKEY.Query.selectRows({
            schemaName: 'targetedms',
            queryName: 'InstrumentUtilizationByDay',
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            columns: 'AcquisitionDate,FileCount,RunCount',
            sort: 'AcquisitionDate',
            filterArray: [ LABKEY.Filter.create('InstrumentNickname', instrumentName) ],
            success: function (response) {
                const rows = response.rows || [];

                let data = [];
                if (!rows.length) {
                    callback(data);
                    return;
                }

                let firstDate = dateOnly(rows[0].AcquisitionDate);
                let lastDate = dateOnly(rows[rows.length - 1].AcquisitionDate);

                // Fill in every day in the range so the heatmap renders contiguous months
                let cursor = new Date(firstDate.getTime());
                while (cursor.getTime() <= lastDate.getTime()) {
                    addEvent(data, cursor);
                    cursor.setDate(cursor.getDate() + 1);
                }

                let currentIndex = 0;
                for (let i = 0; i < rows.length; i++) {
                    let d = dateOnly(rows[i].AcquisitionDate);
                    while (data[currentIndex] && data[currentIndex].startDate.getTime() !== d.getTime()) {
                        currentIndex++;
                    }
                    if (!data[currentIndex]) {
                        continue;
                    }
                    data[currentIndex].fileCount = rows[i].FileCount || 0;
                    data[currentIndex].runCount = rows[i].RunCount || 0;
                    maxFileCount = Math.max(maxFileCount, data[currentIndex].fileCount);
                }

                callback(data);
            },
            failure: function (errorInfo) {
                $('#instrumentUtilizationCalendar').text('Failed loading data. ' + (errorInfo && errorInfo.exception ? errorInfo.exception : ''));
            }
        });
    }

    function updateMonths() {
        let monthCount = parseInt($('#utilizationMonthNumberSelect').val());
        $('#instrumentUtilizationCalendarWrapper').attr('class', 'months-' + monthCount);

        if (calendar) {
            let originalMonthCount = calendar.getNumberMonthsDisplayed();
            let startDate = null;

            if (originalMonthCount < monthCount) {
                startDate = new Date(calendar.getStartDate());
                let endDate = new Date(calendar.getStartDate());
                endDate.setMonth(endDate.getMonth() + monthCount - 1);
                while (endDate.getTime() > newestDataDate.getTime()) {
                    endDate.setMonth(endDate.getMonth() - 1);
                    startDate.setMonth(startDate.getMonth() - 1);
                }
            }
            else {
                let endDate = new Date(calendar.getStartDate());
                endDate.setMonth(endDate.getMonth() + originalMonthCount - monthCount);
                startDate = endDate;
            }

            calendar.setNumberMonthsDisplayed(monthCount);
            if (startDate) {
                calendar.setStartDate(startDate);
            }
        }
    }

    if (!instrumentName) {
        $('#instrumentUtilizationCalendar').text('No instrument specified.');
        return;
    }

    loadData(function (data) {
        if (!data.length) {
            $('#instrumentUtilizationCalendar').text('No samples acquired by this instrument.');
            honorRequestedTab();
            return;
        }

        newestDataDate = new Date(data[data.length - 1].startDate);
        let monthsToShow = 1;

        let startDate = new Date(newestDataDate);
        startDate.setMonth(startDate.getMonth() - monthsToShow + 1);

        $('#utilizationMonthNumberSelect').val(monthsToShow);
        updateMonths();
        $('#utilizationMonthNumberSelect').on('change', updateMonths);

        $('#heatmapFileLegendMax').text(maxFileCount + ' file' + (maxFileCount === 1 ? '' : 's'));

        calendar = new Calendar('#instrumentUtilizationCalendar', {
            startDate: startDate,
            style: 'custom',
            numberMonthsDisplayed: monthsToShow,
            enableRangeSelection: false,
            mouseOnDay: function (e) {
                let event = e.events.length > 0 ? e.events[0] : null;

                let content = '<div class="event-tooltip-content">';
                if (!event || event.fileCount === 0) {
                    content += '<div>No samples</div>';
                }
                else {
                    content += '<div>' + event.fileCount + ' file' + (event.fileCount === 1 ? '' : 's') + ' acquired</div>';
                    content += '<div>' + event.runCount + ' Skyline document' + (event.runCount === 1 ? '' : 's') + '</div>';
                }
                content += '</div>';

                $(e.element).popover({
                    trigger: 'manual',
                    container: 'body',
                    html: true,
                    content: content
                });
                $(e.element).popover('show');
            },
            mouseOutDay: function (e) {
                $(e.element).popover('hide');
            },
            clickDay: function (e) {
                // Drill into the samples acquired on the clicked day, matching the summary-grid links:
                // navigate to this page on the Samples tab with a single-day AcquiredTime filter applied.
                let event = e.events && e.events.length > 0 ? e.events[0] : null;
                if (!event || !event.fileCount) {
                    return;
                }
                window.location = LABKEY.ActionURL.buildURL('targetedms', 'showInstrument', null, {
                    name: instrumentName,
                    utilizationTab: 'samples',
                    'SampleFile.AcquiredTime~dateeq': formatDate(dateOnly(e.date))
                });
            },
            dataSource: data
        });

        calendar.setCustomDayRenderer(function (element, date) {
            let events = calendar.getEvents(date);
            if (events && events.length) {
                let e = events[0];
                if (e.fileCount > 0) {
                    let divisor = Math.max(maxFileCount, 1);
                    element.classList.add('heatmap-shaded');
                    element.classList.add('heatmap-shade' + (Math.round((e.fileCount / divisor) * 13.0)));
                }
            }
        });

        honorRequestedTab();
    });
})();
</script>
