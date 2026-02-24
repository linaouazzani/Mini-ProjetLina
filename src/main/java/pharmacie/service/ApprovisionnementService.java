package pharmacie.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.extern.slf4j.Slf4j;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Medicament;

@Service
@Slf4j
public class ApprovisionnementService {

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Value("${sendgrid.api.key:CLE_DE_TEST}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email:pharmacie@hopital.com}")
    private String fromEmail;

    @Transactional
    public void declencherReapprovisionnement() {
        log.info("Analyse des stocks...");

        List<Medicament> enRupture = medicamentRepository.findMedicamentsAReapprovisionner();

        if (enRupture.isEmpty()) {
            log.info("Rien à commander.");
            return;
        }

        // On crée un message récapitulatif
        StringBuilder sb = new StringBuilder("Commande de réapprovisionnement :\n");
        for (Medicament m : enRupture) {
            sb.append("- ").append(m.getNom()).append(" (Stock: ").append(m.getUnitesEnStock()).append(")\n");
        }

        // Envoi groupé à une adresse fixe pour éviter l'erreur Categorie/Fournisseur
        envoyerEmailSimpli("appro@pharmacie.com", sb.toString());
    }

    private void envoyerEmailSimpli(String destinataire, String corps) {
        if (sendGridApiKey.equals("CLE_DE_TEST")) {
            log.warn("### SIMULATION MAIL ###\nPour: {}\nContenu: \n{}", destinataire, corps);
            return;
        }

        Email from = new Email(fromEmail);
        Email to = new Email(destinataire);
        Content content = new Content("text/plain", corps);
        Mail mail = new Mail(from, "Alerte Stock Pharmacie", to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);
            log.info("Email envoyé avec succès !");
        } catch (IOException e) {
            log.error("Erreur SendGrid : {}", e.getMessage());
        }
    }
}