package com.bugsnag.mazerunner.scenarios;

import com.bugsnag.Bugsnag;
import com.bugsnag.Report;
import com.bugsnag.callbacks.Callback;

/**
 * Sends a handled exception to Bugsnag, which contains metadata that should be filtered
 */
public class ManualFilterScenario extends Scenario {

    public ManualFilterScenario(Bugsnag bugsnag) {
        super(bugsnag);
    }

    @Override
    public void run() {

        bugsnag.setFilters("foo");

        bugsnag.notify(generateException(), new Callback() {
            @Override
            public void beforeNotify(Report report) {
                report.addToTab("user", "foo", "hunter2");
                report.addToTab("custom", "foo", "hunter2");
                report.addToTab("custom", "bar", "hunter2");
            }
        });
    }
}
