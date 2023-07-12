<%
    /*
     * Copyright (c) 2023 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("targetedms/yearCalendar");
    }
%>

<script>
(function() {
    let calendar = null;
    let heatMapSource = 'sampleCount';
    let maxSampleCount = 0;
    let maxOutliers = 0;
    let offlineAnnotationTypeId = -1;
    let newestDataDate = new Date();
    let sampleFiles = [];

    function editEvent(event) {
        $('#event-modal input[name="event-index"]').val(event.id);
        $('#event-modal input[name="event-annotationId"]').val(event.annotation ? event.annotation.id : '');

        let startDate = event.startDate;
        let endDate = event.endDate;
        if (event.annotation) {
            startDate = dateOnly(event.annotation.date);
            endDate = event.annotation.enddate ? dateOnly(event.annotation.enddate) : startDate;
        }


        $('#delete-event').css('display', event.annotation ? '' : 'none');

        $('#event-modal input[name="event-description"]').val(event.annotation ? event.annotation.description : '');
        $('#event-modal input[name="event-start-date"]').val(startDate.getFullYear() + '-' + (startDate.getMonth() + 1 < 10 ? '0' : '') + (startDate.getMonth() + 1) + '-' + (startDate.getDate() < 10 ? '0' : '') + startDate.getDate());
        $('#event-modal input[name="event-end-date"]').val(endDate.getFullYear() + '-' + (endDate.getMonth() + 1 < 10 ? '0' : '') + (endDate.getMonth() + 1) + '-' + (endDate.getDate() < 10 ? '0' : '') + endDate.getDate());
        $('#annotation-save-error').text('');
        $('#event-modal').modal();
    }

    function deleteEvent() {
        const event = {
            id: $('#event-modal input[name="event-annotationId"]').val()
        }

        LABKEY.Query.saveRows({
            commands: [{
                command: 'delete',
                schemaName: 'targetedms',
                queryName: 'QCAnnotation',
                rows: [
                    event
                ]
            }],
            success: function () {
                // Don't reload the full data when only the annotations have been edited
                loadData(function (data) {
                    calendar.setDataSource(data);
                    $('#event-modal').modal('hide');
                }, true);
            },
            failure: function (errorInfo) {
                $('#annotation-save-error').text('Error saving. ' + (errorInfo.exception ? errorInfo.exception : ''));
            }
        });
    }

    function addEvent(data, date) {
        data.push({
            startDate: new Date(date.getTime()),
            endDate: new Date(date.getTime()),
            replicateNames: [],
            outliers: [],
            id: data.length
        });
    }

    function loadData(callback, annotationsOnly) {
        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricOutliers.api'),
            params: {
                sampleLimit: this.sampleLimit,
                includeAnnotations: true,
                includeSampleInfo: annotationsOnly !== true
            },
            success: function (response) {
                const parsed = JSON.parse(response.responseText);
                if (parsed.sampleFiles) {
                    sampleFiles = parsed.sampleFiles;
                }
                const annotations = parsed.instrumentDowntimeAnnotations;
                offlineAnnotationTypeId = parsed.offlineAnnotationTypeId;

                let firstDate = dateOnly(new Date());
                let lastDate = dateOnly(new Date());
                if (sampleFiles.length) {
                    lastDate = dateOnly(sampleFiles[0].AcquiredTime);
                    firstDate = dateOnly(sampleFiles[sampleFiles.length - 1].AcquiredTime);
                }

                if (annotations.length) {
                    let firstAnnotation = dateOnly(annotations[0].date);
                    let lastAnnotation = annotations[annotations.length - 1].enddate ?
                            dateOnly(annotations[annotations.length - 1].enddate) :
                            dateOnly(annotations[annotations.length - 1].date);
                    if (!firstDate || firstAnnotation.getTime() < firstDate.getTime()) {
                        firstDate = firstAnnotation;
                    }
                    if (!lastDate || lastAnnotation.getTime() > lastDate.getTime()) {
                        lastDate = lastAnnotation;
                    }
                }

                // Hack - problem with annotation being at the end of the range
                firstDate.setDate(firstDate.getDate() - 1);
                lastDate.setDate(lastDate.getDate() + 1);

                let data = [];

                while (firstDate && firstDate.getTime() <= lastDate.getTime()) {
                    addEvent(data, firstDate);
                    firstDate.setDate(firstDate.getDate() + 1);
                }

                let currentIndex = 0;

                for (let i = sampleFiles.length - 1; i >= 0; i--) {
                    let sampleFile = sampleFiles[i];
                    let d = dateOnly(sampleFile.AcquiredTime);
                    while (data[currentIndex].startDate.getTime() !== d.getTime()) {
                        currentIndex++;
                        if (!data[currentIndex]) {
                            addEvent(data, d);
                        }
                    }
                    let current = data[currentIndex];
                    current.replicateNames.push(sampleFile.ReplicateName);
                    current.outliers.push(sampleFile.IgnoreForAllMetric ? 0 : sampleFile.LeveyJennings);
                }

                currentIndex = 0;
                for (let i = 0; i < annotations.length; i++) {
                    let annotation = annotations[i];
                    let d = dateOnly(annotation.date);
                    while (data[currentIndex].startDate.getTime() !== d.getTime()) {
                        currentIndex++;
                        if (!data[currentIndex]) {
                            addEvent(data, );
                        }
                    }
                    let current = data[currentIndex];
                    current.annotation = annotation;

                    if (annotation.enddate) {
                        let endDate = dateOnly(annotation.enddate);
                        let endDateIndex = currentIndex;
                        if (!data[endDateIndex]) {
                            addEvent(data, endDate);
                        }
                        while (data[endDateIndex].startDate.getTime() <= endDate.getTime()) {
                            data[endDateIndex].annotation = annotation;
                            endDateIndex++;
                            if (!data[endDateIndex]) {
                                addEvent(data, endDate);
                            }
                        }
                    }
                }

                data.forEach(e => {
                    let values = e.outliers;

                    // Calculate the median
                    values.sort(function (a, b) {
                        return a - b;
                    });
                    let half = Math.floor(values.length / 2);
                    e.medianOutliers = values.length === 0 ? 0 : (values.length % 2 === 0 ? (values[half - 1] + values[half]) / 2.0 : values[half]);

                    maxSampleCount = Math.max(maxSampleCount, e.replicateNames.length);
                    maxOutliers = Math.max(maxOutliers, e.medianOutliers);
                });

                callback(data, parsed.displayConfig);
            },
            failure: LABKEY.Utils.getCallbackWrapper(function (errorInfo) {
                $('#calendar').text('Failed loading data. ' + (errorInfo.exception ? errorInfo.exception : ''));
            })
        });
    }

    function saveEvent() {
        const event = {
            id: $('#event-modal input[name="event-annotationId"]').val(),
            date: $('#event-modal input[name="event-start-date"]').val(),
            endDate: $('#event-modal input[name="event-end-date"]').val(),
            description: $('#event-modal input[name="event-description"]').val(),
            qcAnnotationTypeId: offlineAnnotationTypeId
        }

        LABKEY.Query.saveRows({
            commands: [{
                command: event.id ? 'update' : 'insert',
                schemaName: 'targetedms',
                queryName: 'QCAnnotation',
                rows: [
                    event
                ]
            }],
            success: function () {
                // Don't reload the full data when only the annotations have been edited
                loadData(function (data) {
                    calendar.setDataSource(data);
                    $('#event-modal').modal('hide');
                }, true);
            },
            failure: function (errorInfo) {
                $('#annotation-save-error').text('Error saving. ' + (errorInfo.exception ? errorInfo.exception : ''));
            }
        })

    }

    let dateOnly = function (d) {
        let dateTime = new Date(d);
        return new Date(dateTime.getFullYear(), dateTime.getMonth(), dateTime.getDate());
    }

    loadData(function (data, displayConfig) {
        newestDataDate = data.length ? new Date(data[data.length - 1].startDate) : new Date();
        let monthsToShow = displayConfig && displayConfig.calendarMonthsToShow ? displayConfig.calendarMonthsToShow : 1;

        let startDate = new Date(newestDataDate);
        startDate.setMonth(startDate.getMonth() - monthsToShow + 1);

        heatMapSource = displayConfig && displayConfig.heatMapDataSource ? displayConfig.heatMapDataSource : 'sampleCount';

        $('#monthNumberSelect').val(monthsToShow);
        $('#heatMapSource').val(heatMapSource);

        updateMonths();
        updateHeatmap();

        $('#monthNumberSelect').on('change', updateMonths);
        $('#heatMapSource').on('change', updateHeatmap);


        monthNumberSelect
        calendar = new Calendar('#calendar', {
            startDate: startDate,
            style: 'custom',
            numberMonthsDisplayed: monthsToShow,
            enableRangeSelection: LABKEY.user.canInsert,
            selectRange: function (e) {
                editEvent({
                    startDate: e.startDate,
                    endDate: e.endDate,
                    annotation: e.events.length ? e.events[0].annotation : null,
                    id: e.events.length ? e.events[0].id : null
                });
            },
            mouseOnDay: function (e) {
                let event = e.events.length > 0 ? e.events[0] : null;

                let content = '<div class="event-tooltip-content">'
                        + '<div class="event-name">';
                content += (event && event.annotation ? ('Offline: ' + LABKEY.Utils.encodeHtml(event.annotation.description)) : 'Online') + '</div>';

                if (!event || event.replicateNames.length === 0) {
                    content += '<div>No samples</div>';
                }
                else {
                    content += '<div>' + e.events[0].replicateNames.length + ' Sample' + (event.replicateNames.length === 1 ? '' : 's') + ', ' + event.medianOutliers + ' Median Outliers</div>';
                    for (let j = 0; j < event.replicateNames.length; j++) {
                        content += '<div class="event-location">' + LABKEY.Utils.encodeHtml(event.replicateNames[j]) + ' (' + event.outliers[j] + ' outliers)</div>';
                    }
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
            dataSource: data
        });

        $('#save-event').click(function () {
            saveEvent();
        });

        $('#delete-event').click(function () {
            deleteEvent();
        });

        calendar.setCustomDayRenderer(function (element, date) {
            let events = calendar.getEvents(date);
            if (events && events.length) {
                let e = events[0];

                if (e.replicateNames.length > 0) {
                    let value = heatMapSource === 'sampleCount' ? e.replicateNames.length : Math.max(e.medianOutliers, 1);
                    let divisor = heatMapSource === 'sampleCount' ? maxSampleCount : Math.max(maxOutliers, 1);
                    element.classList.add('heatmap-shaded');
                    element.classList.add('heatmap-shade' + (Math.round((value / divisor) * 13.0)));
                }
                if (e.annotation) {
                    element.classList.add('offline');
                }
            }
        });
    });

    function saveOptions(options) {
        if (!LABKEY.user.isGuest) {
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('targetedms', 'leveyJenningsPlotOptions.api'),
                method: 'POST',
                params: options
            });
        }
    }

    function updateMonths() {
        let monthCount = parseInt($('#monthNumberSelect').val());
        $('#calendarWrapper').attr('class', 'months-' + monthCount);

        // Initial invocation is before the calendar is initialized
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
            saveOptions({
                calendarMonthsToShow: monthCount
            });
        }
    }

    function updateHeatmap() {
        heatMapSource = $('#heatMapSource').val();
        if (heatMapSource === 'sampleCount') {
            $('#heatmapLegendMax').text(maxSampleCount + ' sample' + (maxSampleCount === 1 ? '' : 's'));
        }
        else {
            $('#heatmapLegendMax').text(maxOutliers + ' outlier' + (maxOutliers === 1 ? '' : 's'));
        }

        if (calendar) {
            calendar.render();
            saveOptions({
                heatmapDataSource: heatMapSource
            });
        }
    }
})();
</script>

<div style="text-align: center; width: 100%">
    <label for="monthNumberSelect">Display:</label>
    <select id="monthNumberSelect">
        <option value="1">1 month</option>
        <option value="4">4 months</option>
        <option value="12">12 months</option>
    </select>
    &nbsp;&nbsp;
    <label style="padding-left: 2em" for="heatMapSource">Heat map data source:</label>
    <select id="heatMapSource">
        <option value="sampleCount">Sample count</option>
        <option value="outliers">Median outliers</option>
    </select>

    <div class="calendar months-12">
        <div class="months-container">
            <table class="month">
            </table>
        </div>
    </div>
</div>

<div id="calendarWrapper" style="min-height: 300px;">
    <div id="calendar">
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
        <div class="heatmap-legend-label" id="heatmapLegendMax">Total samples</div>
    </div>

    <div class="heatmap-legend-container">
        Offline:
        <div class="calendar">
            <div class="calendar-month">
                <table class="month">
                    <tr>
                        <td class="day" style="height: 2.5em; width: 2.5em;"><div class="day-content offline">&nbsp;</div></td>
                    </tr>
                </table>
            </div>
        </div>
        Toggle offline/online status by clicking on a day or click/dragging multiple days.
    </div>
</div>
<div class="modal fade" id="event-modal">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title">Instrument Offline Annotation</h4>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <form class="form-horizontal">
                    <input type="hidden" name="event-index">
                    <input type="hidden" name="event-annotationId">
                    <div class="form-group row">
                        <label class="col-sm-4 control-label"></label>
                        <div class="col-sm-8">
                            Manually annotating an instrument as being offline, whether or not it acquired samples on
                            a particular day, helps track instrument utilization and availability.
                            <br/>
                            <br/>
                        </div>
                    </div>
                    <div class="form-group row">
                        <label for="event-description" class="col-sm-4 control-label" title="Required field">Description *</label>
                        <div class="col-sm-8">
                            <input id="event-description" name="event-description" type="text" size="30" class="form-control">
                        </div>
                    </div>
                    <div class="form-group row">
                        <label for="min-date" class="col-sm-4 control-label" title="Required field">Date *</label>
                        <div class="col-sm-8">
                            <input id="min-date" name="event-start-date" type="text" class="form-control">
                        </div>
                    </div>
                    <div class="form-group row">
                        <label for="end-date" class="col-sm-4 control-label">End Date</label>
                        <div class="col-sm-8">
                            <input id="end-date" name="event-end-date" type="text" class="form-control">
                        </div>
                    </div>
                    <div class="form-group row" >
                        <div class="col-sm-12 labkey-error" style="min-height: 2em;" id="annotation-save-error"></div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-primary" id="delete-event">Delete</button>
                <button type="button" class="btn btn-primary" id="save-event">Save</button>
            </div>
        </div>
    </div>
</div>
