package pharmacie.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Value("${postmark.api.token}")
    private String postmarkApiToken;

    @Value("${postmark.sender.email}")
    private String senderEmail;

    // Classe interne pour recevoir le JSON du front
    public static class ContactDTO {
        public String expediteur;
        public String sujet;
        public String message;
    }

    @PostMapping
    public ResponseEntity<String> envoyerContact(@RequestBody ContactDTO dto) {
        log.info("Nouveau message de contact de : {}", dto.expediteur);
        try {
            String corps = String.format(
                "Message de : %s\\n\\nSujet : %s\\n\\n%s",
                dto.expediteur, dto.sujet, dto.message
            );

            String json = String.format(
                "{\"From\":\"%s\",\"To\":\"%s\",\"Subject\":\"Contact : %s\",\"TextBody\":\"%s\",\"MessageStream\":\"outbound\"}",
                senderEmail, senderEmail, dto.sujet, corps
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Postmark-Server-Token", postmarkApiToken);

            new RestTemplate().postForEntity(
                "https://api.postmarkapp.com/email",
                new HttpEntity<>(json, headers),
                String.class
            );

            log.info("Message de contact envoye avec succes");
            return ResponseEntity.ok("Message envoye avec succes");

        } catch (Exception e) {
            log.error("Erreur envoi contact : {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Erreur lors de l envoi");
        }
    }
}