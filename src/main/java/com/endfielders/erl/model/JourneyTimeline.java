package com.endfielders.erl.model;

import java.util.List;

/**
 * A carrier's complete day-by-day journey timeline with weather forecasts.
 * Each carrier gets its own timeline because delivery duration and mode differ.
 */
public class JourneyTimeline {
    private List<DayWeather> days;
    private String disclaimer = "Journey timeline is an estimate based on typical transit patterns. ERL does not guarantee carrier schedules or routing.";

    public JourneyTimeline() {}

    public JourneyTimeline(List<DayWeather> days) {
        this.days = days;
    }

    public List<DayWeather> getDays() { return days; }
    public void setDays(List<DayWeather> days) { this.days = days; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
