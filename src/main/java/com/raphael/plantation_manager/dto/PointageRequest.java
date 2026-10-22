package com.raphael.plantation_manager.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Pointage saisi ou corrigé à la main par le patron. */
public record PointageRequest(Long employeId, LocalDate date, LocalTime heureArrivee, LocalTime heureDepart) {}
