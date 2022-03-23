package com.auth0.flickr2.repository;

import static org.springframework.data.relational.core.query.Criteria.where;

import com.auth0.flickr2.domain.Album;
import com.auth0.flickr2.repository.rowmapper.AlbumRowMapper;
import com.auth0.flickr2.repository.rowmapper.UserRowMapper;
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
 * Spring Data SQL reactive custom repository implementation for the Album entity.
 */
@SuppressWarnings("unused")
class AlbumRepositoryInternalImpl extends SimpleR2dbcRepository<Album, Long> implements AlbumRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final UserRowMapper userMapper;
    private final AlbumRowMapper albumMapper;

    private static final Table entityTable = Table.aliased("album", EntityManager.ENTITY_ALIAS);
    private static final Table userTable = Table.aliased("jhi_user", "e_user");

    public AlbumRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        UserRowMapper userMapper,
        AlbumRowMapper albumMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Album.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.userMapper = userMapper;
        this.albumMapper = albumMapper;
    }

    @Override
    public Flux<Album> findAllBy(Pageable pageable) {
        return findAllBy(pageable, null);
    }

    @Override
    public Flux<Album> findAllBy(Pageable pageable, Criteria criteria) {
        return createQuery(pageable, criteria).all();
    }

    RowsFetchSpec<Album> createQuery(Pageable pageable, Criteria criteria) {
        List<Expression> columns = AlbumSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(UserSqlHelper.getColumns(userTable, "user"));
        SelectFromAndJoinCondition selectFrom = Select
            .builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(userTable)
            .on(Column.create("user_id", entityTable))
            .equals(Column.create("id", userTable));

        String select = entityManager.createSelect(selectFrom, Album.class, pageable, criteria);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Album> findAll() {
        return findAllBy(null, null);
    }

    @Override
    public Mono<Album> findById(Long id) {
        return createQuery(null, where(EntityManager.ENTITY_ALIAS + ".id").is(id)).one();
    }

    @Override
    public Mono<Album> findOneWithEagerRelationships(Long id) {
        return findById(id);
    }

    @Override
    public Flux<Album> findAllWithEagerRelationships() {
        return findAll();
    }

    @Override
    public Flux<Album> findAllWithEagerRelationships(Pageable page) {
        return findAllBy(page);
    }

    private Album process(Row row, RowMetadata metadata) {
        Album entity = albumMapper.apply(row, "e");
        entity.setUser(userMapper.apply(row, "user"));
        return entity;
    }

    @Override
    public <S extends Album> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
