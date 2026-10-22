package com.raphael.plantation_manager.service;

import java.time.Duration;
import java.time.LocalTime;

/** Petits calculs d'heures partagés par les services. */
final class Heures {

    private Heures() {}

    static double entre(LocalTime debut, LocalTime fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            return 0;
        }
        return Duration.between(debut, fin).toMinutes() / 60.0;
    }

    static double arrondi(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }
}
