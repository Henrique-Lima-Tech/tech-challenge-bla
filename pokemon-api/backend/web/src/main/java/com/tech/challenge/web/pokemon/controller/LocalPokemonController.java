package com.tech.challenge.web.pokemon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tech.challenge.application.pokemon.port.in.DeleteLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.GetLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.ListLocalPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.SyncPokemonUseCase;
import com.tech.challenge.application.pokemon.port.in.UpdateLocalPokemonUseCase;
import com.tech.challenge.web.pokemon.dto.request.SyncPokemonRequest;
import com.tech.challenge.web.pokemon.dto.request.UpdateLocalPokemonRequest;
import com.tech.challenge.web.pokemon.dto.response.LocalPokemonResponse;
import com.tech.challenge.web.pokemon.mapper.LocalPokemonMapper;
import com.tech.challenge.web.shared.pagination.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Local copies of Pokémon (D-09), authenticated (D-11). Each user works on their own copies only (D-31).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/local/pokemon")
@RequiredArgsConstructor
public class LocalPokemonController {

    private final ListLocalPokemonUseCase listLocalPokemonUseCase;
    private final GetLocalPokemonUseCase getLocalPokemonUseCase;
    private final SyncPokemonUseCase syncPokemonUseCase;
    private final UpdateLocalPokemonUseCase updateLocalPokemonUseCase;
    private final DeleteLocalPokemonUseCase deleteLocalPokemonUseCase;
    private final LocalPokemonMapper localPokemonMapper;

    /** The JWT subject is the user id ({@code JwtTokenAdapter}). */
    private static long userId(final Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    @GetMapping
    public PageResponse<LocalPokemonResponse> list(@AuthenticationPrincipal final Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "must be greater than or equal to 0") final int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "must be greater than or equal to 1")
            @Max(value = 100, message = "must be less than or equal to 100") final int size) {
        final var userId = userId(jwt);
        log.debug("Received request for local Pokemon page {} (size {}) of user {}", page, size, userId);
        return localPokemonMapper.toResponse(listLocalPokemonUseCase.list(userId, page, size));
    }

    @GetMapping("/{id}")
    public LocalPokemonResponse get(@AuthenticationPrincipal final Jwt jwt,
            @PathVariable @Positive(message = "must be greater than 0") final long id) {
        final var userId = userId(jwt);
        log.debug("Received request for local Pokemon {} of user {}", id, userId);
        return localPokemonMapper.toResponse(getLocalPokemonUseCase.get(userId, id));
    }

    @PostMapping
    public ResponseEntity<LocalPokemonResponse> sync(@AuthenticationPrincipal final Jwt jwt,
            @RequestBody @Valid final SyncPokemonRequest request) {
        final var userId = userId(jwt);
        log.debug("Received sync request of user {}", userId);
        final var pokemon = syncPokemonUseCase.sync(localPokemonMapper.toCommand(userId, request));
        final var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pokemon.id())
                .toUri();
        return ResponseEntity.created(location).body(localPokemonMapper.toResponse(pokemon));
    }

    @PutMapping("/{id}")
    public LocalPokemonResponse update(@AuthenticationPrincipal final Jwt jwt,
            @PathVariable @Positive(message = "must be greater than 0") final long id,
            @RequestBody @Valid final UpdateLocalPokemonRequest request) {
        final var userId = userId(jwt);
        log.debug("Received update request for local Pokemon {} of user {}", id, userId);
        final var pokemon = updateLocalPokemonUseCase.update(localPokemonMapper.toCommand(userId, id, request));
        return localPokemonMapper.toResponse(pokemon);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal final Jwt jwt,
            @PathVariable @Positive(message = "must be greater than 0") final long id) {
        final var userId = userId(jwt);
        log.debug("Received delete request for local Pokemon {} of user {}", id, userId);
        deleteLocalPokemonUseCase.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
