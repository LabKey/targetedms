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
    let calendar = null;
    let heatMapSource = 'sampleCount';
    let maxSampleCount = 0;
    let maxOutliers = 0;

    function editEvent(event) {
        $('#event-modal input[name="event-index"]').val(event.id);
        $('#event-modal input[name="event-annotationId"]').val(event.annotation ? event.annotation.id : '');

        let startDate = event.startDate;
        let endDate = event.endDate;
        if (event.annotation) {
            startDate = dateOnly(event.annotation.date);
            endDate = event.annotation.enddate ? dateOnly(event.annotation.enddate) : startDate;
        }

        $('#event-modal input[name="event-description"]').val(event.annotation ? event.annotation.description : '');
        $('#event-modal input[name="event-start-date"]').val(startDate.getFullYear() + '-' + (startDate.getMonth() + 1) + '-' + startDate.getDate());
        $('#event-modal input[name="event-end-date"]').val(endDate.getFullYear() + '-' + (endDate.getMonth() + 1) + '-' + endDate.getDate());
        $('#event-modal').modal();
    }

    function deleteEvent(event) {
        var dataSource = calendar.getDataSource();

        calendar.setDataSource(dataSource.filter(item => item.id !== event.id));
    }

    function saveEvent() {
        let statusValue = $('#event-modal select[name="event-status"]').val();
        var event = {
            id: $('#event-modal input[name="event-index"]').val(),
            startDate: new Date($('#event-modal input[name="event-start-date"]').val()),
            endDate: new Date($('#event-modal input[name="event-end-date"]').val()),
            offline: statusValue === 'Offline'
        }

        var dataSource = calendar.getDataSource();

        if (event.id) {
            for (let i in dataSource) {
                if (dataSource[i].id === event.id) {
                    dataSource[i].startDate = event.startDate;
                    dataSource[i].offline = event.offline;
                    dataSource[i].endDate = event.endDate;
                }
            }
        }
        else
        {
            var newId = 0;
            for(var i in dataSource) {
                if(dataSource[i].id > newId) {
                    newId = dataSource[i].id;
                }
            }

            newId++;
            event.id = newId;
            event.medianOutliers = 0;
            event.replicateNames = [];

            dataSource.push(event);
        }

        calendar.setDataSource(dataSource);
        $('#event-modal').modal('hide');
    }

    let dateOnly = function(d) {
        let dateTime = new Date(d);
        return new Date(dateTime.getFullYear(), dateTime.getMonth(), dateTime.getDate());
    }

    let initCal = function(sampleFiles, annotations) {

        let firstDate;
        let lastDate;
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

        let data = [];

        while (firstDate && firstDate.getTime() <= lastDate.getTime()) {
            data.push({
                startDate: new Date(firstDate.getTime()),
                endDate: new Date(firstDate.getTime()),
                replicateNames: [],
                outliers: [],
                id: data.length
            });
            firstDate.setDate(firstDate.getDate() + 1);
        }

        let currentIndex = 0;

        for (let i = sampleFiles.length - 1; i >= 0; i--) {
            let sampleFile = sampleFiles[i];
            let d = dateOnly(sampleFile.AcquiredTime);
            while (data[currentIndex].startDate.getTime() !== d.getTime()) {
                currentIndex++;
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
            }
            let current = data[currentIndex];
            current.annotation = annotation;

            if (annotation.enddate) {
                let endDate = dateOnly(annotation.enddate);
                let endDateIndex = currentIndex + 1;
                while (data[endDateIndex].startDate.getTime() <= endDate.getTime()) {
                    data[endDateIndex++].annotation = annotation;
                }
            }
        }


        data.forEach(e => {
            let values = e.outliers;

            // Calculate the median
            values.sort(function(a,b){
                return a-b;
            });
            let half = Math.floor(values.length / 2);
            e.medianOutliers = values.length === 0 ? 0 : (values.length % 2 === 0 ? (values[half - 1] + values[half]) / 2.0 : values[half]);

            maxSampleCount = Math.max(maxSampleCount, e.replicateNames.length);
            maxOutliers = Math.max(maxOutliers, e.medianOutliers);
        });

        let newestDate = sampleFiles.length ? new Date(sampleFiles[0].AcquiredTime) : new Date();
        let startDate = newestDate;
        let monthsToShow = 1;

        document.getElementById('monthNumberSelect').value = monthsToShow;
        document.getElementById('heatMapSource').value = heatMapSource;
        updateMonths();

        calendar = new Calendar('#calendar', {
            startDate: startDate,
            style: 'custom',
            numberMonthsDisplayed: monthsToShow,
            enableRangeSelection: true,
            selectRange: function(e) {
                editEvent({
                    startDate: e.startDate,
                    endDate: e.endDate,
                    annotation: e.events.length ? e.events[0].annotation : null,
                    id: e.events.length ? e.events[0].id : null
                });
            },
            mouseOnDay: function(e) {
                if(e.events.length > 0) {
                    let content = '';

                    let separator = '';
                    for(var i in e.events) {
                        content += separator;
                        separator = '<br/><br/>'
                        content += '<div class="event-tooltip-content">'
                                + '<div class="event-name" style="color:' + e.events[i].color + '">' + (e.events[i].annotation ? ('Offline (' + LABKEY.Utils.encodeHtml(e.events[i].annotation.description) + ')') : 'Online') + '</div>'
                                + '<div>' + e.events[i].replicateNames.length + ' Sample' + (e.events[i].replicateNames.length === 1 ? '' : 's') + ':</div>';
                        for (let j in e.events[i].replicateNames) {
                            content += '<div class="event-location">' + LABKEY.Utils.encodeHtml(e.events[i].replicateNames[j]) + ' (' + e.events[i].outliers[j] + ' outliers)</div>';
                        }
                        content += '</div>';
                    }

                    $(e.element).popover({
                        trigger: 'manual',
                        container: 'body',
                        html:true,
                        content: content
                    });

                    $(e.element).popover('show');
                }
            },
            mouseOutDay: function(e) {
                if (e.events.length > 0) {
                    $(e.element).popover('hide');
                }
            },
            dayContextMenu: function(e) {
                $(e.element).popover('hide');
            },
            dataSource: data
        });

        $('#save-event').click(function() {
            saveEvent();
        });


        calendar.setCustomDayRenderer(function(element, date) {
            let events = calendar.getEvents(date);
            if (events && events.length) {
                let e = events[0];

                let value = heatMapSource === 'sampleCount' ? e.replicateNames.length : e.medianOutliers;
                let divisor = heatMapSource === 'sampleCount' ? maxSampleCount : maxOutliers;

                // Choose the best result to determine the color
                let color = '0,128,0';

                element.style.backgroundColor = 'rgba(' + color + ', ' + value / divisor + ')';
                if (e.annotation) {
                    element.style.backgroundImage = 'linear-gradient(45deg, transparent 0%, transparent 50%, #3334 50%, #3334 100%)';
                }
            }
        });
    };

    LABKEY.Ajax.request({
        url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricOutliers.api'),
        params: {sampleLimit: this.sampleLimit, includeAnnotations: true},
        success: function (response) {
            var parsed = JSON.parse(response.responseText);
            if (parsed.sampleFiles) {
                initCal(parsed.sampleFiles, parsed.instrumentDowntimeAnnotations);
            }
        },
        failure: function(errorInfo) {
            document.getElementById('calendar').innerText = 'Failed to load data';
        }
    });


    function updateMonths()
    {
        let monthCount = document.getElementById('monthNumberSelect').value;
        document.getElementById('calendarWrapper').className = 'months-' + monthCount;
        if (calendar) {
            calendar.setNumberMonthsDisplayed(monthCount);
        }
    }

    function updateHeatmap() {
        heatMapSource = document.getElementById('heatMapSource').value;
        calendar.render();
    }
