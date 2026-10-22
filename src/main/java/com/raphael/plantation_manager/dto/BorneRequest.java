package com.raphael.plantation_manager.dto;

/** Action d'un employé sur la borne. tacheId et parcelleId servent seulement pour démarrer une tâche. */
public record BorneRequest(Long employeId, String pin, Long tacheId, Long parcelleId) {}
