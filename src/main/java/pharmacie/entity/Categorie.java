package pharmacie.entity;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter @Setter @NoArgsConstructor @RequiredArgsConstructor @ToString
public class Categorie {

    @ManyToMany(mappedBy = "categories") 
    private List<Fournisseur> fournisseurs = new LinkedList<>();
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE) 
    private Integer code;

    @NonNull
    @Size(min = 1, max = 255)
    @Column(unique=true, length = 255)
    @NotBlank 
    private String libelle;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @ToString.Exclude
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "categorie")
    @JsonIgnoreProperties({"categorie", "lignes"})
    private List<Medicament> medicaments = new LinkedList<>();

    public List<Fournisseur> getFournisseurs() {
        return fournisseurs;
    }
}