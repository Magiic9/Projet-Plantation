package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public List<Employee> findActifs() {
        return employeeRepository.findByActifTrue();
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé introuvable : " + id));
    }

    public List<Employee> search(String terme) {
        return employeeRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(terme, terme);
    }

    public Employee save(Employee employee) {
        employee.setCodePin(encoderPin(employee.getCodePin()));
        return employeeRepository.save(employee);
    }

    public Employee update(Long id, Employee updated) {
        Employee existing = findById(id);
        existing.setNom(updated.getNom());
        existing.setPrenom(updated.getPrenom());
        existing.setDateNaissance(updated.getDateNaissance());
        existing.setPhoto(updated.getPhoto());
        existing.setActif(updated.getActif());
        existing.setTauxHoraire(updated.getTauxHoraire());
        // PIN vide = on garde l'ancien
        if (updated.getCodePin() != null && !updated.getCodePin().isBlank()) {
            existing.setCodePin(encoderPin(updated.getCodePin()));
        }
        return employeeRepository.save(existing);
    }

    public boolean verifierPin(Employee employee, String pin) {
        return employee.isPinDefini() && pin != null && passwordEncoder.matches(pin, employee.getCodePin());
    }

    private String encoderPin(String pin) {
        if (pin == null || pin.isBlank()) {
            return null;
        }
        if (!pin.matches("\\d{4,6}")) {
            throw new RuntimeException("Le code PIN doit contenir 4 à 6 chiffres.");
        }
        return passwordEncoder.encode(pin);
    }

    public void desactiver(Long id) {
        Employee employee = findById(id);
        employee.setActif(false);
        employeeRepository.save(employee);
    }

    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }
}
