package com.auth0.flickr2.repository.rowmapper;

import com.auth0.flickr2.domain.Photo;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Photo}, with proper type conversions.
 */
@Service
public class PhotoRowMapper implements BiFunction<Row, String, Photo> {

    private final ColumnConverter converter;

    public PhotoRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Photo} stored in the database.
     */
    @Override
    public Photo apply(Row row, String prefix) {
        Photo entity = new Photo();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTitle(converter.fromRow(row, prefix + "_title", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        entity.setImageContentType(converter.fromRow(row, prefix + "_image_content_type", String.class));
        entity.setImage(converter.fromRow(row, prefix + "_image", byte[].class));
        entity.setHeight(converter.fromRow(row, prefix + "_height", Integer.class));
        entity.setWidth(converter.fromRow(row, prefix + "_width", Integer.class));
        entity.setTaken(converter.fromRow(row, prefix + "_taken", Instant.class));
        entity.setUploaded(converter.fromRow(row, prefix + "_uploaded", Instant.class));
        entity.setAlbumId(converter.fromRow(row, prefix + "_album_id", Long.class));
        return entity;
    }
}
