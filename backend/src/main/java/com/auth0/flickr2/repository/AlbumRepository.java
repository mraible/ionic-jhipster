package com.auth0.flickr2.repository;

import com.auth0.flickr2.domain.Album;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Album entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AlbumRepository extends ReactiveCrudRepository<Album, Long>, AlbumRepositoryInternal {
    Flux<Album> findAllBy(Pageable pageable);

    @Override
    Mono<Album> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Album> findAllWithEagerRelationships();

    @Override
    Flux<Album> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM album entity WHERE entity.user_id = :id")
    Flux<Album> findByUser(Long id);

    @Query("SELECT * FROM album entity WHERE entity.user_id IS NULL")
    Flux<Album> findAllWhereUserIsNull();

    @Override
    <S extends Album> Mono<S> save(S entity);

    @Override
    Flux<Album> findAll();

    @Override
    Mono<Album> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface AlbumRepositoryInternal {
    <S extends Album> Mono<S> save(S entity);

    Flux<Album> findAllBy(Pageable pageable);

    Flux<Album> findAll();

    Mono<Album> findById(Long id);

    Flux<Album> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Album> findOneWithEagerRelationships(Long id);

    Flux<Album> findAllWithEagerRelationships();

    Flux<Album> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
