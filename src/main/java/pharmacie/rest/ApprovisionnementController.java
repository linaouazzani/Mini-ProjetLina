package pharmacie.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import pharmacie.service.ApprovisionnementService;

@Slf4j
@RestController
@RequestMapping("/api/approvisionnement")
public class ApprovisionnementController {

    @Autowired
    private ApprovisionnementService approvisionnementService;

    @PostMapping("/declencher")
    public ResponseEntity<String> declencher() {
        log.info("API REST appelée : /api/approvisionnement/declencher");
        try {
            approvisionnementService.declencherReapprovisionnement();
            return ResponseEntity.ok("Réapprovisionnement déclenché avec succès !");
        } catch (Exception e) {
            log.error("Erreur lors du déclenchement", e);
            return ResponseEntity.internalServerError().body("Erreur interne du serveur.");
        }
    }
}