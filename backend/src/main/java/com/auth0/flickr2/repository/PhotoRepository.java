package com.auth0.flickr2.repository;

import com.auth0.flickr2.domain.Photo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Photo entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PhotoRepository extends ReactiveCrudRepository<Photo, Long>, PhotoRepositoryInternal {
    Flux<Photo> findAllBy(Pageable pageable);

    @Override
    Mono<Photo> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Photo> findAllWithEagerRelationships();

    @Override
    Flux<Photo> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM photo entity WHERE entity.album_id = :id")
    Flux<Photo> findByAlbum(Long id);

    @Query("SELECT * FROM photo entity WHERE entity.album_id IS NULL")
    Flux<Photo> findAllWhereAlbumIsNull();

    @Query("SELECT entity.* FROM photo entity JOIN rel_photo__tag joinTable ON entity.id = joinTable.photo_id WHERE joinTable.tag_id = :id")
    Flux<Photo> findByTag(Long id);

    @Override
    <S extends Photo> Mono<S> save(S entity);

    @Override
    Flux<Photo> findAll();

    @Override
    Mono<Photo> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface PhotoRepositoryInternal {
    <S extends Photo> Mono<S> save(S entity);

    Flux<Photo> findAllBy(Pageable pageable);

    Flux<Photo> findAll();

    Mono<Photo> findById(Long id);

    Flux<Photo> findAllBy(Pageable pageable, Criteria criteria);

    Mono<Photo> findOneWithEagerRelationships(Long id);

    Flux<Photo> findAllWithEagerRelationships();

    Flux<Photo> findAllWithEagerRelationships(Pageable page);

    Mono<Void> deleteById(Long id);
}
