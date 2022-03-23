package com.auth0.flickr2.repository;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import com.auth0.flickr2.domain.Photo;
import com.auth0.flickr2.domain.Tag;
import com.auth0.flickr2.repository.rowmapper.AlbumRowMapper;
import com.auth0.flickr2.repository.rowmapper.PhotoRowMapper;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoinCondition;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive custom repository implementation for the Photo entity.
 */
@SuppressWarnings("unused")
class PhotoRepositoryInternalImpl extends SimpleR2dbcRepository<Photo, Long> implements PhotoRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final AlbumRowMapper albumMapper;
    private final PhotoRowMapper photoMapper;

    private static final Table entityTable = Table.aliased("photo", EntityManager.ENTITY_ALIAS);
    private static final Table albumTable = Table.aliased("album", "album");

    private static final EntityManager.LinkTable tagLink = new EntityManager.LinkTable("rel_photo__tag", "photo_id", "tag_id");

    public PhotoRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        AlbumRowMapper albumMapper,
        PhotoRowMapper photoMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Photo.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.albumMapper = albumMapper;
        this.photoMapper = photoMapper;
    }

    @Override
    public Flux<Photo> findAllBy(Pageable pageable) {
        return findAllBy(pageable, null);
    }

    @Override
    public Flux<Photo> findAllBy(Pageable pageable, Criteria criteria) {
        return createQuery(pageable, criteria).all();
    }

    RowsFetchSpec<Photo> createQuery(Pageable pageable, Criteria criteria) {
        List<Expression> columns = PhotoSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(AlbumSqlHelper.getColumns(albumTable, "album"));
        SelectFromAndJoinCondition selectFrom = Select
            .builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(albumTable)
            .on(Column.create("album_id", entityTable))
            .equals(Column.create("id", albumTable));

        String select = entityManager.createSelect(selectFrom, Photo.class, pageable, criteria);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Photo> findAll() {
        return findAllBy(null, null);
    }

    @Override
    public Mono<Photo> findById(Long id) {
        return createQuery(null, where(EntityManager.ENTITY_ALIAS + ".id").is(id)).one();
    }

    @Override
    public Mono<Photo> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Photo> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Photo> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Photo process(Row row, RowMetadata metadata) {
        Photo entity = photoMapper.apply(row, "e");
        entity.setAlbum(albumMapper.apply(row, "album"));
        return entity;
    }

    @Override
    public <S extends Photo> Mono<S> save(S entity) {
        return super.save(entity).flatMap((S e) -> updateRelations(e));
    }

    protected <S extends Photo> Mono<S> updateRelations(S entity) {
        Mono<Void> result = entityManager.updateLinkTable(tagLink, entity.getId(), entity.getTags().stream().map(Tag::getId)).then();
        return result.thenReturn(entity);
    }

    @Override
    public Mono<Void> deleteById(Long entityId) {
        return deleteRelations(entityId).then(super.deleteById(entityId));
    }

    protected Mono<Void> deleteRelations(Long entityId) {
        return entityManager.deleteFromLinkTable(tagLink, entityId);
    }
}