</script>

<div>
    <label for="monthNumberSelect">Display:</label>
    <select id="monthNumberSelect" onchange="updateMonths()">
        <option value="1">1 month</option>
        <option value="4">4 months</option>
        <option value="12">12 months</option>
    </select>
    &nbsp;&nbsp;
    <label for="heatMapSource">Heat map data source:</label>
    <select id="heatMapSource" onchange="updateHeatmap()">
        <option value="sampleCount">Sample count</option>
        <option value="outliers">Outliers</option>
    </select>
</div>

<div id="calendarWrapper">
    <div id="calendar"></div>
</div>

<div class="modal fade" id="event-modal">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Event</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <form class="form-horizontal">
                    <input type="hidden" name="event-index">
                    <input type="hidden" name="event-annotationId">
                    <div class="form-group row">
                        <label for="event-description" class="col-sm-4 control-label">Description</label>
                        <div class="col-sm-8">
                            <input id="event-description" name="event-description" type="text" size="10" class="form-control">
                        </div>
                    </div>
                    <div class="form-group row">
                        <label for="min-date" class="col-sm-4 control-label">Dates</label>
                        <div class="col-sm-4">
                            <div class="input-group">
                                <input id="min-date" name="event-start-date" type="text" class="form-control"> through
                                <input name="event-end-date" type="text" class="form-control">
                            </div>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-primary" id="save-event">
                    Save
                </button>
            </div>
        </div>
    </div>
</div>
<div id="context-menu">
</div>