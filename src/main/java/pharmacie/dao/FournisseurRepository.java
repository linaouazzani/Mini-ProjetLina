package pharmacie.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import pharmacie.entity.Fournisseur;

@RepositoryRestResource
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {
}
