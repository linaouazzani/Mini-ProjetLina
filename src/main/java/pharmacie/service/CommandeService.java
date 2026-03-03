package pharmacie.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import pharmacie.dao.CommandeRepository;
import pharmacie.dao.DispensaireRepository;
import pharmacie.dao.LigneRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Commande;
import pharmacie.entity.Dispensaire;
import pharmacie.entity.Ligne;
import pharmacie.entity.Medicament;

@Service
@Validated
public class CommandeService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository commandeDao;
    private final LigneRepository ligneDao;
    private final MedicamentRepository medicamentDao;
    private final DispensaireRepository dispensaireDao;

    public CommandeService(CommandeRepository commandeDao, LigneRepository ligneDao, 
                          MedicamentRepository medicamentDao, DispensaireRepository dispensaireDao) {
        this.commandeDao = commandeDao;
        this.ligneDao = ligneDao;
        this.medicamentDao = medicamentDao;
        this.dispensaireDao = dispensaireDao;
    }

    @Transactional
    public Commande creerCommande(String dispensaireCode) {
        Dispensaire disp = dispensaireDao.findById(dispensaireCode).orElseThrow();
        Commande cmd = new Commande(disp);
        cmd.setAdresseLivraison(disp.getAdresse());
        if (dispensaireDao.nombreArticlesCommandesPar(dispensaireCode) > 100) {
            cmd.setRemise(new BigDecimal("0.15"));
        }
        return commandeDao.save(cmd);
    }

    @Transactional
    public Ligne ajouterLigne(int commandeNum, int medicamentRef, @Positive int quantite) {
        Medicament med = medicamentDao.findById(medicamentRef).orElseThrow();
        Commande cmd = commandeDao.findById(commandeNum).orElseThrow();
        
        if (med.isIndisponible()) throw new IllegalStateException("Médicament indisponible");
        if (cmd.getEnvoyeele() != null) throw new IllegalStateException("Commande déjà expédiée");

        Ligne ligne = ligneDao.findByCommandeAndMedicament(cmd, med)
                              .orElse(new Ligne(cmd, med, 0));

        ligne.setQuantite(ligne.getQuantite() + quantite);
        med.setUnitesCommandees(med.getUnitesCommandees() + quantite);
        return ligneDao.save(ligne);
    }

    @Transactional
    public void supprimerLigne(int idLigne) {
        ligneDao.findById(idLigne).ifPresent(ligne -> {
            if (ligne.getCommande().getEnvoyeele() != null) {
                throw new IllegalStateException("Commande déjà envoyée");
            }
            Medicament m = ligne.getMedicament();
            m.setUnitesCommandees(m.getUnitesCommandees() - ligne.getQuantite());
            ligneDao.delete(ligne);
        });
    }

    @Transactional
    public Commande enregistreExpedition(int commandeNum) {
        Commande cmd = commandeDao.findById(commandeNum).orElseThrow();
        if (cmd.getEnvoyeele() != null) throw new IllegalStateException("Déjà expédiée");
        
        cmd.setEnvoyeele(LocalDate.now());
        for (Ligne l : cmd.getLignes()) {
            Medicament m = l.getMedicament();
            m.setUnitesEnStock(m.getUnitesEnStock() - l.getQuantite());
            m.setUnitesCommandees(m.getUnitesCommandees() - l.getQuantite());
        }
        return commandeDao.save(cmd);
    }

    @Transactional(readOnly = true)
    public Commande getCommande(int num) {
        return commandeDao.findById(num).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<Commande> getCommandeEnCoursPour(String code) {
        return commandeDao.commandesEnCoursPour(code);
    }
}