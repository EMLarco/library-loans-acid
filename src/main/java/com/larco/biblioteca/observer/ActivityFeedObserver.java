package com.larco.biblioteca.observer;

import com.larco.biblioteca.support.ActivityLog;
import org.springframework.stereotype.Component;

/** Observador 3: registro de actividad visible en la interfaz web. */
@Component
public class ActivityFeedObserver implements LibraryObserver {
    private final ActivityLog activity;

    public ActivityFeedObserver(ActivityLog activity) { this.activity = activity; }

    @Override public String name() { return "ActivityFeedObserver"; }

    @Override
    public void onEvent(LibraryEvent e) {
        activity.add("OBSERVER", "ActivityFeedObserver -> " + e.type() + " por " + e.performedBy() + " " + e.data());
    }
}
