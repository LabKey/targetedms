if (!LABKEY.targetedms) {
    LABKEY.targetedms = {};
}

if (!LABKEY.targetedms.QCMetricConfigLoader) {
    LABKEY.targetedms.QCMetricConfigLoader = {
        initialQcMetrics: null,
        initialQcMetricsCallbacks: [],
        initialQcMetricsRequested: false,

        getMetrics : function(successCallback, callbackScope) {
            if (this.initialQcMetrics) {
                successCallback.call(callbackScope, this.initialQcMetrics);
            }
            else {
                this.initialQcMetricsCallbacks.push({callback: successCallback, scope: callbackScope});

                if (!this.initialQcMetricsRequested) {
                    this.initialQcMetricsRequested = true;
                    LABKEY.Ajax.request({
                        url: LABKEY.ActionURL.buildURL('targetedms', 'GetQCMetricConfigurations.api'),
                        method: 'GET',
                        success: function (response) {
                            const configs = Ext4.JSON.decode(response.responseText).configurations;
                            this.initialQcMetrics = configs;
                            for (const c of this.initialQcMetricsCallbacks) {
                                c.callback.call(c.scope, this.initialQcMetrics);
                            }
                            this.initialQcMetricsCallbacks = [];
                        },
                        failure: LABKEY.Utils.getCallbackWrapper(function (response) {
                            this.failureHandler(response);
                        }, null, true),
                        scope: this
                    });
                }
            }
        },

    }
}