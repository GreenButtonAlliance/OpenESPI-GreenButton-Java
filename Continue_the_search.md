
# Analysis of CustomerAccountEntity Country Column Duplication Issue

## Problem Identification

After reviewing the code, I've identified the root cause of the "country" column duplication issue in the CustomerAccountEntity:

1. The issue stems from how embedded objects and column name mappings are handled across related entities.

2. The core of the problem involves these classes:
   - `Organisation` (embeddable) contains `StreetAddress` (embeddable)
   - `StreetAddress` has a `country` field mapped to column name "country"
   - `CustomerEntity` embeds `Organisation` and uses `@AttributeOverride` to rename columns
   - `CustomerAccountEntity` has a `@ManyToOne` relationship with `CustomerEntity`

## Technical Details

### Organisation and StreetAddress Classes

The `Organisation` class embeds two instances of `StreetAddress` (streetAddress and postalAddress):

```java
@Embeddable
public class Organisation {
    @Embedded
    private StreetAddress streetAddress;
    
    @Embedded
    private StreetAddress postalAddress;
    // ...
}

@Embeddable
public static class StreetAddress {
    @Column(name = "country", length = 256)
    private String country;
    // ...
}
```

### CustomerEntity Mapping

`CustomerEntity` properly uses `@AttributeOverride` to rename the embedded columns:

```java
@Entity
public class CustomerEntity extends IdentifiedObject {
    @Embedded
    @AttributeOverrides({
        // ...
        @AttributeOverride(name = "streetAddress.country", column = @Column(name = "customer_country")),
        // ...
        @AttributeOverride(name = "postalAddress.country", column = @Column(name = "customer_postal_country")),
        // ...
    })
    private Organisation organisation;
    // ...
}
```

### CustomerAccountEntity Relationship

`CustomerAccountEntity` has a relationship with `CustomerEntity`:

```java
@Entity
public class CustomerAccountEntity extends IdentifiedObject {
    // ...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
}
```

## The Issue

When Hibernate generates the schema or attempts to map these entities:

1. It encounters the `country` column in `StreetAddress`
2. It also sees the overridden columns `customer_country` and `customer_postal_country` in `CustomerEntity`
3. When joining `CustomerAccountEntity` with `CustomerEntity`, Hibernate tries to map all these columns
4. This results in duplicate column name conflicts

## Confirmation

The issue is acknowledged in the project's branch strategy document:

```
feature/fix-customer-account-jpa-mapping - Resolve CustomerAccountEntity country column duplication
```

## Solution Approach

The solution would involve:

1. Ensuring consistent column naming across all related entities
2. Properly using `@AttributeOverride` annotations for all embedded fields
3. Potentially refactoring the entity relationships to avoid deep embedding chains
4. Ensuring that the database schema matches the JPA entity mappings

The `feature/fix-customer-account-jpa-mapping` branch is likely implementing these fixes.

## Additional Observations

I also noticed other potential issues:
- The `CustomerAccountRepository` references a non-existent `isPrePay` field in `CustomerAccountEntity`
- The database migration scripts don't include all the columns defined in the entities
- There may be a mismatch between the JPA entity definitions and the actual database schema

These issues should also be addressed as part of the overall entity mapping cleanup.