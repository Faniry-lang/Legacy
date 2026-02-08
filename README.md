# Legacy ORM - Documentation Officielle

**Legacy** est un ORM (Object-Relational Mapping) Java léger et orienté objet. Contrairement à Spring/JPA, Legacy permet d'intégrer la logique métier directement dans les entités, réduisant le boilerplate.

---

## Table des matières

1. [Installation](#installation)
2. [Mapper une table : BaseEntity](#mapper-une-table--baseentity)
3. [Mapper une vue SQL : BaseView](#mapper-une-vue-sql--baseview)
4. [Annotations](#annotations)
5. [Génération d'entités : EntityGenerator](#génération-dentités--entitygenerator)
6. [Valeurs générées automatiquement](#valeurs-générées-automatiquement)
7. [Stratégies personnalisées](#stratégies-personnalisées)
8. [Dépendances entre champs générés](#dépendances-entre-champs-générés)
9. [Champs nullable / non-nullable](#champs-nullable--non-nullable)
10. [Filtrage avec FilterSet](#filtrage-avec-filterset)
11. [Requêtes brutes avec fetch](#requêtes-brutes-avec-fetch)
12. [Chargement LAZY des clés étrangères](#chargement-lazy-des-clés-étrangères)
13. [QueryManager et RawObject](#querymanager-et-rawobject)
14. [Architecture orientée objet](#architecture-orientée-objet)

---

## Installation

Ajoutez la dépendance dans votre `pom.xml` :

```xml
<dependency>
    <groupId>legacy</groupId>
    <artifactId>legacy-orm</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

---

## Mapper une table : BaseEntity

`BaseEntity` mappe une classe Java à une table SQL.

### Créer une entité

```java
@Entity(tableName = "aeroport")
public class Aeroport extends BaseEntity {
    public Aeroport() {
        super();
    }

    @Id
    @Column
    private Integer id;

    @Column(name = "code_iata")
    private String codeIata;

    @Column
    private String nom;

    // getters et setters...
}
```

### Méthodes principales

| Méthode | Description |
|---------|-------------|
| `save()` | Insère l'entité en base |
| `update()` | Met à jour l'entité |
| `delete()` | Supprime l'entité |
| `findAll(Class<T>)` | Récupère tous les enregistrements |
| `findById(id, Class<T>)` | Récupère par clé primaire |
| `filter(Class<T>, FilterSet)` | Recherche avec filtres |
| `fetch(Class<T>, sql, params...)` | Requête SQL brute |
| `getForeignKey(fieldName)` | Charge une clé étrangère |

### Exemples d'utilisation

**Insérer :**

```java
Aeroport a = new Aeroport();
a.setCodeIata("CDG");
a.setNom("Charles de Gaulle");
a.save();
```

**Récupérer tous :**

```java
List<Aeroport> aeroports = Aeroport.findAll(Aeroport.class);
```

**Récupérer par ID :**

```java
Aeroport a = Aeroport.findById(1, Aeroport.class);
```

**Mettre à jour :**

```java
a.setNom("Nouveau Nom");
a.update();
```

**Supprimer :**

```java
a.delete();
```

---

## Mapper une vue SQL : BaseView

`BaseView` mappe une classe Java à une vue SQL. **Lecture seule**.

### Créer une vue

```java
@Entity(tableName = "vol_details")
public class VolDetails extends BaseView {
    public VolDetails() {
        super();
    }

    @Column(name = "id_vol")
    private Integer idVol;

    @Column(name = "numero_vol")
    private String numeroVol;

    @Column(name = "places_restantes")
    private Integer placesRestantes;

    // getters et setters...
}
```

### Méthodes disponibles

- `findAll(Class<T>)` ✅
- `filter(Class<T>, FilterSet)` ✅
- `fetch(Class<T>, sql, params...)` ✅

### Méthodes dépréciées (ne pas utiliser)

- `save()` ❌
- `update()` ❌
- `delete()` ❌

---

## Annotations

### @Entity

Lie la classe à une table ou vue.

```java
@Entity(tableName = "nom_table")
```

### @Column

Mappe un champ à une colonne.

```java
@Column                          // nom = nom du champ
@Column(name = "nom_colonne")    // nom personnalisé
@Column(nullable = false)        // non-nullable
```

### @Id

Marque la clé primaire.

```java
@Id
@Column
private Integer id;
```

### @Generated

Génère automatiquement la valeur avant insertion.

```java
@Generated(strategy = UUIDStrategy.class)
private String uuid;
```

### @ForeignKey

Déclare une relation vers une autre entité.

```java
@Column(name = "id_aeroport")
@ForeignKey(mappedBy = "aeroport", entity = Aeroport.class)
private Integer idAeroport;
```

### @DependsOnFieldGeneration

Déclare une dépendance entre champs générés.

```java
@Generated(strategy = OrderStrategy.class)
@DependsOnFieldGeneration(fieldName = "date_depart")
private String id;
```

---

## Génération d'entités : EntityGenerator

Génère automatiquement les classes d'entités depuis le schéma de la base.

### Générer toutes les tables/vues

```java
EntityGenerator.generateAllEntities("src/main/java", "com.example.entities");
```

### Générer une table spécifique

```java
EntityGenerator.generateEntity("vol", "TABLE", "src/main/java", "com.example.entities");
```

### Via Maven

```bash
# Toutes les tables
mvn exec:java -Dexec.mainClass="com.example.EntityCodeGenerator"

# Une table spécifique
mvn exec:java -Dexec.mainClass="com.example.EntityCodeGenerator" -Dexec.args="vol table"
```

### Exemple de générateur

```java
public class EntityCodeGenerator {
    public static void main(String[] args) throws Exception {
        String outputFolder = "src/main/java";
        String packageName = "com.example.entities";

        if (args.length >= 2) {
            String tableName = args[0];
            String tableType = args[1]; // "table" ou "view"
            EntityGenerator.generateEntity(tableName, tableType, outputFolder, packageName);
        } else {
            EntityGenerator.generateAllEntities(outputFolder, packageName);
        }
    }
}
```

---

## Valeurs générées automatiquement

Utilisez `@Generated` pour auto-générer des valeurs.

### Stratégies intégrées

| Stratégie | Description |
|-----------|-------------|
| `UUIDStrategy` | Génère un UUID |
| `TimestampStrategy` | Génère un timestamp courant |
| `RandomIntStrategy` | Génère un entier aléatoire |
| `GeneratedAfterPersistence` | Valeur générée par la DB (auto-increment) |

### Exemples

**UUID :**

```java
@Column
@Generated(strategy = UUIDStrategy.class)
private String uuid;
```

**Timestamp :**

```java
@Column(name = "created_at")
@Generated(strategy = TimestampStrategy.class)
private Timestamp createdAt;
```

**Auto-increment (DB) :**

```java
@Id
@Column
@Generated(strategy = GeneratedAfterPersistence.class)
private Integer id;
```

---

## Stratégies personnalisées

Créez votre propre stratégie en implémentant `Strategy<T>`.

### Interface Strategy

```java
public interface Strategy<T extends BaseEntity> {
    Serializable generate(T entity);
}
```

### Exemple : générer un code de commande

```java
public class OrderStrategy implements Strategy<OrdreDepart> {
    @Override
    public Serializable generate(OrdreDepart ordre) {
        return "ORDR-" + ordre.getDateDepart().toString();
    }
}
```

### Utilisation

```java
@Entity(tableName = "ordre_depart")
public class OrdreDepart extends BaseEntity {

    @Id
    @Column
    @Generated(strategy = OrderStrategy.class)
    @DependsOnFieldGeneration(fieldName = "date_depart")
    private String id;

    @Column(name = "date_depart")
    @Generated(strategy = TimestampStrategy.class)
    private Timestamp dateDepart;

    // getters et setters...
}
```

---

## Dépendances entre champs générés

`@DependsOnFieldGeneration` ordonne la génération des champs.

### Règles

- Le champ référencé doit exister et être annoté `@Generated`.
- Détection automatique des cycles (erreur si cycle détecté).
- Les champs sont générés dans l'ordre topologique.

### Exemple

```java
@Column(name = "date_depart")
@Generated(strategy = TimestampStrategy.class)
private Timestamp dateDepart;

@Id
@Column
@Generated(strategy = OrderStrategy.class)
@DependsOnFieldGeneration(fieldName = "date_depart")
private String id;
```

`date_depart` est généré **avant** `id`, car `id` dépend de `date_depart`.

---

## Champs nullable / non-nullable

Utilisez `nullable` dans `@Column`.

### Champ nullable (défaut)

```java
@Column
private String description;  // peut être null
```

### Champ non-nullable

```java
@Column(nullable = false)
private String nom;  // erreur si null lors du save()
```

### Comportement

- Si `nullable = false` et valeur `null` → `IllegalArgumentException`.
- Si `nullable = true` (défaut) et valeur `null` → colonne ignorée dans l'INSERT.

---

## Filtrage avec FilterSet

`FilterSet` permet de construire des requêtes filtrées.

### Comparateurs disponibles

| Comparateur | SQL |
|-------------|-----|
| `EQUALS` | `=` |
| `NOT_EQUALS` | `<>` |
| `GREATER_THAN` | `>` |
| `LESS_THAN` | `<` |
| `GREATER_THAN_OR_EQUALS` | `>=` |
| `LESS_THAN_OR_EQUALS` | `<=` |
| `LIKE` | `LIKE` |
| `ILIKE` | `ILIKE` (insensible à la casse) |
| `IN` | `IN (...)` |

### Exemple

```java
FilterSet filters = new FilterSet();
filters.add("nom", Comparator.ILIKE, "%Paris%");
filters.add("pays", Comparator.EQUALS, "France");

List<Aeroport> result = Aeroport.filter(Aeroport.class, filters);
```

### Filtrage avec IN

```java
List<Integer> ids = Arrays.asList(1, 2, 3);
filters.add("id", Comparator.IN, ids);
```

---

## Requêtes brutes avec fetch

Exécutez des requêtes SQL personnalisées.

### Syntaxe

```java
List<T> results = BaseEntity.fetch(Class<T>, sql, params...);
```

### Exemple

```java
String sql = "SELECT * FROM vol WHERE numero_vol = ? AND id_aeroport_depart = ?";
List<Vol> vols = Vol.fetch(Vol.class, sql, "AF123", 5);
```

### Requête dans une méthode statique personnalisée

```java
@Entity(tableName = "vol_details")
public class VolDetails extends BaseView {
    // ...

    public static List<VolDetails> findByIdVol(Integer idVol) throws Exception {
        String sql = "SELECT * FROM vol_details WHERE id_vol = ?";
        return fetch(VolDetails.class, sql, idVol);
    }
}
```

---

## Chargement LAZY des clés étrangères

Les clés étrangères sont chargées **à la demande** (lazy loading).

### Déclarer une clé étrangère

```java
@Column(name = "id_aeroport_depart")
@ForeignKey(mappedBy = "aeroport", entity = Aeroport.class)
private Integer idAeroportDepart;
```

### Charger une relation

```java
Vol vol = Vol.findById(1, Vol.class);
Aeroport aeroport = vol.getForeignKey("id_aeroport_depart");
```

### Charger toutes les relations

```java
ForeignKeysCollection fks = vol.getForeignKeysCollection();
Aeroport depart = (Aeroport) fks.get("id_aeroport_depart");
Aeroport arrivee = (Aeroport) fks.get("id_aeroport_arrivee");
```

---

## QueryManager et RawObject

### QueryManager

Gestionnaire central des connexions et requêtes SQL.

```java
QueryManager qm = QueryManager.get_instance();
```

Utilisé en interne par `findAll`, `filter`, `fetch`, etc.

### RawObject

Représente un résultat SQL non typé (Map de colonnes).

```java
List<RawObject> rows = qm.executeSelect("SELECT * FROM vol");
for (RawObject row : rows) {
    Map<String, Object> data = row.getData();
    System.out.println(data.get("numero_vol"));
}
```

Conversion en entité :

```java
Vol vol = row.toEntity(Vol.class);
```

---

## Architecture orientée objet

### Philosophie Legacy vs Spring

| Spring/JPA | Legacy |
|------------|--------|
| Service + Repository séparés | Logique dans l'entité |
| Beaucoup de boilerplate | Code concis |
| Injection de dépendances | Appels directs |

### Exemple Legacy

```java
@Entity(tableName = "vol")
public class Vol extends BaseEntity {
    // ... champs ...

    // Logique métier directement dans l'entité
    public boolean estComplet() {
        return placesRestantes <= 0;
    }

    public static List<Vol> findVolsDisponibles() throws Exception {
        String sql = "SELECT * FROM vol WHERE places_restantes > 0";
        return fetch(Vol.class, sql);
    }
}
```

### Utilisation

```java
List<Vol> vols = Vol.findVolsDisponibles();
for (Vol v : vols) {
    if (!v.estComplet()) {
        // ...
    }
}
```

---

## Résumé rapide

| Tâche | Code |
|-------|------|
| Créer entité | `extends BaseEntity` + annotations |
| Créer vue | `extends BaseView` + annotations |
| Insérer | `entity.save()` |
| Modifier | `entity.update()` |
| Supprimer | `entity.delete()` |
| Tout récupérer | `findAll(Class)` |
| Par ID | `findById(id, Class)` |
| Filtrer | `filter(Class, FilterSet)` |
| SQL brut | `fetch(Class, sql, params)` |
| Clé étrangère | `getForeignKey(fieldName)` |
| Générer entités | `EntityGenerator.generateAllEntities(...)` |

---

*Legacy ORM - Simple, orienté objet, efficace.*
