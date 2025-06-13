if (!LABKEY.targetedms) {
    LABKEY.targetedms = {};
}

if (!LABKEY.targetedms.QCMetricConfigLoader) {
    LABKEY.targetedms.QCMetricConfigLoader = {
        initialQcMetrics: null,
        failureResponse: null,
        initialQcMetricsCallbacks: [],
        initialQcMetricsRequested: false,

        getMetrics : function(successCallback, callbackScope, failureCallback) {
            if (this.initialQcMetrics) {
                successCallback.call(callbackScope, this.initialQcMetrics);
            }
            else if (this.failureResponse) {
                failureCallback.call(callbackScope, this.failureResponse);
            }
            else {
                this.initialQcMetricsCallbacks.push({callback: successCallback, scope: callbackScope, failure: failureCallback});

                if (!this.initialQcMetricsRequested) {
                    this.initialQcMetricsRequested = true;
                    LABKEY.Ajax.request({
                        url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricConfigurations.api'),
                        method: 'GET',
                        success: function (response) {
                            const configs = Ext4.JSON.decode(response.responseText).configurations;
                            this.initialQcMetrics = configs;
                            for (const c of this.initialQcMetricsCallbacks) {
                                if (c.callback) {
                                    c.callback.call(c.scope, this.initialQcMetrics);
                                }
                            }
                            this.initialQcMetricsCallbacks = [];
                        },
                        failure: LABKEY.Utils.getCallbackWrapper(function (response) {
                            this.failureResponse = response;
                            for (const c of this.initialQcMetricsCallbacks) {
                                if (c.failure) {
                                    c.failure.call(c.scope, response);
                                }
                            }
                            this.initialQcMetricsCallbacks = [];
                        }, null, true),
                        scope: this
                    });
                }
            }
        },

    }
}