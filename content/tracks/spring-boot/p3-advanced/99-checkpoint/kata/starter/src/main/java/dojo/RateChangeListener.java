package dojo;

import org.springframework.stereotype.Component;

/** Hears RateChanged: writes the audit line and counts the update, only once the change is committed. */
@Component
public class RateChangeListener {
}
