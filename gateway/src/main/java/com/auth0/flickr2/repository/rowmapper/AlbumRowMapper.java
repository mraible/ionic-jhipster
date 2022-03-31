package com.auth0.flickr2.repository.rowmapper;

import com.auth0.flickr2.domain.Album;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Album}, with proper type conversions.
 */
@Service
public class AlbumRowMapper implements BiFunction<Row, String, Album> {

    private final ColumnConverter converter;

    public AlbumRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Album} stored in the database.
     */
    @Override
    public Album apply(Row row, String prefix) {
        Album entity = new Album();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        entity.setCreated(converter.fromRow(row, prefix + "_created", Instant.class));
        entity.setUserId(converter.fromRow(row, prefix + "_user_id", String.class));
        return entity;
    }
}
