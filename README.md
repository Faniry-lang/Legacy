# Legacy ORM

**Legacy** est un ORM Java léger et orienté objet pour PostgreSQL.

## Caractéristiques

- 🗃️ Mapping table/vue vers classes Java
- ⚡ Génération automatique d'entités depuis le schéma DB
- 🔗 Chargement LAZY des clés étrangères
- 🎯 Valeurs générées automatiquement (UUID, Timestamp, custom)
- 🔍 Filtrage puissant avec FilterSet
- 📝 Requêtes SQL brutes supportées
- 🏗️ Architecture orientée objet (logique métier dans les entités)

## Installation

```xml
<dependency>
    <groupId>legacy</groupId>
    <artifactId>legacy-orm</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Exemple rapide

### Définir une entité

```java
@Entity(tableName = "aeroport")
public class Aeroport extends BaseEntity {
    @Id @Column
    private Integer id;

    @Column(name = "code_iata")
    private String codeIata;

    @Column
    private String nom;

    // getters/setters...
}
```

### Opérations CRUD

```java
// Créer
Aeroport a = new Aeroport();
a.setCodeIata("CDG");
a.setNom("Charles de Gaulle");
a.save();

// Lire
List<Aeroport> tous = Aeroport.findAll(Aeroport.class);
Aeroport un = Aeroport.findById(1, Aeroport.class);

// Filtrer
FilterSet filters = new FilterSet();
filters.add("pays", Comparator.EQUALS, "France");
List<Aeroport> francais = Aeroport.filter(Aeroport.class, filters);

// Modifier
a.setNom("Nouveau nom");
a.update();

// Supprimer
a.delete();
```

### Générer les entités

```java
EntityGenerator.generateAllEntities("src/main/java", "com.example.entities");
```

## Documentation complète

📖 Voir **[legacy-documentation.md](legacy-documentation.md)** pour :

- Toutes les annotations (`@Entity`, `@Column`, `@Id`, `@Generated`, `@ForeignKey`, `@DependsOnFieldGeneration`)
- BaseEntity vs BaseView
- Stratégies de génération (UUID, Timestamp, custom)
- Dépendances entre champs générés
- FilterSet et Comparators
- Requêtes brutes avec `fetch()`
- Chargement LAZY des relations
- QueryManager et RawObject
- Architecture orientée objet

## Structure du projet

```
src/main/java/legacy/
├── annotations/     # @Entity, @Column, @Id, @Generated, @ForeignKey, @DependsOnFieldGeneration
├── exceptions/      # Exceptions personnalisées
├── query/           # QueryManager, Filter, FilterSet, Comparator, RawObject
├── schema/          # BaseEntity, BaseView, ForeignKeysCollection
├── strategy/        # Strategy, UUIDStrategy, TimestampStrategy, etc.
└── utils/           # DbConn, EntityGenerator, PropertyLoader
```

## Licence

MIT

---

*Legacy ORM - Simple, orienté objet, efficace.*
