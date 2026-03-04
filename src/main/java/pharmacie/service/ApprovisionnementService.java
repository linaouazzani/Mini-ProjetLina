package pharmacie.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

@Service
public class ApprovisionnementService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApprovisionnementService.class);

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Value("${postmark.api.token}")
    private String postmarkApiToken;

    @Value("${postmark.sender.email}")
    private String senderEmail;

    @Transactional
    public void declencherReapprovisionnement() {
        log.info("Vérification des stocks...");
        List<Medicament> rupture = medicamentRepository.findMedicamentsAReapprovisionner();

        if (rupture.isEmpty()) return;

        Map<Fournisseur, List<Medicament>> parFournisseur = new HashMap<>();
        for (Medicament m : rupture) {
            if (m.getCategorie() != null) {
                for (Fournisseur f : m.getCategorie().getFournisseurs()) {
                    parFournisseur.computeIfAbsent(f, k -> new ArrayList<>()).add(m);
                }
            }
        }

        parFournisseur.forEach(this::envoyerEmail);
    }

    private void envoyerEmail(Fournisseur f, List<Medicament> meds) {
        try {
            StringBuilder sb = new StringBuilder("Besoin de réapprovisionnement pour :\\n");
            for (Medicament m : meds) {
                int qte = m.getNiveauDeReappro() - m.getUnitesEnStock() + 50;
                sb.append("- ").append(m.getNom()).append(" (Qté: ").append(qte).append(")\\n");
            }

            String json = String.format(
                "{\"From\":\"%s\",\"To\":\"%s\",\"Subject\":\"Commande Pharmacie\",\"TextBody\":\"%s\"}",
                senderEmail, f.getEmail(), sb.toString()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Postmark-Api-Token", postmarkApiToken);

            new RestTemplate().postForEntity("https://api.postmarkapp.com/email", new HttpEntity<>(json, headers), String.class);
            log.info("Email envoyé à {}", f.getNom());
        } catch (Exception e) {
            log.error("Échec envoi à {}: {}", f.getNom(), e.getMessage());
        }
    }
}