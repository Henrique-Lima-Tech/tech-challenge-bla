package com.tech.challenge.infrastructure.pokemon.persistence.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "local_pokemon")
@Getter
@Setter
@NoArgsConstructor
public class LocalPokemonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "poke_api_id", nullable = false)
    private Integer pokeApiId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "sprite_url", length = 500)
    private String spriteUrl;

    @Column(length = 50)
    private String category;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 1)
    private BigDecimal weightKg;

    @ElementCollection
    @CollectionTable(name = "local_pokemon_abilities", joinColumns = @JoinColumn(name = "local_pokemon_id"))
    @OrderColumn(name = "position")
    @Column(name = "ability", nullable = false, length = 50)
    private List<String> abilities = new ArrayList<>();

    @Column(name = "localized_name", length = 100)
    private String localizedName;

    @Column(length = 100)
    private String region;

    @ElementCollection
    @CollectionTable(name = "local_pokemon_internal_tags", joinColumns = @JoinColumn(name = "local_pokemon_id"))
    @OrderColumn(name = "position")
    @Column(name = "tag", nullable = false, length = 30)
    private List<String> internalTags = new ArrayList<>();
}
