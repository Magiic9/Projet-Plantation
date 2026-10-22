package com.raphael.plantation_manager;

import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.entity.Parcelle;
import com.raphael.plantation_manager.entity.Tache;
import com.raphael.plantation_manager.service.EmployeeService;
import com.raphael.plantation_manager.service.ParcelleService;
import com.raphael.plantation_manager.service.TacheService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Mode démo : lance l'application avec une base en mémoire et quelques données d'exemple.
 *   ./mvnw spring-boot:test-run
 * Comptes : patron / patron-demo et borne / borne-demo. Code PIN des employés : 1234.
 */
public class TestPlantationManagerApplication {

    public static void main(String[] args) {
        SpringApplication.from(PlantationManagerApplication::main).with(DonneesDemo.class).run(args);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DonneesDemo {

        @Bean
        CommandLineRunner remplirDemo(EmployeeService employes, ParcelleService parcelles, TacheService taches) {
            return args -> {
                if (!employes.findAll().isEmpty()) {
                    return;
                }
                String[][] personnes = {
                        {"Joseph", "Marie-Claire", "11.50"},
                        {"Rosier", "Jean-Pierre", "11.88"},
                        {"Lafleur", "Sandrine", "12.20"},
                        {"Bellemare", "Patrick", "11.88"},
                };
                for (String[] p : personnes) {
                    Employee e = new Employee();
                    e.setNom(p[0]);
                    e.setPrenom(p[1]);
                    e.setTauxHoraire(Double.parseDouble(p[2]));
                    e.setCodePin("1234");
                    employes.save(e);
                }
                for (String[] p : new String[][] {{"Parcelle Nord", "4.5"}, {"Parcelle Rivière", "3.2"}, {"Grand Champ", "7.0"}}) {
                    Parcelle parcelle = new Parcelle();
                    parcelle.setNom(p[0]);
                    parcelle.setSurface(Double.parseDouble(p[1]));
                    parcelles.save(parcelle);
                }
                for (String nom : new String[] {"Récolte", "Désherbage", "Œilletonnage", "Traitement", "Emballage"}) {
                    Tache tache = new Tache();
                    tache.setNom(nom);
                    taches.save(tache);
                }
            };
        }
    }
}
