# Documentation Legacy (ORM fait maison)

But : BaseEntity

- Classe de base pour mapper une entité Java à une table SQL.
- Opérations principales : lecture, écriture, suppression, filtrage.
- Méthodes importantes (résumé) :
  - save() : insert ou update.
  - delete() : supprime l'enregistrement.
  - findAll(Class<T>, QueryManager) : récupère tous les enregistrements.
  - findById(Class<T>, QueryManager, id) : récupère par id.
  - filter(Class<T>, QueryManager, Filter...) : recherche paramétrée.
  - fetch(Class<T>, QueryManager, sql, params...) : requête SQL brute mappée.
  - mount() : charge les relations (foreign keys).

Exemple : Créez une entité

```text
@Entity(tableName = "vol")
public class Vol extends BaseEntity {
  @Id @Column private Long id;
  @Column(name = "numero_vol") private String numeroVol;
}
```

---

But : BaseView

- Variante de `BaseEntity` pour mapper une vue SQL.
- Usage : lecture seule.
- Les fonctions d'écriture héritées (`save()`, `delete()`, etc.) sont considérées comme dépréciées sur `BaseView`.
- Utilisez uniquement les fonctions de lecture (findAll, filter, fetch).

Exemple : Créez une view-mapped entity

```text
@Entity(tableName = "vol_details")
public class VolDetails extends BaseView {
  @Column(name = "id_vol") private Integer idVol;
  @Column(name = "numero_vol") private String numeroVol;
}
```

---

But : EntityGenerator

- Génère du code d'entité à partir du schéma (tables / views).
- Usage courant : ligne de commande Maven (exemples fournis dans le projet `exemple.txt`).

Exemple de commandes :

```text
# générer une table spécifique
mvn exec:java -Dexec.mainClass="gestion_compagnie_aerienne.code_generator.EntityCodeGenerator" -Dexec.args="TABLE_NAME table"

# générer toutes les tables
mvn exec:java -Dexec.mainClass="gestion_compagnie_aerienne.code_generator.EntityCodeGenerator"
```

- Paramètres : dossier de sortie, package cible.
- Résultat : classes Java prêtes, annotations appliquées.

---

But : Filter et Comparator

- `Filter` représente une condition sur une colonne.
- `Comparator` est l'opérateur (EQ, ILIKE, LT, GT, ...).
- `BaseEntity.filter()` et `BaseView.filter()` acceptent des `Filter`.

Exemple d'usage :

```text
List<Filter> filters = new ArrayList<>();
filters.add(new Filter("nom", Comparator.ILIKE, "%Paris%"));
List<Aeroport> res = Aeroport.filter(Aeroport.class, QueryManager.get_instance(), filters.toArray(new Filter[0]));
```

- Le filtrage construit la clause WHERE.
- Les comparateurs déterminent la syntaxe SQL.

---

But : QueryManager

- Pièce centrale pour exécuter les requêtes.
- Fournit la connexion et les helpers SQL.
- Accès via `QueryManager.get_instance()`.
- Utilisé par `findAll`, `filter`, `fetch`, `findById`.

But : RawObject

- Représente un résultat SQL non-typé.
- Utile pour projections et requêtes ad-hoc.
- Moins central qu'une entité, mais pratique pour éviter de créer une classe.

---

But : Annotations

- `@Entity(tableName = "...")` : lie la classe à une table ou vue.
- `@Column(name = "...")` : mappe un champ à une colonne.
- `@Id` : identifie la clé primaire.
- `@ForeignKey(mappedBy = "...", entity = X.class)` : indique une relation et la cible.

Exemple court :

```text
@Entity(tableName = "aeroport")
public class Aeroport extends BaseEntity {
  @Id @Column private Integer id;
  @Column(name = "code_iata") private String codeIata;
  @Column private String nom;
}
```

---

But : Bonnes pratiques (très courtes)

- Utiliser `BaseView` pour les vues. Lecture seule.
- Ne pas appeler `save()`/`delete()` sur `BaseView`.
- Toujours passer `QueryManager.get_instance()` aux méthodes statiques.
- Appeler `mount()` après fetch si relations attendues.

---

Référence

- Des exemples d'utilisation sont fournis dans `exemple.txt`.
- Pour générer des entités, voir `EntityCodeGenerator` dans le même fichier.

Fin du document.
