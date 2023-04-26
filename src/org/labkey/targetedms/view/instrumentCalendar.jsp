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
    var calendar = null;

    function editEvent(event) {
        $('#event-modal input[name="event-index"]').val(event ? event.id : '');
        let status = event ? (event.offline ? 'Offline' : 'Online') : 'Unknown';
        $('#event-modal select[name="event-status"]').val(status);
        $('#event-modal input[name="event-start-date"]').val(event ? event.startDate.getFullYear() + '-' + (event.startDate.getMonth() + 1) + '-' + event.startDate.getDate() : '');
        $('#event-modal input[name="event-end-date"]').val(event ? event.endDate.getFullYear() + '-' + (event.endDate.getMonth() + 1) + '-' + event.endDate.getDate() : '');
        $('#event-modal').modal();
    }

    function deleteEvent(event) {
        var dataSource = calendar.getDataSource();

        calendar.setDataSource(dataSource.filter(item => item.id !== event.id));
    }

    function saveEvent() {
        let statusValue = $('#event-modal select[name="event-status"]').val();
        console.log(statusValue);
        var event = {
            id: $('#event-modal input[name="event-index"]').val(),
            startDate: new Date($('#event-modal input[name="event-start-date"]').val()),
            endDate: new Date($('#event-modal input[name="event-end-date"]').val()),
            offline: statusValue === 'Offline'
        }

        var dataSource = calendar.getDataSource();

        if (event.id) {
            for (var i in dataSource) {
                if (dataSource[i].id == event.id) {
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
            event.greens = 0;
            event.yellows = 0;
            event.reds = 0;
            event.replicateNames = [];


            dataSource.push(event);
        }

        calendar.setDataSource(dataSource);
        $('#event-modal').modal('hide');
    }


    let initCal = function(sampleFiles) {

        let data = [];

        for (let i = 0; i < sampleFiles.length; i++) {
            let sampleFile = sampleFiles[i];
            let acquiredDate = new Date(sampleFile.AcquiredTime);
            let dateOnly = new Date(acquiredDate.getFullYear(), acquiredDate.getMonth(), acquiredDate.getDate());
            let current = data[data.length - 1];
            if (data.length === 0 ||
                    current.startDate.getFullYear() !== dateOnly.getFullYear() ||
                    current.startDate.getMonth() !== dateOnly.getMonth() ||
                    current.startDate.getDate() !== dateOnly.getDate()) {

                current = {
                    startDate: dateOnly,
                    endDate: dateOnly,
                    replicateNames: [],
                    greens: 0,
                    yellows: 0,
                    reds: 0,
                    offline: Math.random() > 0.9,
                    id: data.length
                };
                data.push(current);
            }
            current.replicateNames.push(sampleFile.ReplicateName);
            if (!sampleFile.IgnoreForAllMetric) {
                if (sampleFile.LeveyJennings > 0)
                    current.reds++;
                else if (sampleFile.mR > 0)
                    current.yellows++;
                else
                    current.greens++;
            }
        }

        let newestDate = new Date(sampleFiles[0].AcquiredTime);
        let startDate = new Date(newestDate.getFullYear() - 1, newestDate.getMonth() + 1, newestDate.getDate())

        calendar = new Calendar('#calendar', {
            startDate: startDate,
            style: 'custom',
            enableRangeSelection: true,
            selectRange: function(e) {
                editEvent({ startDate: e.startDate, endDate: e.endDate, offline: e.events.length ? e.events[0].offline : false, id: e.events.length ? e.events[0].id : null});
            },
            mouseOnDay: function(e) {
                if(e.events.length > 0) {
                    var content = '';

                    let separator = '';
                    for(var i in e.events) {
                        content += separator;
                        separator = '<br/><br/>'
                        content += '<div class="event-tooltip-content">'
                                + '<div class="event-name" style="color:' + e.events[i].color + '">' + (e.events[i].offline ? 'Offline' : 'Online') + ', ' + e.events[i].replicateNames.length + ' Samples' + '</div>'
                                + '<div class="event-location">' + e.events[i].replicateNames.join(', ') + '</div>'
                                + '</div>';
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
                if(e.events.length > 0) {
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

                // Choose the best result to determine the color
                let color = e.greens > 0 ? '0,128,0' : (e.yellows > 0 ? '240,230,140' : '139,0,0');

                element.style.backgroundColor = 'rgba(' + color + ', ' + e.replicateNames.length / 10 + ')';
                if (e.offline) {
                    element.style.backgroundImage = 'linear-gradient(45deg, transparent 0%, transparent 80%, #333 80%, #333 100%)';
                }
            }
        });
    };

    LABKEY.Ajax.request({
        url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricOutliers.api'),
        params: {sampleLimit: this.sampleLimit},
        success: function (response) {
            var parsed = JSON.parse(response.responseText);
            if (parsed.sampleFiles) {
                initCal(parsed.sampleFiles);
            }
        }
    });

</script>

<div id="calendar"></div>

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
                <input type="hidden" name="event-index">
                <form class="form-horizontal">
                    <div class="form-group row">
                        <label for="event-status" class="col-sm-4 control-label">Status</label>
                        <div class="col-sm-8">
                            <select id="event-status" name="event-status" class="form-control">
                                <option value="Online">Online</option>
                                <option value="Offline">Offline</option>
                                <option value="Unknown">Unknown</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-group row">
                        <label for="min-date" class="col-sm-4 control-label">Dates</label>
                        <div class="col-sm-8">
                            <div class="input-group input-daterange">
                                <input id="min-date" name="event-start-date" size="10" type="text" class="form-control">
                                <div class="input-group-prepend input-group-append">
                                    <div class="input-group-text">to</div>
                                </div>
                                <input name="event-end-date" type="text" size="10" class="form-control">
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