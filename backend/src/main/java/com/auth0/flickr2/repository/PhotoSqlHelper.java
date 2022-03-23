package com.auth0.flickr2.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class PhotoSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("title", table, columnPrefix + "_title"));
        columns.add(Column.aliased("description", table, columnPrefix + "_description"));
        columns.add(Column.aliased("image", table, columnPrefix + "_image"));
        columns.add(Column.aliased("image_content_type", table, columnPrefix + "_image_content_type"));
        columns.add(Column.aliased("height", table, columnPrefix + "_height"));
        columns.add(Column.aliased("width", table, columnPrefix + "_width"));
        columns.add(Column.aliased("taken", table, columnPrefix + "_taken"));
        columns.add(Column.aliased("uploaded", table, columnPrefix + "_uploaded"));

        columns.add(Column.aliased("album_id", table, columnPrefix + "_album_id"));
        return columns;
    }
}
