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
        System.out.println(">>> [LOG] Début de la vérification des stocks...");
        log.info("--- Début de la vérification des stocks ---");
        
        // Utilisation de findAll() pour filtrer manuellement (plus fiable pour le test)
        List<Medicament> rupture = medicamentRepository.findAll().stream()
                .filter(m -> m.getUnitesEnStock() < m.getNiveauDeReappro())
                .toList();

        if (rupture.isEmpty()) {
            System.out.println(">>> [LOG] Aucun médicament en rupture.");
            log.info("Aucun médicament en rupture (Stock > Seuil).");
            return;
        }

        System.out.println(">>> [LOG] " + rupture.size() + " médicaments à commander.");

        Map<Fournisseur, List<Medicament>> parFournisseur = new HashMap<>();
        for (Medicament m : rupture) {
            if (m.getCategorie() != null && m.getCategorie().getFournisseurs() != null) {
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
                sb.append("- ").append(m.getNom()).append(" (Stock: ").append(m.getUnitesEnStock()).append(" / Commande: ").append(qte).append(")\\n");
            }

            // JSON corrigé avec MessageStream (INDISPENSABLE pour Postmark)
            String json = String.format(
                "{\"From\":\"%s\",\"To\":\"%s\",\"Subject\":\"COMMANDE : %s\",\"TextBody\":\"%s\",\"MessageStream\":\"outbound\"}",
                senderEmail, senderEmail, f.getNom(), sb.toString()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Postmark-Server-Token", postmarkApiToken);

            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            
            System.out.println(">>> [POSTMARK] Envoi du mail pour " + f.getNom() + "...");
            String response = new RestTemplate().postForObject("https://api.postmarkapp.com/email", entity, String.class);
            
            System.out.println(">>> [SUCCÈS] Mail reçu par Postmark : " + response);
            log.info("Email simulé envoyé pour {} (Reçu sur votre boîte mail)", f.getNom());
            
        } catch (Exception e) {
            System.err.println(">>> [ERREUR] Échec de l'envoi : " + e.getMessage());
            log.error("Échec de l'envoi pour {}: {}", f.getNom(), e.getMessage());
        }
    }
}